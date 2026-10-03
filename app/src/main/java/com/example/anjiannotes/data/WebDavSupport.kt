package com.example.anjiannotes.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** WebDAV 连接参数会整体加密后保存在本机私有偏好中。 */
data class WebDavConfig(
    val endpoint: String,
    val username: String,
    val password: String,
    val remoteDirectory: String = "an-jian-backup",
    val syncDeleteRemoteFiles: Boolean = true
) {
    fun normalized(): WebDavConfig = copy(
        endpoint = endpoint.trim().trimEnd('/'),
        username = username.trim(),
        remoteDirectory = remoteDirectory.trim().trim('/').ifBlank { "an-jian-backup" }
    )

    fun validate() {
        require(endpoint.startsWith("https://")) { "为保护笔记与密码，WebDAV 服务器地址必须使用 https://" }
        require(username.isNotBlank()) { "请输入 WebDAV 用户名" }
        require(password.isNotBlank()) { "请输入 WebDAV 密码或应用专用密码" }
    }
}

data class WebDavSyncResult(
    val uploadedNotes: Int,
    val skippedNotes: Int,
    val foldersUploaded: Boolean,
    val deletedNotes: Int = 0,
    val repairedNotes: Int = 0
)

data class WebDavRemoteSnapshot(
    val snapshot: BackupSnapshot,
    val backupId: String,
    val updatedAt: Long
)

class WebDavConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("webdav_backup", Context.MODE_PRIVATE)

    fun load(): WebDavConfig? = runCatching {
        val encrypted = preferences.getString("config", null) ?: return null
        val root = JSONObject(decrypt(encrypted))
        WebDavConfig(
            endpoint = root.getString("endpoint"),
            username = root.getString("username"),
            password = root.getString("password"),
            remoteDirectory = root.optString("remoteDirectory", "an-jian-backup"),
            syncDeleteRemoteFiles = root.optBoolean("syncDeleteRemoteFiles", true)
        )
    }.getOrNull()

    fun save(config: WebDavConfig) {
        val value = config.normalized().also { it.validate() }
        val raw = JSONObject().apply {
            put("endpoint", value.endpoint)
            put("username", value.username)
            put("password", value.password)
            put("remoteDirectory", value.remoteDirectory)
            put("syncDeleteRemoteFiles", value.syncDeleteRemoteFiles)
        }.toString()
        preferences.edit().putString("config", encrypt(raw)).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 12) { "WebDAV 配置无效" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return cipher.doFinal(payload.copyOfRange(12, payload.size)).toString(Charsets.UTF_8)
    }

    private companion object { const val KEY_ALIAS = "an_jian_webdav_config" }
}

/** 单设备、本地优先的 WebDAV 远程备份客户端。清单永远最后写入。 */
class WebDavBackupClient(context: Context) {
    private val applicationContext = context.applicationContext
    private val taskLock = Mutex()

    suspend fun testConnection(config: WebDavConfig) = taskLock.withLock {
        withContext(Dispatchers.IO) {
            val connection = config.normalized().also { it.validate() }
            val root = connection.rootUrl()
            val response = request(root, "PROPFIND", connection.basicAuthorization(),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:propname/></d:propfind>".toByteArray(),
                mapOf("Depth" to "0", "Content-Type" to "application/xml")
            )
            require(response.code == 207 || response.code in 200..299) { httpMessage("测试连接", response.code) }
            Unit
        }
    }

    suspend fun syncIncremental(
        config: WebDavConfig,
        snapshot: BackupSnapshot,
        appVersion: String
    ): WebDavSyncResult = taskLock.withLock {
        withContext(Dispatchers.IO) {
            val connection = config.normalized().also { it.validate() }
            val auth = connection.basicAuthorization()
            val root = connection.rootUrl()
            ensureCollection(root, auth)
            val notesRoot = "$root/notes"
            ensureCollection(notesRoot, auth)

            val localFiles = snapshot.notes.associate { it.id.toString() to MarkdownZipBackupCodec.encodeNote(it).toByteArray(Charsets.UTF_8) }
            val remoteNames = listFiles(notesRoot, auth).toMutableSet()
            val remoteManifest = readManifest("$root/manifest.json", auth)
                ?: migrateLegacyManifest(root, notesRoot, remoteNames, auth)
            var uploaded = 0
            var skipped = 0
            var repaired = 0

            val folders = MarkdownZipBackupCodec.foldersJson(snapshot.folders).toByteArray(Charsets.UTF_8)
            val foldersHash = digest(folders)
            if (remoteManifest?.foldersHash != foldersHash) put("$root/folders.json", folders, auth) else Unit
            put("$root/metadata.json", MarkdownZipBackupCodec.metadataJson(snapshot, appVersion).toByteArray(Charsets.UTF_8), auth)

            localFiles.forEach { (id, bytes) ->
                val fileName = "$id.md"
                val shouldUpload = remoteManifest?.notes?.get(id) != digest(bytes) || fileName !in remoteNames
                if (shouldUpload) {
                    put("$notesRoot/${encodeSegment(fileName)}", bytes, auth)
                    uploaded++
                    if (fileName in remoteNames) repaired++
                } else skipped++
            }

            var deleted = 0
            if (connection.syncDeleteRemoteFiles) {
                remoteNames.filter { it.endsWith(".md") && it.removeSuffix(".md") !in localFiles.keys }.forEach { fileName ->
                    delete("$notesRoot/${encodeSegment(fileName)}", auth)
                    deleted++
                }
            }

            val manifest = JSONObject().apply {
                put("schemaVersion", MANIFEST_SCHEMA_VERSION)
                put("backupId", "${System.currentTimeMillis()}-${digest((snapshot.notes.size.toString() + foldersHash))}")
                put("updatedAt", System.currentTimeMillis())
                put("foldersHash", foldersHash)
                put("notes", JSONObject().apply { localFiles.forEach { (id, bytes) -> put(id, digest(bytes)) } })
            }
            // 只有 metadata、folders、笔记和清理全部成功后才写 manifest。
            put("$root/manifest.json", manifest.toString(2).toByteArray(Charsets.UTF_8), auth)
            WebDavSyncResult(uploaded, skipped, remoteManifest?.foldersHash != foldersHash, deleted, repaired)
        }
    }

    suspend fun fetchRemoteSnapshot(config: WebDavConfig): WebDavRemoteSnapshot = taskLock.withLock {
        withContext(Dispatchers.IO) {
            val connection = config.normalized().also { it.validate() }
            val auth = connection.basicAuthorization()
            val root = connection.rootUrl()
            val notesRoot = "$root/notes"
            val remoteNames = listFiles(notesRoot, auth)
            val manifest = readManifest("$root/manifest.json", auth)
                ?: migrateLegacyManifest(root, notesRoot, remoteNames, auth)
                ?: throw WebDavException("远程备份缺少清单且没有可迁移的旧版文件", 404)
            require(manifest.schemaVersion == MANIFEST_SCHEMA_VERSION) { "不支持的 WebDAV 备份架构版本" }
            val folders = getRequired("$root/folders.json", auth)
            require(digest(folders) == manifest.foldersHash) { "远程 folders.json 校验失败，备份可能已损坏或被篡改" }
            val noteBytes = manifest.notes.map { (id, expectedHash) ->
                val bytes = getRequired("$notesRoot/${encodeSegment("$id.md")}", auth)
                require(digest(bytes) == expectedHash) { "笔记 $id 校验失败，远程文件可能已损坏或被篡改" }
                id to bytes
            }
            require(noteBytes.size == manifest.notes.size) { "远程笔记数量与清单不一致" }
            val metadata = getRequired("$root/metadata.json", auth)
            val zip = java.io.ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(zip).use { output ->
                writeZip(output, "metadata.json", metadata)
                writeZip(output, "folders.json", folders)
                noteBytes.forEach { (id, bytes) -> writeZip(output, "notes/$id.md", bytes) }
            }
            WebDavRemoteSnapshot(
                snapshot = MarkdownZipBackupCodec.decode(zip.toByteArray()),
                backupId = manifest.backupId,
                updatedAt = manifest.updatedAt
            )
        }
    }

    private fun migrateLegacyManifest(root: String, notesRoot: String, remoteNames: Set<String>, auth: String): Manifest? {
        val folders = getOptional("$root/folders.json", auth) ?: return null
        val notes = remoteNames.filter { it.endsWith(".md") }.associate { name ->
            name.removeSuffix(".md") to digest(getRequired("$notesRoot/${encodeSegment(name)}", auth))
        }
        return Manifest(MANIFEST_SCHEMA_VERSION, "legacy", 0L, notes, digest(folders))
    }

    private fun readManifest(url: String, auth: String): Manifest? = getOptional(url, auth)?.let { raw ->
        runCatching {
            val json = JSONObject(raw.toString(Charsets.UTF_8))
            val values = json.optJSONObject("notes") ?: JSONObject()
            Manifest(
                json.optInt("schemaVersion", -1),
                json.optString("backupId"),
                json.optLong("updatedAt"),
                values.keys().asSequence().associateWith { values.getString(it) },
                json.optString("foldersHash")
            )
        }.getOrElse { throw WebDavException("远程 manifest.json 损坏，无法继续", null, it) }
    }

    private fun ensureCollection(url: String, auth: String) {
        val code = request(url, "MKCOL", auth, null).code
        require(code == 201 || code == 405) { httpMessage("创建远程目录", code) }
    }

    private fun listFiles(url: String, auth: String): Set<String> {
        val body = "<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:propname/></d:propfind>".toByteArray()
        val response = request(url, "PROPFIND", auth, body, mapOf("Depth" to "1", "Content-Type" to "application/xml"))
        val hrefs = Regex("<[^>]*href[^>]*>(.*?)</[^>]*href>", RegexOption.IGNORE_CASE).findAll(response.body.toString(Charsets.UTF_8))
        return hrefs.mapNotNull { match ->
            val decoded = java.net.URLDecoder.decode(match.groupValues[1], Charsets.UTF_8.name()).trimEnd('/')
            decoded.substringAfterLast('/').takeIf { it.isNotBlank() && !it.contains("notes") }
        }.toSet()
    }

    private fun getRequired(url: String, auth: String): ByteArray = getOptional(url, auth) ?: throw WebDavException("远程文件不存在：${url.substringAfterLast('/')}", 404)
    private fun getOptional(url: String, auth: String): ByteArray? {
        val response = request(url, "GET", auth, null, emptyMap(), allowNotFound = true)
        return if (response.code == 404) null else response.body
    }

    private fun put(url: String, bytes: ByteArray, auth: String) {
        val response = request(url, "PUT", auth, bytes, mapOf("Content-Type" to if (url.endsWith(".md")) "text/markdown; charset=utf-8" else "application/json; charset=utf-8"))
        require(response.code in 200..299) { httpMessage("上传远程文件", response.code) }
    }

    private fun delete(url: String, auth: String) {
        val response = request(url, "DELETE", auth, null)
        require(response.code in 200..299 || response.code == 404) { httpMessage("删除远程文件", response.code) }
    }

    private fun request(url: String, method: String, auth: String, body: ByteArray?, headers: Map<String, String> = emptyMap(), allowNotFound: Boolean = false): Response {
        val connection = try { URL(url).openConnection() as HttpURLConnection } catch (error: IOException) { throw WebDavException("网络不可用，请检查网络连接", null, error) }
        return try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Authorization", auth)
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code == 404 && allowNotFound) return Response(code, ByteArray(0))
            if (code !in 200..299 && code != 207) throw WebDavException(httpMessage("WebDAV 请求", code), code)
            Response(code, (connection.inputStream ?: ByteArray(0).inputStream()).use { it.readBytes() })
        } catch (error: WebDavException) {
            throw error
        } catch (error: java.net.SocketTimeoutException) {
            throw WebDavException("网络连接超时，请稍后重试", null, error)
        } catch (error: IOException) {
            throw WebDavException("网络不可用或服务器暂时无法连接", null, error)
        } finally { connection.disconnect() }
    }

    private fun WebDavConfig.rootUrl(): String = endpoint + "/" + remoteDirectory.split('/').filter(String::isNotBlank).joinToString("/") { encodeSegment(it) }
    private fun WebDavConfig.basicAuthorization(): String = "Basic ${Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"

    private fun httpMessage(action: String, code: Int): String = when (code) {
        401 -> "WebDAV 账号或密码错误（HTTP 401）"
        403 -> "服务器拒绝访问，请检查权限（HTTP 403）"
        404 -> "远程备份目录或文件不存在（HTTP 404）"
        in 500..599 -> "WebDAV 服务器暂时故障（HTTP $code）"
        else -> "${action}失败（HTTP $code）"
    }

    private fun writeZip(zip: java.util.zip.ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(java.util.zip.ZipEntry(name)); zip.write(bytes); zip.closeEntry()
    }

    private fun encodeSegment(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun digest(value: String): String = digest(value.toByteArray(Charsets.UTF_8))

    private data class Response(val code: Int, val body: ByteArray)
    private data class Manifest(val schemaVersion: Int, val backupId: String, val updatedAt: Long, val notes: Map<String, String>, val foldersHash: String)
    private companion object { const val MANIFEST_SCHEMA_VERSION = 2; const val CONNECT_TIMEOUT = 15_000; const val READ_TIMEOUT = 30_000 }
}

class WebDavException(message: String, val statusCode: Int?, cause: Throwable? = null) : RuntimeException(message, cause)

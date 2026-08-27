package com.example.anjiannotes.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** WebDAV 连接参数会整体加密后保存在本机私有偏好中。 */
data class WebDavConfig(
    val endpoint: String,
    val username: String,
    val password: String,
    val remoteDirectory: String = "an-jian-backup"
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
    val foldersUploaded: Boolean
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
            remoteDirectory = root.optString("remoteDirectory", "an-jian-backup")
        )
    }.getOrNull()

    fun save(config: WebDavConfig) {
        val value = config.normalized().also { it.validate() }
        val raw = JSONObject().apply {
            put("endpoint", value.endpoint)
            put("username", value.username)
            put("password", value.password)
            put("remoteDirectory", value.remoteDirectory)
        }.toString()
        preferences.edit().putString("config", encrypt(raw)).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 12) { "WebDAV 配置无效" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return cipher.doFinal(payload.copyOfRange(12, payload.size)).toString(Charsets.UTF_8)
    }

    private companion object {
        const val KEY_ALIAS = "an_jian_webdav_config"
    }
}

/**
 * WebDAV 使用与本地 ZIP 相同的文件布局，但将 notes 目录下的 Markdown 文件独立上传，
 * 使后续同步只需上传变更笔记而无需重复传输完整备份包。
 */
class WebDavBackupClient(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("webdav_incremental_state", Context.MODE_PRIVATE)

    suspend fun syncIncremental(
        config: WebDavConfig,
        snapshot: BackupSnapshot,
        appVersion: String
    ): WebDavSyncResult = withContext(Dispatchers.IO) {
        val connection = config.normalized().also { it.validate() }
        val authorization = connection.basicAuthorization()
        val root = "${connection.endpoint}/${connection.remoteDirectory}"
        ensureCollection(root, authorization)
        ensureCollection("$root/notes", authorization)

        val scope = digest("${connection.endpoint}|${connection.username}|${connection.remoteDirectory}")
        val pendingState = preferences.edit()
        var uploadedNotes = 0
        var skippedNotes = 0
        snapshot.notes.forEach { note ->
            val content = MarkdownZipBackupCodec.encodeNote(note).toByteArray(Charsets.UTF_8)
            val key = "$scope.note.${note.id}"
            val hash = digest(content)
            if (preferences.getString(key, null) == hash) {
                skippedNotes++
            } else {
                put("$root/${MarkdownZipBackupCodec.noteFileName(note)}", content, authorization)
                pendingState.putString(key, hash)
                uploadedNotes++
            }
        }

        val folders = MarkdownZipBackupCodec.foldersJson(snapshot.folders).toByteArray(Charsets.UTF_8)
        val folderHash = digest(folders)
        val folderKey = "$scope.folders"
        val foldersUploaded = preferences.getString(folderKey, null) != folderHash
        if (foldersUploaded) {
            put("$root/folders.json", folders, authorization)
            pendingState.putString(folderKey, folderHash)
        }
        put("$root/metadata.json", MarkdownZipBackupCodec.metadataJson(snapshot, appVersion).toByteArray(Charsets.UTF_8), authorization)
        pendingState.apply()
        WebDavSyncResult(uploadedNotes, skippedNotes, foldersUploaded)
    }

    private fun ensureCollection(url: String, authorization: String) {
        val response = connection(url, "MKCOL", authorization).useAndCode()
        require(response == HttpURLConnection.HTTP_CREATED || response == HttpURLConnection.HTTP_BAD_METHOD) {
            "无法创建 WebDAV 目录（HTTP $response）"
        }
    }

    private fun put(url: String, bytes: ByteArray, authorization: String) {
        val connection = connection(url, "PUT", authorization).apply {
            doOutput = true
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("Content-Type", if (url.endsWith(".md")) "text/markdown; charset=utf-8" else "application/json; charset=utf-8")
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        connection.disconnect()
        require(code in 200..299) { "WebDAV 上传失败（HTTP $code）" }
    }

    private fun connection(url: String, method: String, authorization: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", authorization)
        }
    }

    private fun WebDavConfig.basicAuthorization(): String {
        val credential = "$username:$password".toByteArray(Charsets.UTF_8)
        return "Basic ${Base64.encodeToString(credential, Base64.NO_WRAP)}"
    }

    private fun HttpURLConnection.useAndCode(): Int = try {
        responseCode
    } finally {
        disconnect()
    }

    private fun digest(value: String): String = digest(value.toByteArray(Charsets.UTF_8))
    private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

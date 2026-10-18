package com.example.anjiannotes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anjiannotes.data.WebDavConfig
import com.example.anjiannotes.data.WebDavRemoteSnapshot
import com.example.anjiannotes.ui.DialogAction
import com.example.anjiannotes.ui.DialogActionButtons
import com.example.anjiannotes.ui.DialogContentColumn
import com.example.anjiannotes.ui.screenHorizontalPadding
import com.example.anjiannotes.ui.rememberIsWatchWindow
import com.example.anjiannotes.ui.contentMaxWidth
import com.example.anjiannotes.ui.contentWidthCap
import com.example.anjiannotes.ui.theme.AppearanceMode
import com.example.anjiannotes.ui.theme.MotionLevel
import com.example.anjiannotes.ui.theme.NoteFont
import java.io.File
import java.util.Date
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPage(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    appearanceMode: AppearanceMode,
    onOpenMotion: () -> Unit,
    motionLevel: MotionLevel,
    noteListSortByOpen: Boolean,
    onNoteListSortByOpenChange: (Boolean) -> Unit,
    onBackupClick: () -> Unit,
    onWebDavClick: () -> Unit,
    webDavConfigured: Boolean
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val compact = rememberIsWatchWindow()
        // 宽屏内容封顶居中：设置项不横贯平板；手机竖屏与手表不封顶，布局不变。
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentWidthCap(contentMaxWidth())
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = screenHorizontalPadding(),
                        vertical = if (compact) 12.dp else 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
            ) {
            SettingsGroup(
                title = "外观",
                items = listOf(
                    SettingsEntry("◐", "外观", "当前：${appearanceMode.label}", onOpenAppearance),
                    SettingsEntry("»", "动画", "当前：${motionLevel.label}", onOpenMotion)
                )
            )
            SettingsToggleGroup(
                title = "列表排序",
                items = listOf(
                    SettingsToggleEntry(
                        symbol = "⇅",
                        title = "笔记按最近打开置顶",
                        subtitle = "最近打开的笔记排在列表最前；关闭时按最近修改排序",
                        checked = noteListSortByOpen,
                        onCheckedChange = onNoteListSortByOpenChange
                    )
                )
            )
            SettingsGroup(
                title = "数据与备份",
                items = listOf(
                    SettingsEntry("↥", "本地备份与恢复", "Markdown ZIP，可在其他应用中直接阅读", onBackupClick),
                    SettingsEntry(
                        "⌁",
                        "WebDAV 增量备份",
                        if (webDavConfigured) "已配置 · 仅上传发生变化的笔记" else "配置服务器后上传 Markdown 与元数据",
                        onWebDavClick
                    )
                )
            )
            SettingsGroup(
                title = "关于",
                items = listOf(
                    SettingsEntry("i", "安装", "安笺 ${BuildConfig.VERSION_NAME} · 离线笔记", onClick = {})
                )
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSettingsPage(
    appearanceMode: AppearanceMode,
    noteFont: NoteFont,
    customFontPath: String?,
    onBack: () -> Unit,
    onSelect: (AppearanceMode) -> Unit,
    onFontSelect: (NoteFont) -> Unit,
    onCustomFontPath: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("外观", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val context = LocalContext.current
        val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                val target = File(context.filesDir, "custom_note_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: error("无法读取字体文件")
                onCustomFontPath(target.absolutePath)
            }
        }
        // 宽屏内容封顶居中：外观选项不横贯平板；手机竖屏与手表不封顶，布局不变。
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentWidthCap(contentMaxWidth())
                    .padding(horizontal = screenHorizontalPadding(), vertical = if (rememberIsWatchWindow()) 12.dp else 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Text(
                "选择主题后会立即应用到当前页面和所有组件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            AppearanceOption(
                symbol = "☀",
                title = "浅色",
                subtitle = "柔和浅灰背景，适合明亮环境",
                selected = appearanceMode == AppearanceMode.LIGHT,
                onClick = { onSelect(AppearanceMode.LIGHT) }
            )
            AppearanceOption(
                symbol = "☾",
                title = "深色",
                subtitle = "低亮度深灰层级，适合暗光阅读",
                selected = appearanceMode == AppearanceMode.DARK,
                onClick = { onSelect(AppearanceMode.DARK) }
            )
            AppearanceOption(
                symbol = "⚙",
                title = "跟随系统",
                subtitle = "随 Android 系统的浅色与深色模式实时变化",
                selected = appearanceMode == AppearanceMode.SYSTEM,
                onClick = { onSelect(AppearanceMode.SYSTEM) }
            )
            Text("阅读字体", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            Text("字体只影响阅读预览和编辑正文，不改变笔记内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppearanceOption(
                symbol = "Aa",
                title = "默认字体",
                subtitle = "内置中文无衬线字体",
                selected = noteFont == NoteFont.DEFAULT,
                onClick = { onFontSelect(NoteFont.DEFAULT) }
            )
            AppearanceOption(
                symbol = "宋",
                title = "宋体",
                subtitle = "适合长篇文档和正式内容阅读",
                selected = noteFont == NoteFont.SONG,
                onClick = { onFontSelect(NoteFont.SONG) }
            )
            AppearanceOption(
                symbol = "T",
                title = "导入字体",
                subtitle = if (customFontPath != null) "已导入自定义字体" else "从设备选择 .ttf 或 .otf 文件",
                selected = noteFont == NoteFont.CUSTOM,
                onClick = {
                    if (customFontPath != null) onFontSelect(NoteFont.CUSTOM)
                    else fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
                }
            )
            TextButton(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) }) {
                Text("重新导入字体")
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MotionSettingsPage(
    motionLevel: MotionLevel,
    onBack: () -> Unit,
    onSelect: (MotionLevel) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("动画", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        // 宽屏内容封顶居中：与外观页同一布局约定；手机竖屏与手表不封顶。
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentWidthCap(contentMaxWidth())
                    .padding(horizontal = screenHorizontalPadding(), vertical = if (rememberIsWatchWindow()) 12.dp else 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "档位决定页面过渡、列表滑动与状态反馈的速度。极速档最快，也是默认档；" +
                        "更高档位的动作更完整，等待感也会相应增加。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // 档位序号即速度梯度：1 最快、4 最完整。符号用数字，避免非常用字符的字形覆盖问题。
                MotionLevel.entries.forEachIndexed { index, level ->
                    AppearanceOption(
                        symbol = "${index + 1}",
                        title = level.label,
                        subtitle = level.description,
                        selected = motionLevel == level,
                        onClick = { onSelect(level) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppearanceOption(
    symbol: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

internal data class SettingsEntry(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
internal fun SettingsGroup(title: String, items: List<SettingsEntry>) {
    val compact = rememberIsWatchWindow()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 56.dp else 60.dp)
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = if (compact) 12.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            modifier = Modifier.padding(start = 52.dp)
                        )
                    }
                }
            }
        }
    }
}

internal data class SettingsToggleEntry(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

/** 与 SettingsGroup 同款卡片的开关设置组；整行可点切换。 */
@Composable
internal fun SettingsToggleGroup(title: String, items: List<SettingsToggleEntry>) {
    val compact = rememberIsWatchWindow()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 56.dp else 60.dp)
                            .clickable { item.onCheckedChange(!item.checked) }
                            .padding(horizontal = if (compact) 12.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange
                        )
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            modifier = Modifier.padding(start = 52.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WebDavBackupDialog(
    initialConfig: WebDavConfig?,
    task: WebDavTask,
    onDismiss: () -> Unit,
    onTest: (WebDavConfig) -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onRestore: (WebDavConfig) -> Unit,
    onSync: (WebDavConfig) -> Unit
) {
    var endpoint by remember(initialConfig) { mutableStateOf(initialConfig?.endpoint.orEmpty()) }
    var username by remember(initialConfig) { mutableStateOf(initialConfig?.username.orEmpty()) }
    var password by remember(initialConfig) { mutableStateOf(initialConfig?.password.orEmpty()) }
    var remoteDirectory by remember(initialConfig) { mutableStateOf(initialConfig?.remoteDirectory ?: "an-jian-backup") }
    var syncDeleteRemoteFiles by remember(initialConfig) { mutableStateOf(initialConfig?.syncDeleteRemoteFiles ?: true) }
    val busy = task != WebDavTask.IDLE
    fun currentConfig() = WebDavConfig(endpoint, username, password, remoteDirectory, syncDeleteRemoteFiles)

    // 表单字段在两种外壳间共享，状态提升到本函数；小屏走全屏页、常规走 AlertDialog。
    val form: @Composable ColumnScope.() -> Unit = {
        WebDavConfigForm(
            endpoint = endpoint,
            onEndpointChange = { endpoint = it },
            username = username,
            onUsernameChange = { username = it },
            password = password,
            onPasswordChange = { password = it },
            remoteDirectory = remoteDirectory,
            onRemoteDirectoryChange = { remoteDirectory = it },
            syncDeleteRemoteFiles = syncDeleteRemoteFiles,
            onSyncDeleteRemoteFilesChange = { syncDeleteRemoteFiles = it },
            busy = busy,
            task = task
        )
    }
    // FlowRow 自动换行：四个操作在窄手机/手表上超过一行宽度时折行，不再被裁切。
    val actions = listOf(
        DialogAction("测试连接", enabled = !busy, onClick = { onTest(currentConfig()) }),
        DialogAction("仅保存", enabled = !busy, onClick = { onSave(currentConfig()) }),
        DialogAction("从 WebDAV 恢复", enabled = !busy, onClick = { onRestore(currentConfig()) }),
        DialogAction("立即备份", emphasized = true, enabled = !busy, onClick = { onSync(currentConfig()) })
    )

    if (rememberIsWatchWindow()) {
        // Watch 档（窗口宽度 < 320dp，手表/分屏小窗）：整表单 + 4 个操作无法在矮屏内平铺，
        // 改用全屏对话框——标题与操作区常驻、表单内部滚动，任何按钮都不会被顶出屏幕。
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "WebDAV 增量备份",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    // weight(1f) 已把表单限制在标题与操作区之间的剩余空间，
                    // 表单自身滚动即可，无需再叠加高度上限（避免留白）。
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { form() }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DialogActionButtons(
                        actions = actions,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("WebDAV 增量备份") },
            text = {
                // 限高 + 内部滚动：矮屏（横屏手机、手表兼容窗口）下按钮不会被顶出屏幕。
                DialogContentColumn(spacing = 10.dp, content = form)
            },
            dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
            confirmButton = { DialogActionButtons(actions = actions) }
        )
    }
}

/** WebDAV 备份表单字段：全屏页与对话框两种外壳共用，状态由调用方提升。 */
@Composable
internal fun WebDavConfigForm(
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    remoteDirectory: String,
    onRemoteDirectoryChange: (String) -> Unit,
    syncDeleteRemoteFiles: Boolean,
    onSyncDeleteRemoteFilesChange: (Boolean) -> Unit,
    busy: Boolean,
    task: WebDavTask
) {
    // 表单自带纵向节奏：两种外壳（全屏页 / 对话框）都不再需要额外间距。
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "服务器上会保存 notes/*.md、folders.json 与 metadata.json。密码使用设备加密密钥保存在本机。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpointChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("服务器地址", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            // 占位符单行省略：多行占位符会把输入框撑高、URL 断行显示，小屏上尤其难看。
            placeholder = {
                Text(
                    "https://dav.example.com/remote.php/dav/files/name",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("用户名", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密码或应用专用密码", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        OutlinedTextField(
            value = remoteDirectory,
            onValueChange = onRemoteDirectoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("远程备份目录", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = syncDeleteRemoteFiles,
                onCheckedChange = onSyncDeleteRemoteFilesChange,
                enabled = !busy
            )
            Column {
                Text("同步删除远程备份文件")
                Text(
                    "删除本地笔记后，下次备份会清理远端冗余文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            when (task) {
                WebDavTask.BACKUP -> "正在备份，请勿重复操作"
                WebDavTask.RESTORE -> "正在读取远程备份，请稍候"
                WebDavTask.IDLE -> "手动备份工具：不会自动覆盖本地数据"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun WebDavRestorePreviewDialog(
    remote: WebDavRemoteSnapshot,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从 WebDAV 恢复？") },
        text = {
            // 复用全局 ThreadLocal 格式化器：SimpleDateFormat 非线程安全且构造成本高，
            // 直接在字符串模板里 new 会让该对话框每次重组都重复分配。
            Text("备份时间：${NoteDateFormatter.get().format(Date(remote.updatedAt))}\n笔记：${remote.snapshot.notes.size} 条\n收藏夹：${remote.snapshot.folders.size} 个\n\n恢复将替换当前本地数据。远程文件已完成完整性校验。")
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认恢复") } }
    )
}

@Composable
internal fun BackupMenuDialog(
    onDismiss: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onExportText: () -> Unit,
    onImportText: () -> Unit
) {
    // 内容两种外壳共享；小屏走全屏对话框、常规走 AlertDialog（同 WebDavBackupDialog 的既有范式）。
    val menu: @Composable ColumnScope.() -> Unit = {
        Text(
            "备份会创建可跨平台阅读的 ZIP：每篇笔记均为独立 Markdown 文件，同时包含收藏夹与备份元数据。恢复会替换当前本地数据。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onExportZip, modifier = Modifier.fillMaxWidth()) { Text("导出 Markdown ZIP 备份", modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onImportZip, modifier = Modifier.fillMaxWidth()) { Text("导入 Markdown ZIP 并完整恢复", modifier = Modifier.fillMaxWidth()) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
        Text(
            "TXT 明文备份保留为轻量、直接可读的本地备份格式。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onExportText, modifier = Modifier.fillMaxWidth()) { Text("导出 TXT 明文备份", modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onImportText, modifier = Modifier.fillMaxWidth()) { Text("导入 TXT 明文并完整恢复", modifier = Modifier.fillMaxWidth()) }
    }
    if (rememberIsWatchWindow()) {
        // Watch 档（窗口宽度 < 320dp，手表/分屏小窗）：M3 AlertDialog 在此档位有两处硬伤——
        //   1) BasicAlertDialog 对内容施加 sizeIn(minWidth = 280dp)，且该约束位于调用方
        //      modifier 内层（constrain 语义会把外层 widthIn 顶回 280dp，无法封顶），
        //      约 298dp 宽的手表屏上卡片两侧仅剩约 9dp，窄于 280dp 的手表直接横向溢出；
        //   2) 文本槽是 weight(1f, fill = false) 的 Box，不限高也不滚动，内容超高时
        //      实测标题被顶出屏幕顶部、第四个动作按钮整行丢失（v2.16.3 用户截图）。
        // 改用全屏对话框：标题与关闭常驻、菜单内部滚动，任何按钮都不会丢失。
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "备份与恢复",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) { Text("关闭") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    // weight(1f) 已把菜单限制在标题行与屏幕底边之间的剩余空间，菜单自身滚动即可。
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = menu
                    )
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("备份与恢复") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = menu)
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
        )
    }
}

@Composable
internal fun RestoreTextBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "恢复 TXT 明文备份？",
    message = "恢复会以 TXT 备份中的收藏夹和笔记替换当前本地数据，此操作无法撤销。",
    confirmLabel = "确认恢复",
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
internal fun RestoreBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "恢复备份？",
    message = "恢复会以备份文件中的收藏夹和笔记替换当前本地数据，此操作无法撤销。",
    confirmLabel = "确认恢复",
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
internal fun FeedbackDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

@Composable
internal fun QuickCaptureBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStartWriting: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit
) {
    val pillShape = RoundedCornerShape(30.dp)
    val shadowColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    val compact = rememberIsWatchWindow()
    val barHeight = if (compact) 52.dp else 56.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = if (compact) 12.dp else 16.dp,
                end = if (compact) 12.dp else 16.dp,
                top = 8.dp,
                bottom = 16.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .shadow(
                        elevation = 6.dp,
                        shape = pillShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .clip(pillShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .clickable(onClick = onStartWriting)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "写点什么…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                Surface(
                    modifier = Modifier
                        .size(barHeight)
                        .shadow(
                            elevation = 5.dp,
                            shape = CircleShape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                        .clickable { onExpandedChange(!expanded) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⋮", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    CreateMenuItem("新建笔记", "打开完整编辑页开始书写", onClick = { onExpandedChange(false); onStartWriting() })
                    CreateMenuItem("导入文件", "支持 .md、.markdown、.txt 与 UTF-8 文本", onClick = { onExpandedChange(false); onImportFile() })
                    CreateMenuItem("从剪切板导入", "读取当前最近一条剪切板文本", onClick = { onExpandedChange(false); onImportClipboard() })
                }
            }
        }
    }
}

@Composable
internal fun CreateMenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick
    )
}

/** 笔记详情预览页的正文搜索栏：关键字、命中计数与上一个/下一个导航。 */
@Composable
internal fun NoteSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchIndex: Int,
    matchTotal: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val compact = rememberIsWatchWindow()
    val navButtonSize = if (compact) 36.dp else 40.dp
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onNext() }),
            modifier = Modifier.weight(1f).padding(vertical = 8.dp).focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            "搜索正文…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        Text(
            text = when {
                query.isBlank() -> ""
                matchTotal == 0 -> "无结果"
                else -> "${matchIndex + 1}/$matchTotal"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
        )
        TextButton(onClick = onPrevious, enabled = matchTotal > 0, modifier = Modifier.size(navButtonSize)) {
            Text("↑", fontSize = 18.sp)
        }
        TextButton(onClick = onNext, enabled = matchTotal > 0, modifier = Modifier.size(navButtonSize)) {
            Text("↓", fontSize = 18.sp)
        }
        TextButton(onClick = onClose, modifier = Modifier.size(navButtonSize)) {
            Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

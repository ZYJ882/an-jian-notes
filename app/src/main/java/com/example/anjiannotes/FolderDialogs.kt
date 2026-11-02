package com.example.anjiannotes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.anjiannotes.data.DEFAULT_FOLDER_ID
import com.example.anjiannotes.data.FolderEntity
import com.example.anjiannotes.data.STARRED_FOLDER_ID
import com.example.anjiannotes.ui.DialogContentColumn


@Composable
internal fun FolderNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建收藏夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("收藏夹名称") },
                singleLine = true
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("创建") } }
    )
}

@Composable
internal fun FolderDeleteDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = ConfirmationDialog(
    title = "删除收藏夹？",
    message = "将删除“${folder.name}”及其中全部笔记。此操作不可恢复。",
    confirmLabel = "删除",
    destructive = true,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

/** 抽屉收藏夹条目：视觉仿 NavigationDrawerItem，额外支持长按呼出操作菜单。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 长按收藏夹弹出的操作菜单：重命名 / 排序 / 删除（默认收藏夹与星标入口不可删除，不出现删除项）。 */
@Composable
internal fun FolderActionsMenuDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onSort: () -> Unit,
    onDelete: () -> Unit
) {
    val canDelete = folder.id != DEFAULT_FOLDER_ID && folder.id != STARRED_FOLDER_ID
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Text("重命名", modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = onSort, modifier = Modifier.fillMaxWidth()) {
                    Text("排序", modifier = Modifier.fillMaxWidth())
                }
                if (canDelete) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("删除", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {}
    )
}

/** 重命名收藏夹；风格与新建收藏夹对话框一致。 */
@Composable
internal fun FolderRenameDialog(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(folder.id, folder.name) { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名收藏夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("收藏夹名称") },
                singleLine = true
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = { if (name.trim().isNotBlank()) onConfirm(name) }) { Text("保存") }
        }
    )
}

/** 手动排序收藏夹：上移 / 下移 / 置顶，基于 sortOrder 基准顺序实时生效。 */
@Composable
internal fun FolderSortDialog(
    folderId: Long,
    baseOrder: List<FolderEntity>,
    onDismiss: () -> Unit,
    onTop: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    val folder = baseOrder.firstOrNull { it.id == folderId }
    val index = baseOrder.indexOfFirst { it.id == folderId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "收藏夹排序" else "排序 · ${folder.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            // 排序操作项可内部滚动：小屏上不与按钮争夺屏幕高度。
            DialogContentColumn(spacing = 10.dp) {
                Text(
                    if (folder == null) "该收藏夹不存在或已被删除。"
                    else "当前位置：第 ${index + 1} 个 / 共 ${baseOrder.size} 个",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "调整收藏夹在抽屉中的显示顺序，改动立即生效。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (folder != null && index >= 0) {
                    TextButton(onClick = onUp, enabled = index > 0, modifier = Modifier.fillMaxWidth()) {
                        Text("↑  上移一位", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onDown, enabled = index < baseOrder.lastIndex, modifier = Modifier.fillMaxWidth()) {
                        Text("↓  下移一位", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onTop, enabled = index > 0, modifier = Modifier.fillMaxWidth()) {
                        Text("⇧  移到最前", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        confirmButton = {}
    )
}

@Composable
internal fun FolderPickerDialog(
    folders: List<FolderEntity>,
    selectedFolderId: Long,
    title: String = "移动到收藏夹",
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // 收藏夹较多时列表可内部滚动，按钮不会被顶出屏幕（小屏尤其重要）。
            DialogContentColumn(spacing = 6.dp) {
                folders.forEach { folder ->
                    TextButton(
                        onClick = { onSelected(folder.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (folder.id == selectedFolderId) "${folder.name} ✓" else folder.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {}
    )
}

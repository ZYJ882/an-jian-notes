package com.example.anjiannotes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color


@Composable
internal fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmationDialog(
    title = "删除这条笔记？",
    message = "删除后无法恢复。",
    confirmLabel = "删除",
    destructive = true,
    onDismiss = onDismiss,
    onConfirm = onConfirm
)

@Composable
internal fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        }
    )
}

@Composable
internal fun ImportErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入未完成") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}


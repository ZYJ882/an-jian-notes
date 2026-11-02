package com.example.anjiannotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 小屏（手表/超窄窗口）判定。
 *
 * 由窗口宽度分类驱动（Watch 档，宽度 < 320dp）：手表整机、手机分屏、小窗模式等
 * 任何窄窗口都会启用最紧凑的策略；普通手机（Compact 档，320–599dp）完全不受影响。
 */
@Composable
fun rememberIsWatchWindow(): Boolean = rememberWindowSizeClass().isWatch

/**
 * 页面水平内边距分档纯函数（可单元测试）。
 *
 * 随窗口宽度分类逐级放宽，为宽屏内容保留呼吸感：
 * Watch 12dp 让内容优先，Compact 20dp 保持原有节奏，Medium/Expanded 24/28dp 适配平板。
 */
fun horizontalPaddingFor(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.Watch -> 12.dp
    WindowWidthClass.Compact -> 20.dp
    WindowWidthClass.Medium -> 24.dp
    WindowWidthClass.Expanded -> 28.dp
}

/** 当前页面水平内边距（随窗口宽度分类切换）。 */
@Composable
fun screenHorizontalPadding(): Dp = horizontalPaddingFor(rememberWindowSizeClass().widthClass)

/**
 * 整页内容宽度封顶纯函数（可单元测试）。
 *
 * 手机竖屏与手表不封顶（内容天然铺满窗口）；Medium/Expanded 封顶后水平居中，
 * 避免长文本行横贯平板导致可读性下降，也与 M3 断点下对话框宽度（≤560dp）呼应。
 * 封顶值取自阅读行长约 60–75 个中文字符的经验区间（17sp 正文 ≈ 8.5dp/字）。
 */
fun contentMaxWidthFor(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.Watch, WindowWidthClass.Compact -> Dp.Infinity
    WindowWidthClass.Medium -> 640.dp
    WindowWidthClass.Expanded -> 720.dp
}

/** 当前整页内容宽度封顶值；配合 Box 水平居中与 [contentWidthCap] 使用。 */
@Composable
fun contentMaxWidth(): Dp = contentMaxWidthFor(rememberWindowSizeClass().widthClass)

/**
 * 导航抽屉宽度封顶纯函数（可单元测试）。
 *
 * 抽屉原按屏幕宽度的 76% 拉伸：手机竖屏（312dp）与手表（182dp）比例合理，
 * 但 840dp 的 Medium 屏会拉到 638dp、1280dp 的平板会拉到 973dp，
 * 远超 Material3 的 360dp 规范值。宽屏封顶 360dp，窄屏保持 76% 比例填充。
 */
fun drawerMaxWidthFor(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.Watch, WindowWidthClass.Compact -> Dp.Infinity
    WindowWidthClass.Medium, WindowWidthClass.Expanded -> 360.dp
}

/** 当前导航抽屉宽度封顶值；配合 fillMaxWidth(0.76f) 与 [contentWidthCap] 使用。 */
@Composable
fun drawerMaxWidth(): Dp = drawerMaxWidthFor(rememberWindowSizeClass().widthClass)

/**
 * 是否启用列表-详情双栏纯函数（可单元测试）。
 *
 * 仅 Expanded（≥840dp，平板竖屏/横屏、桌面模式）启用：双栏是 Material3 对该断点的
 * 规范布局，手机竖屏、手表、手机横屏与折叠屏（Medium）保持单栏导航不变。
 * Medium 档预留：后续如需在折叠屏展开态启用，只改此函数与 [listPaneWidthFor]。
 */
fun twoPaneFor(widthClass: WindowWidthClass): Boolean = widthClass == WindowWidthClass.Expanded

/**
 * 双栏列表 pane 宽度纯函数（可单元测试）。
 *
 * 仅双栏启用时有意义；未启用档位返回 [Dp.Unspecified]（调用方不得使用）。
 * 400dp 取自 Material3 导航抽屉规范宽度量级：容纳卡片摘要的同时
 * 给详情 pane 留出 ≥440dp（1280dp 平板）的阅读宽度。
 */
fun listPaneWidthFor(widthClass: WindowWidthClass): Dp =
    if (twoPaneFor(widthClass)) 400.dp else Dp.Unspecified

/** 当前是否启用双栏（随窗口宽度分类切换：旋转、分屏、折叠屏展开实时生效）。 */
@Composable
fun rememberTwoPane(): Boolean = rememberWindowSizeClass().let { twoPaneFor(it.widthClass) }

/** 当前双栏列表 pane 宽度；仅在 [rememberTwoPane] 为 true 时有意义。 */
@Composable
fun listPaneWidth(): Dp = listPaneWidthFor(rememberWindowSizeClass().widthClass)

/**
 * 内容宽度封顶修饰符。
 *
 * 封顶值为 [Dp.Infinity]（手机竖屏与手表）时原样返回，不施加任何限制——
 * 不可直接把 Infinity 传进 widthIn：Dp.Infinity.roundToPx() 会得到
 * Int.MAX_VALUE，使子节点约束上限变成 21 亿 px，内部 fillMaxWidth 的
 * 子节点会按该值测量。Medium/Expanded 下等价于 widthIn(max = 封顶值)，
 * 配合 Box 的 contentAlignment = TopCenter 实现居中。
 *
 * 注意修饰符顺序：置于 fillMaxSize() 之后。FillNode 上报的是子节点实际尺寸
 * （见 FillNode.measure 的 layout(placeable.width, placeable.height)），
 * 因此内层封顶会如实反馈到外层占位，居中才能生效。
 */
fun Modifier.contentWidthCap(maxWidth: Dp): Modifier =
    if (maxWidth == Dp.Infinity) this else this.widthIn(max = maxWidth)

/**
 * 对话框正文最大高度：取屏幕高度的 62%。
 *
 * AlertDialog 的正文槽位既不限高也不滚动，内容超高时按钮会被顶出屏幕；
 * 用该高度兜底后，正文在内部滚动，按钮始终可见。内容不超高时与普通 Column 无异。
 */
@Composable
fun dialogContentMaxHeight(): Dp = (LocalConfiguration.current.screenHeightDp * 0.62f).dp

/** 对话框正文容器：限制最大高度并可纵向滚动；内容不超限时表现与普通 Column 一致。 */
@Composable
fun DialogContentColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(max = dialogContentMaxHeight())
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** 对话框操作按钮的动作描述。 */
data class DialogAction(
    val label: String,
    val emphasized: Boolean = false,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * 对话框操作按钮组。
 *
 * 使用 FlowRow 自动换行：按钮总宽超过对话框宽度时折到下一行，
 * 替代固定单行 Row——后者在窄手机与手表上会把按钮直接裁掉。
 *
 * compact 模式（手表档全屏外壳底部操作区）：36dp 胶囊底色按钮 + labelMedium 小字。
 * 常规 TextButton 遵循 M3 的 40dp 最小高度与宽松内边距，四个操作在约 274dp 宽的
 * 手表内容区会折成三行、连同间距占据约 130dp——这是 v2.17.3 之前手表上
 * 「底部操作区太大、上方滚动区太小」的主因（用户截图实测）。胶囊按钮折两行
 * 约 78dp，为滚动区多让出约 50dp；底色也给了小屏更清晰的触控范围。
 * 非紧凑路径（常规手机/平板对话框）渲染完全不变。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogActionButtons(
    actions: List<DialogAction>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 4.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 4.dp)
    ) {
        actions.forEach { action ->
            if (compact) {
                // 胶囊尺寸：36dp 高、水平 14dp 内边距；强调动作用实心主色，
                // 其余用次级容器色，破坏性动作文字标红。
                if (action.emphasized) {
                    Button(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = if (action.destructive) {
                            ButtonDefaults.filledTonalButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = action.onClick,
                    enabled = action.enabled
                ) {
                    Text(
                        text = action.label,
                        fontWeight = if (action.emphasized) FontWeight.SemiBold else null,
                        color = if (action.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }
        }
    }
}

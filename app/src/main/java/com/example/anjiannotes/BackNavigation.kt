package com.example.anjiannotes

/**
 * 系统返回「逐级回退」的显式优先级契约。
 *
 * v2.18.0 起，详情页按系统返回时分层消化：先收起正文搜索栏，再退出专注阅读，
 * 两者都不满足才执行最终保存并离开。此前这段优先级内联在 BackHandler 的 when
 * 里、无法单独验证；抽成纯函数后与 shouldForceFinalDraftSave 等退出策略同源，
 * 由 BackNavigationTest 固化，避免将来新增浮层时层级被无意打乱。
 *
 * 本文件只做「该执行哪个动作」的判定，不含任何状态写入或导航副作用；调用方据
 * 返回值决定状态转移，行为与原内联 when 完全等价。
 */
internal enum class DetailBackAction {
    /** 收起正文搜索栏，并清空查询与命中序号。 */
    CLOSE_SEARCH,

    /** 退出专注阅读，恢复普通预览。 */
    EXIT_FOCUS,

    /** 最终保存并离开详情页。 */
    LEAVE
}

/**
 * 按「搜索 > 专注 > 离开」的优先级解析一次系统返回应执行的动作。
 *
 * 搜索栏优先级最高：即使同时处于专注阅读（理论上专注模式下搜索栏不可见，
 * 但状态可能残留），也先消化搜索态，保证一次返回只做一件事、逐层收敛。
 */
internal fun resolveDetailBackAction(
    searchVisible: Boolean,
    focusMode: Boolean
): DetailBackAction = when {
    searchVisible -> DetailBackAction.CLOSE_SEARCH
    focusMode -> DetailBackAction.EXIT_FOCUS
    else -> DetailBackAction.LEAVE
}

/**
 * 设置层级的逐级回退目标：外观 / 动画 → 设置 → 列表。
 *
 * 系统返回与页面自带的返回箭头共用此决策点，保证两条入口行为恒定一致，
 * 不会因将来新增设置子页而各自漂移。
 */
internal fun settingsBackTarget(page: AppPage): AppPage = when (page) {
    AppPage.Appearance, AppPage.Motion -> AppPage.Settings
    else -> AppPage.List
}

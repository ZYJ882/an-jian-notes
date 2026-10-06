package com.example.anjiannotes.ui

/**
 * 笔记详情页正文搜索的纯匹配逻辑，与 UI 解耦，便于单元测试。
 *
 * 匹配规则：
 * - 大小写不敏感（中文不受影响，英文自动覆盖大小写变体）；
 * - 命中区间互不重叠（上一次命中的结尾即下一次查找的起点）；
 * - 偏移均基于「渲染后的展示文本」，Markdown 语法标记不参与匹配，
 *   因此 `**加粗**` 命中的是显示出来的加粗文字本身。
 */
fun findSearchMatchRanges(text: String, query: String): List<IntRange> {
    val trimmedQuery = query.trim()
    if (text.isEmpty() || trimmedQuery.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var from = 0
    val lastPossibleStart = text.length - trimmedQuery.length
    while (from <= lastPossibleStart) {
        val index = text.indexOf(trimmedQuery, from, ignoreCase = true)
        if (index < 0) break
        ranges += IntRange(index, index + trimmedQuery.length - 1)
        from = index + trimmedQuery.length
    }
    return ranges
}

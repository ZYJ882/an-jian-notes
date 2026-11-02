package com.example.anjiannotes.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 单位纪律守护测试（源码扫描）。
 *
 * 把三条硬规则变成可执行契约，任何后续改动违反都会在 ./gradlew test 直接失败：
 *
 * 1. 控件尺寸一律用 dp、文字一律用 sp —— 禁止 sp 流入尺寸类 Modifier
 *    （size/padding/width/height/offset/sizeIn/spacedBy/PaddingValues），
 *    禁止 dp 流入文字属性（fontSize/lineHeight/letterSpacing）；
 * 2. 禁止直接使用 px 写死尺寸 —— 只允许「dp 定义 + with(density){ Dp.toPx() }」
 *    的 Canvas 正规转换（本测试扫描不到 .toPx()，因其前面不是数字字面量）；
 * 3. 禁止全局缩放界面 —— 禁止改写 fontScale、updateConfiguration、
 *    覆盖 LocalDensity；仅允许读取 LocalDensity.current 做 Canvas 换算。
 *
 * 扫描前先剥离注释与字符串字面量，避免注释里提到「16.sp」之类的示例误报。
 */
class UnitDisciplineTest {

    private data class Violation(val file: String, val line: Int, val rule: String, val text: String)

    /** 硬编码 px 字面量：数字（可含小数与 f 后缀）紧跟 .px。 */
    private val pxLiteral = Regex("""\d+(\.\d+)?f?\.px\b""")

    /** sp 流入尺寸类修饰符或布局参数。 */
    private val spInDimension = Regex(
        """\.(size|padding|width|height|offset|sizeIn|requiredSize|requiredWidth|requiredHeight|paddingFrom)\([^)]*\d+(\.\d+)?\.sp\b"""
    )
    private val spInArrangement = Regex("""(spacedBy|PaddingValues)\([^)]*\d+(\.\d+)?\.sp\b""")

    /** dp 流入文字属性。 */
    private val dpInText = Regex("""(fontSize|lineHeight|letterSpacing)\s*=\s*[^,)]*\.dp\b""")

    /** 全局缩放机制。 */
    private val fontScaleWrite = Regex("""\bfontScale\b""")
    private val densityOverride = Regex("""\bdensity\s*=""")
    private val configurationMutation = Regex("""updateConfiguration|LocalDensity\s+provides|Configuration\(""")

    @Test
    fun mainSourceRespectsUnitDiscipline() {
        val sourceRoot = findSourceRoot()
        val files = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .toList()
        assertTrue("未扫描到任何源码文件：$sourceRoot", files.isNotEmpty())

        val violations = mutableListOf<Violation>()
        for (file in files) {
            val stripped = stripCommentsAndStrings(file.readText())
            stripped.lineSequence().forEachIndexed { index, line ->
                val lineNumber = index + 1
                val relative = file.relativeTo(sourceRoot).path
                if (pxLiteral.containsMatchIn(line)) {
                    violations += Violation(relative, lineNumber, "禁止硬编码 px 字面量", line.trim())
                }
                if (spInDimension.containsMatchIn(line) || spInArrangement.containsMatchIn(line)) {
                    violations += Violation(relative, lineNumber, "禁止 sp 用于尺寸", line.trim())
                }
                if (dpInText.containsMatchIn(line)) {
                    violations += Violation(relative, lineNumber, "禁止 dp 用于文字", line.trim())
                }
                if (fontScaleWrite.containsMatchIn(line) ||
                    configurationMutation.containsMatchIn(line) ||
                    (densityOverride.containsMatchIn(line) && !line.contains("LocalDensity"))
                ) {
                    violations += Violation(relative, lineNumber, "禁止全局缩放界面", line.trim())
                }
            }
        }

        assertTrue(
            buildString {
                appendLine("发现 ${violations.size} 处违反单位纪律的代码：")
                violations.forEach { appendLine("  [${it.rule}] ${it.file}:${it.line}  ${it.text}") }
            },
            violations.isEmpty()
        )
    }

    /** 从测试工作目录逐级向上查找 app/src/main/java。 */
    private fun findSourceRoot(): File {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java")
        )
        candidates.firstOrNull { it.isDirectory }?.let { return it }
        var dir = File("").absoluteFile
        repeat(5) {
            val candidate = File(dir, "app/src/main/java")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile ?: return@repeat
        }
        error("未找到源码根目录（cwd=${File("").absolutePath}），无法执行单位纪律扫描")
    }

    /**
     * 剥离注释与字符串/字符字面量（保留换行以保证行号准确）。
     * 被剥离的内容替换为空白，避免把两侧 token 粘成一个新 token。
     */
    private fun stripCommentsAndStrings(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        val n = source.length
        while (i < n) {
            val c = source[i]
            when {
                c == '/' && i + 1 < n && source[i + 1] == '/' -> {
                    while (i < n && source[i] != '\n') i++
                }

                c == '/' && i + 1 < n && source[i + 1] == '*' -> {
                    i += 2
                    while (i < n && !(source[i] == '*' && i + 1 < n && source[i + 1] == '/')) {
                        if (source[i] == '\n') out.append('\n')
                        i++
                    }
                    i = minOf(i + 2, n)
                }

                c == '"' -> {
                    i++
                    while (i < n && source[i] != '"') {
                        if (source[i] == '\\') i++
                        if (i < n && source[i] == '\n') out.append('\n')
                        i++
                    }
                    i = minOf(i + 1, n)
                    out.append(' ')
                }

                c == '\'' -> {
                    i++
                    while (i < n && source[i] != '\'') {
                        if (source[i] == '\\') i++
                        i++
                    }
                    i = minOf(i + 1, n)
                    out.append(' ')
                }

                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }
}

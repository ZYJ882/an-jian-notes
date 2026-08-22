# 安笺（AnJian Notes）

**安笺**是一款采用 Kotlin 与 Jetpack Compose 开发的安卓原生离线笔记应用。所有笔记均使用 Room 数据库保存在设备本地；应用不请求网络权限，也不依赖账户登录。

## 已实现的功能

| 功能 | 说明 |
|---|---|
| 新建与编辑 | 支持标题、正文与笔记颜色编辑；标题和正文均为空时不会保存空白笔记。 |
| 本地持久化 | 使用 Room/SQLite 保存笔记；应用重新启动后内容仍可读取。 |
| 搜索 | 可按标题和正文进行关键词检索。 |
| 置顶 | 置顶笔记始终显示在列表顶部，并按更新时间排序。 |
| 删除确认 | 删除已有笔记前显示确认提示，避免误操作。 |
| 深色模式 | 自动跟随 Android 系统的深浅色设置，并使用低刺激暖灰配色。 |
| Markdown 编辑与预览 | 编辑和预览均在同一详情页内切换；预览会按照已保存的笔记格式显示，不会将纯文本误标记为 Markdown；支持标题、粗体、斜体、删除线、行内代码、列表、引用、分隔线和标准管道表格的轻量预览。 |
| 加号创建菜单 | 点击右下角加号，可新建笔记、导入文件或从剪切板导入；导入文件会直接调起系统文件管理器，并标注支持 `.md`、`.markdown`、`.txt` 和 UTF-8 文本。 |
| 自动格式识别 | 新建或从剪切板导入的内容会根据标题、列表、引用、强调、代码等常见语法自动判定 Markdown 或纯文本；可在详情页中手动覆盖。 |
| 收藏夹 | 首次打开自动创建“默认收藏夹”；顶部横向切换收藏夹；在当前收藏夹中新建、导入文件或导入剪切板文本时，笔记会自动归属当前收藏夹；详情页可将笔记移动到其他收藏夹。创建操作会在成功或失败时显示明确反馈。 |
| 备份与恢复 | 顶部“备份”入口支持 JSON 结构化备份和 TXT 明文备份；两种文件均可选择导入，确认后会以备份内容替换当前本地数据。 |
| 预览与编辑 | 单击列表笔记后默认进入预览页；双击标题或正文会在当前页面位置切换到编辑态，并将正文光标定位到双击对应的字符附近；右上角可手动切换“编辑 / 预览”。 |
| 链接交互 | 预览页中的 Markdown 链接和纯文本链接可直接打开系统浏览器；编辑页只提供文本输入，不会触发任何链接跳转。 |
| 详情布局 | 元信息区域会自适应换行，避免长日期和收藏夹名称将容器异常拉高。 |
| 快速响应 | 搜索采用 80ms 极短防抖；列表和详情页面使用 220ms 缩放淡入与 120ms 淡出过渡；数据库容器由应用级懒初始化管理。 |

## 项目结构

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/example/anjiannotes/MainActivity.kt` | Compose 页面、Markdown 编辑弹窗及交互入口。 |
| `app/src/main/java/com/example/anjiannotes/ui/Markdown.kt` | 轻量级 Markdown 预览、摘要与语法提示组件。 |
| `app/src/main/java/com/example/anjiannotes/ui/ImportSupport.kt` | 导入文件读取、格式自动识别和编辑器种子数据。 |
| `app/src/main/java/com/example/anjiannotes/AnJianApplication.kt` | 应用级数据库容器与 Room 迁移配置。 |
| `app/src/main/java/com/example/anjiannotes/NotesViewModel.kt` | 搜索、保存、置顶、删除等界面状态与业务逻辑。 |
| `app/src/main/java/com/example/anjiannotes/data/NoteData.kt` | Room 实体、DAO、数据库与仓库。 |
| `app/src/main/java/com/example/anjiannotes/ui/theme/` | Material 3 深浅色主题。 |
| `app/build.gradle.kts` | Android 模块依赖与构建参数。 |

## 构建要求

本项目使用 **JDK 17**、Android SDK Platform 35、Build Tools 35.0.0、Gradle 8.7 和 Android Gradle Plugin 8.6.1。首次构建时，Gradle 会下载所需依赖。

```bash
# 进入源码目录
cd android-notes

# Linux/macOS：设置 JDK 17 后构建调试包
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
```

Windows PowerShell 可执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 的默认输出位置为：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装与验证

如已配置 Android Platform Tools 且设备已启用 USB 调试，可通过以下命令安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

建议依次验证新建笔记、搜索正文关键词、置顶、重启应用后的持久化结果，以及系统深浅色切换后的显示效果。

## 本次构建结果

调试 APK 已在 JDK 17 环境下成功构建。它是使用调试签名生成的测试安装包，不适用于上架或生产分发；正式发布前应配置专属签名密钥、应用图标、隐私政策与备份/恢复策略。

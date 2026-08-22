# 安笺（AnJian Notes）

**安笺**是一款采用 Kotlin 与 Jetpack Compose 开发的安卓原生离线笔记应用。所有笔记均使用 Room 数据库保存在设备本地；应用不请求网络权限，也不依赖账户登录。

## 已实现的功能

| 功能 | 说明 |
|---|---|
| 新建与编辑 | 支持标题、正文、笔记颜色及标签编辑；标题和正文均为空时不会保存空白笔记。 |
| 本地持久化 | 使用 Room/SQLite 保存笔记；应用重新启动后内容仍可读取。 |
| 搜索 | 可按标题、正文和标签进行关键词检索。 |
| 标签 | 输入以英文或中文逗号分隔的标签，展示为标签胶囊。 |
| 置顶 | 置顶笔记始终显示在列表顶部，并按更新时间排序。 |
| 删除确认 | 删除已有笔记前显示确认提示，避免误操作。 |
| 深色模式 | 自动跟随 Android 系统的深浅色设置，并使用低刺激暖灰配色。 |
| Markdown 编辑与预览 | 编辑器内可单独开启 Markdown，支持标题、粗体、斜体、删除线、行内代码、列表、引用与分隔线的轻量预览。 |
| 快速响应 | 搜索采用 80ms 极短防抖；界面展开和内容尺寸动效控制在 80–140ms；数据库容器由应用级懒初始化管理。 |

## 项目结构

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/example/anjiannotes/MainActivity.kt` | Compose 页面、Markdown 编辑弹窗及交互入口。 |
| `app/src/main/java/com/example/anjiannotes/ui/Markdown.kt` | 轻量级 Markdown 预览、摘要与语法提示组件。 |
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

建议依次验证新建笔记、为笔记添加多个标签、搜索正文关键词、置顶、重启应用后的持久化结果，以及系统深浅色切换后的显示效果。

## 本次构建结果

调试 APK 已在 JDK 17 环境下成功构建。它是使用调试签名生成的测试安装包，不适用于上架或生产分发；正式发布前应配置专属签名密钥、应用图标、隐私政策与备份/恢复策略。

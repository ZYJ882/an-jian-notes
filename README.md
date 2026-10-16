# 安笺（AnJian Notes）

**安笺**是一款使用 Kotlin、Jetpack Compose 与 Room 构建的安卓原生离线笔记应用。笔记数据保存在设备本地；网络权限仅用于用户主动配置的 WebDAV 增量备份，不依赖账户登录或云端同步服务。

## 主要功能

| 功能 | 说明 |
|---|---|
| 笔记编辑 | 支持标题、正文、星标、收藏夹与纯文本/Markdown 格式。已有笔记默认以预览方式打开，可双击内容或使用编辑按钮进入编辑状态。 |
| 自动保存 | 每次编辑都会标记为待保存；停止输入约 700ms 后自动写入。新建笔记首次编辑立即创建草稿，返回前会等待最后一次写入完成。 |
| 两级返回 | 编辑状态第一次返回会保存并停留在同一篇笔记的预览页；预览状态再次返回才回到笔记列表。页面返回、物理返回键、三键导航和系统手势使用同一返回逻辑。 |
| 本地数据 | 使用 Room/SQLite 保存笔记和收藏夹；首次运行自动创建“默认收藏夹”。 |
| Markdown | 自动识别常见 Markdown 语法，也可手动切换格式；预览支持标题、强调、列表、引用、代码、分隔线、链接和表格。 |
| 搜索与收藏夹 | 支持按标题和正文搜索，可创建、切换、移动和删除自定义收藏夹。删除时笔记会安全迁回默认收藏夹；默认收藏夹与“星标笔记”系统入口受保护。星标不会改变笔记原有收藏夹归属。 |
| 列表多选 | 长按任意笔记可进入多选模式；随后可点击增减选择，并批量加入星标或批量删除。 |
| 导入与导出 | 支持 TXT、Markdown、剪切板导入；支持 JSON、TXT 与 Markdown ZIP 本地备份和恢复。 |
| WebDAV | 支持 HTTPS WebDAV 增量备份，笔记以可直接阅读的 Markdown 文件保存，并附带 JSON 元数据。 |
| 外观主题 | 提供浅色、深色与跟随系统三种模式，切换后即时生效。 |
| 窗口尺寸适配 | 按当前窗口尺寸分类切换布局（Material3 断点 + 手表专用档）：Watch（< 320dp）手表/分屏小窗最紧凑策略，Compact（320–599dp）手机竖屏布局不变，Medium（600–839dp）横屏/折叠屏内边距放宽且内容限宽居中，Expanded（≥ 840dp，平板）在 Medium 策略基础上追加「列表-详情」双栏。控件尺寸一律 dp、文字一律 sp，禁止 px 写死与全局缩放（有源码守护测试）。 |

## 源码结构

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/example/anjiannotes/MainActivity.kt` | Activity 入口、应用外壳（NotesApp 导航与对话框编排）、窗口尺寸类状态与双栏保存协议（离开意图与令牌）。 |
| `app/src/main/java/com/example/anjiannotes/NoteListPage.kt` | 列表页（单栏/双栏共用主体）、笔记卡片、列表空态与双栏详情空态。 |
| `app/src/main/java/com/example/anjiannotes/NoteDetailPage.kt` | 详情页与自动保存状态机（防抖、最终保存、预览/编辑切换）。 |
| `app/src/main/java/com/example/anjiannotes/SettingsPages.kt` | 设置页、外观页、WebDAV 与备份对话框、底部快速输入栏。 |
| `app/src/main/java/com/example/anjiannotes/FolderDialogs.kt` | 收藏夹相关对话框与抽屉项。 |
| `app/src/main/java/com/example/anjiannotes/Dialogs.kt` | 通用确认对话框。 |
| `app/src/main/java/com/example/anjiannotes/NotesViewModel.kt` | 笔记、搜索、收藏夹、备份与 WebDAV 的界面业务逻辑。 |
| `app/src/main/java/com/example/anjiannotes/data/NoteData.kt` | Room 实体、DAO、数据库迁移与仓库。 |
| `app/src/main/java/com/example/anjiannotes/data/BackupSupport.kt` | JSON、TXT 与 Markdown ZIP 的备份编解码。 |
| `app/src/main/java/com/example/anjiannotes/data/WebDavSupport.kt` | WebDAV 配置存储、加密与增量上传。 |
| `app/src/main/java/com/example/anjiannotes/ui/` | Markdown 渲染、文本导入及格式识别。 |
| `app/src/main/java/com/example/anjiannotes/ui/WindowSizeClass.kt` | 窗口尺寸分类（宽度四档 + 高度三档，Material3 断点）与响应式判定。 |
| `app/src/main/java/com/example/anjiannotes/ui/AdaptiveLayout.kt` | 分类驱动的布局分档（内边距、内容限宽、抽屉限宽、双栏开关与列表 pane 宽度）与对话框自适应组件。 |
| `app/src/main/java/com/example/anjiannotes/ui/theme/` | Material 3 主题、配色与外观偏好持久化。 |
| `app/src/test/` | 备份、Markdown、导入、数据模型、窗口尺寸分类与单位纪律守护的单元测试。 |

## 构建要求

项目使用 **JDK 17**、Android SDK Platform 35、Gradle 8.7、Android Gradle Plugin 8.6.1 和 Kotlin 2.0.21。仓库包含 Gradle Wrapper；首次构建会下载依赖。编译堆上限（`gradle.properties`）为 4GB，内存紧张的开发机可调低。

```bash
# 进入源码目录
cd android-notes

# 指向 JDK 17 与 Android SDK
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"

# 完整单元测试与调试构建
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

调试 APK 默认输出至：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 验证建议

完成安装后，建议验证下列关键流程：

1. 新建笔记，输入文字后立即使用系统侧滑返回；应先进入预览页且正文完整。
2. 在预览页再次返回列表，重新打开笔记；正文应保持一致。
3. 连续输入多次后立即返回；最终内容应为最后一次输入结果。
4. 输入内容后删除为空白再返回；已编辑笔记应保存最终空白状态，不应保留旧内容。
5. 分别验证 Markdown ZIP、TXT 和 JSON 的备份恢复，以及 WebDAV 增量备份。
6. 适配验证：在开发者选项里强制设定最小宽度（或使用模拟器/折叠屏/平板）为 320dp 以下、600dp、840dp 三档，确认页面内边距、内容限宽与对话框形态随窗口切换；手机竖屏（360–412dp）应与日常使用完全一致。
7. 双栏验证（≥ 840dp）：左侧点选笔记右侧即时显示；在右侧输入后立即点选另一篇，切回前者内容应已保存；从双栏旋转/分屏到单栏再回来，正在读的笔记不丢编辑。

单元测试覆盖上述保存语义、备份编解码、窗口尺寸分类边界、双栏分档契约与单位纪律守护（`./gradlew test`，守护测试会扫描主源码，任何 px 写死、sp/dp 混用或全局缩放都会直接失败；双栏契约测试会逐 dp 校验分栏开关与宽度分类对齐、以及最窄平板下双栏不超窗）。

## 源码包说明

发布的源码压缩包不包含 Android SDK 路径、构建缓存、APK、IDE 文件或签名材料。解压后按上述步骤创建本机 `local.properties` 或设置 `ANDROID_HOME`，即可直接构建。

## 许可证

本仓库当前未附带单独的开源许可证文件。使用、分发或二次开发前，请由仓库所有者确定适用的授权条款。

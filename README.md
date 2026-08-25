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
| 搜索与收藏夹 | 支持按标题和正文搜索，可创建、切换和移动收藏夹。侧边栏固定提供“星标笔记”系统入口；星标不会改变笔记原有收藏夹归属。 |
| 导入与导出 | 支持 TXT、Markdown、剪切板导入；支持 JSON、TXT 与 Markdown ZIP 本地备份和恢复。 |
| WebDAV | 支持 HTTPS WebDAV 增量备份，笔记以可直接阅读的 Markdown 文件保存，并附带 JSON 元数据。 |
| 外观主题 | 提供浅色、深色与跟随系统三种模式，切换后即时生效。 |

## 源码结构

| 路径 | 职责 |
|---|---|
| `app/src/main/java/com/example/anjiannotes/MainActivity.kt` | Compose 页面、导航、详情编辑和保存队列入口。 |
| `app/src/main/java/com/example/anjiannotes/NotesViewModel.kt` | 笔记、搜索、收藏夹、备份与 WebDAV 的界面业务逻辑。 |
| `app/src/main/java/com/example/anjiannotes/data/NoteData.kt` | Room 实体、DAO、数据库迁移与仓库。 |
| `app/src/main/java/com/example/anjiannotes/data/BackupSupport.kt` | JSON、TXT 与 Markdown ZIP 的备份编解码。 |
| `app/src/main/java/com/example/anjiannotes/data/WebDavSupport.kt` | WebDAV 配置存储、加密与增量上传。 |
| `app/src/main/java/com/example/anjiannotes/ui/` | Markdown 渲染、文本导入及格式识别。 |
| `app/src/main/java/com/example/anjiannotes/ui/theme/` | Material 3 主题、配色与外观偏好持久化。 |
| `app/src/test/` | 备份、Markdown、导入和数据模型的单元测试。 |

## 构建要求

项目使用 **JDK 17**、Android SDK Platform 35、Gradle 8.7、Android Gradle Plugin 8.6.1 和 Kotlin 2.0.21。仓库包含 Gradle Wrapper；首次构建会下载依赖。

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

## 源码包说明

发布的源码压缩包不包含 Android SDK 路径、构建缓存、APK、IDE 文件或签名材料。解压后按上述步骤创建本机 `local.properties` 或设置 `ANDROID_HOME`，即可直接构建。

## 许可证

本仓库当前未附带单独的开源许可证文件。使用、分发或二次开发前，请由仓库所有者确定适用的授权条款。

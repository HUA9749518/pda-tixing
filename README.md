# PDA 消息兜底提醒 APP

工业 PDA（Android 7.1）上 Hi护士 等第三方 APP 的**通知兜底提醒**工具：当目标 APP 通知已进入系统通知栏时，强制再提醒一次（声音 + 振动 + 亮屏）。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/CLOUD_BUILD.md](docs/CLOUD_BUILD.md) | **推荐**：GitHub Actions 云构建 APK（无需本机 Android Studio） |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 开发需求（Cursor 可直接按此实现） |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 开发、构建、维护说明 |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | 现场 PDA 部署检查清单 |
| [docs/STATUS.md](docs/STATUS.md) | 项目实施完成度与待办 |

## 快速开始（推荐：云构建）

本机环境不齐时，用 GitHub Actions 出包：

1. 按 [docs/CLOUD_BUILD.md](docs/CLOUD_BUILD.md) 新建仓库并 `git push`
2. 打开仓库 **Actions** → 下载 **pda-alert-relay-debug** Artifact
3. 将 APK 安装到 PDA，按 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) 配置权限

## 本地构建（可选）

需本机 JDK 11+ 与 Android SDK。用 Android Studio 打开本目录 Sync 后：

```bash
gradlew assembleDebug
# 或
gradlew assembleRelease
```

## 能力边界

- ✅ 目标 APP **已有系统通知**但提醒不到位 → 兜底有效
- ❌ 目标 APP 进程被杀且未产生通知 → 无法兜底
- ❌ WLAN 漫游导致消息未到设备 → 无法兜底

## 技术栈

- Java · minSdk 25 · targetSdk 25
- NotificationListenerService + ForegroundService
- 完全离线，无网络权限

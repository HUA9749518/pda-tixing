# PDA 消息兜底提醒 APP — 开发与维护文档

---

## 1. 项目概览

> **完成度清单见 [STATUS.md](STATUS.md)**

| 项 | 说明 |
|----|------|
| 应用名 | PDA 消息兜底 |
| 包名 | `com.pda.alertrelay` |
| minSdk / targetSdk | 25（Android 7.1） |
| 语言 | Java |
| 构建 | Gradle 7.x + Android Gradle Plugin 7.4.x |

**核心思路**：`NotificationListenerService` 监听目标 APP 通知 → `AlertHelper` 叠加高优先级兜底通知 + 独立响铃/振动/亮屏；`KeepAliveService` 前台保活并看门狗 NLS。

---

## 2. 目录结构

```
pda-tixing/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/pda/alertrelay/
│   │   ├── PdaAlertApp.java
│   │   ├── ui/
│   │   │   ├── PermissionActivity.java    # 权限引导（Launcher）
│   │   │   └── MainActivity.java          # 设置主界面
│   │   ├── service/
│   │   │   ├── KeepAliveService.java      # 前台保活 + NLS 看门狗
│   │   │   └── AlertNotificationListenerService.java
│   │   ├── receiver/
│   │   │   └── BootReceiver.java
│   │   ├── model/
│   │   │   └── AlertRecord.java
│   │   └── util/
│   │       ├── AlertHelper.java           # 兜底通知 + 声光
│   │       ├── NotificationDeduper.java
│   │       ├── PermissionHelper.java
│   │       └── PreferenceHelper.java
│   └── res/
├── docs/
│   ├── REQUIREMENTS.md
│   ├── DEVELOPMENT.md
│   └── DEPLOYMENT.md
└── README.md
```

---

## 3. 关键类说明

### 3.1 AlertNotificationListenerService

- 继承 `NotificationListenerService`
- `onNotificationPosted`：过滤包名、去重、调用 `AlertHelper`
- `onListenerConnected` / `onListenerDisconnected`：更新 `KeepAliveService` 连接状态
- 断开时调用 `requestRebind(this)`

### 3.2 KeepAliveService

- `startForeground` 显示低干扰「监控运行中」通知
- 静态标志 `sListenerConnected` 供 UI 展示
- `onStartCommand` 返回 `START_STICKY`

### 3.3 AlertHelper

- 从 `StatusBarNotification` 提取 title/text
- 生成唯一 notifyId（避免覆盖）
- WakeLock 3~5 秒（可配置开关）
- Ringtone + Vibrator 独立于 Notification（双通道可靠）
- Handler 延时 cancel 实现 30s/60s 停留

### 3.4 NotificationDeduper

- 缓存最近 N 个 `(packageName, tag, id)` 或 `notification.key`
- 5 秒窗口内相同 key 视为重复

### 3.5 PreferenceHelper

SharedPreferences 键：

| Key | 类型 | 默认 |
|-----|------|------|
| `target_package` | String | `""` |
| `alert_enabled` | boolean | true |
| `sound_enabled` | boolean | true |
| `vibrate_enabled` | boolean | true |
| `wake_screen_enabled` | boolean | true |
| `stay_duration` | int | 60（秒，0=手动清除） |
| `autostart_guide_done` | boolean | false |

---

## 4. 构建与运行

### 环境要求

- Android Studio Arctic Fox 或更高
- JDK 11
- Android SDK Platform 25

### 命令行

```bash
cd d:\CURSOR\pda-tixing
gradlew assembleRelease
# 输出：app/build/outputs/apk/release/app-release-unsigned.apk
```

### 调试

1. 安装 APK 到 PDA 或模拟器（API 25 镜像）
2. 完成权限引导
3. 监控包名可先填 **本 APP 包名** `com.pda.alertrelay`，用「测试」按钮验证
4. 联调时改为 Hi护士 真实包名

### 签名 Release

```bash
# 生成 keystore（仅首次）
keytool -genkey -v -keystore release.keystore -alias pdaalert -keyalg RSA -keysize 2048 -validity 10000

# 在 app/build.gradle 中配置 signingConfigs 后
gradlew assembleRelease
```

---

## 5. API 25 注意事项

1. **无 NotificationChannel**：直接使用 `Notification.Builder(context)`
2. **无 setTimeoutAfter**：用 `Handler(Looper.getMainLooper()).postDelayed`
3. **PRIORITY_MAX**：API 25 仍有效
4. **WakeLock**：`PowerManager.SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP`
5. **不要提高 targetSdk**：避免 Android 8+ 后台限制影响行为

---

## 6. 测试用例

| # | 步骤 | 期望 |
|---|------|------|
| T1 | 测试按钮 | 兜底通知 + 声 + 振 |
| T2 | Hi护士 发真实消息 | 双通知并存 |
| T3 | 关闭响铃开关 | 仅振动/通知 |
| T4 | 停留 60s | 60 秒内不消失 |
| T5 | 划掉 APP 最近任务 | 30 分钟内仍能兜底（若 ROM 允许） |
| T6 | 关闭通知使用权 | 保活页显示 NLS 未连接 |

---

## 7. 常见问题（开发侧）

**Q: NLS 收不到回调？**  
A: 确认通知使用权、目标 APP 确实 post 了通知、包名正确。

**Q: 兜底触发两次？**  
A: 检查 Deduper；部分 ROM 会 post 两次，可调窗口时间。

**Q: 自触发死循环？**  
A: 必须 filter 本 APP `packageName`。

**Q: 编译 SDK 33 但 target 25？**  
A: 可以；`compileSdk` 可高于 `targetSdk`，便于 AS 兼容，运行时行为以 target 25 为准。

---

## 8. 版本规划

| 版本 | 内容 |
|------|------|
| v1.0 | MVP：NLS 兜底 + 保活 + 设置 + 测试 + 最近 20 条 |
| v1.1 | 可选：同一条消息 30s 后再响一次（最多 2 次） |
| v1.2 | 可选：关键词加强提醒（简单 contains） |

---

## 9. 修改代码时的原则

1. 不增加网络权限与联网逻辑
2. 不删除第三方原通知
3. 新功能先更新 `REQUIREMENTS.md` 再实现
4. 保持 Java、minSdk 25

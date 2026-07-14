# PDA 消息兜底提醒 APP — 开发需求文档（修订版）

> 版本：v1.0 | 目标平台：Android 7.1（API 25）| 语言：Java  
> 用途：供 Cursor / 开发人员直接按本文档实现，勿添加文档外功能。

---

## 1. 产品定位

**Hi护士 通知镜像兜底器**：当 Hi护士（或指定第三方 APP）的通知**已进入系统通知栏**时，本 APP 强制再提醒一次（声音 + 振动 + 亮屏 + 高优先级展示）。

### 1.1 能力边界（必须写进验收与说明）

| 场景 | 本 APP 能否解决 |
|------|----------------|
| Hi护士 已发出系统通知，但无声/一闪/被压掉 | ✅ 能 |
| Hi护士 进程被杀，推送到了但未建通知 | ❌ 不能 |
| WLAN 漫游导致长连接断线，消息未到设备 | ❌ 不能 |

---

## 2. 运行环境

| 项 | 要求 |
|----|------|
| 系统 | Android 7.1（API 25） |
| 设备 | 工业手持 PDA，精简 ROM，无 Google 服务 |
| 网络 | 公司内网 WLAN（H3C AP，有漫游）；**本 APP 不联网** |
| 业务 APP | Hi护士（包名可配置） |

---

## 3. 核心目标（唯一）

当监控 APP 产生系统通知时：

- ✅ 保留原通知，**额外**新建一条独立兜底通知
- ✅ 必有顶部横幅/锁屏可见文字
- ✅ 必有铃声 + 振动（可独立开关）
- ✅ 熄屏时自动亮屏（可开关）
- ✅ 兜底通知在用户设定时长内不会一闪消失
- ✅ 极致轻量、低内存、长期后台存活

**禁止**：广告、埋点、统计、联网、云同步、AI 过滤、UI 美化、多余页面。

---

## 4. 技术架构

```
BootReceiver
    └── 启动 KeepAliveService（ForegroundService，看门狗 + 常驻通知）

AlertNotificationListenerService（NLS）
    └── onNotificationPosted → 过滤 → 兜底提醒

MainActivity / PermissionActivity
    └── 权限引导 + 设置 + 测试
```

### 4.1 组件职责

| 组件 | 职责 |
|------|------|
| `KeepAliveService` | 唯一前台服务；显示「监控运行中」；NLS 断开时 `requestRebind` + 重启自身 |
| `AlertNotificationListenerService` | 监听系统通知；过滤包名；触发兜底 |
| `BootReceiver` | 开机自启 KeepAliveService |
| `AlertHelper` | 构建兜底通知、Ringtone、Vibrator、WakeLock |
| `NotificationDeduper` | 按 notification key/id 去重，防重复回调 |
| `PreferenceHelper` | 本地配置持久化 |

**禁止**双进程守护、1 像素 Activity、WorkManager 等过度保活。

---

## 5. 功能清单（MVP）

### 5.1 权限引导页（首次 / 未授权时）

必须完成以下三项才能进入主界面：

1. **通知使用权**（Notification Listener Access）
2. **忽略电池优化**（REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）
3. **厂商自启动说明**（图文步骤，非系统 API；用户勾选「已完成」）

未全部完成则持续引导，不可跳过。

### 5.2 主界面（设置页）

| 配置项 | 说明 |
|--------|------|
| 监控包名 | 默认空，用户填写 Hi护士 实际包名 |
| 提醒总开关 | 开/关兜底功能 |
| 响铃 | 独立开关 |
| 振动 | 独立开关 |
| 锁屏亮屏 | 独立开关 |
| 通知停留 | 30 秒 / 60 秒 / 手动清除（默认 60 秒） |
| 保活状态 | 显示前台服务 + NLS 是否已连接 |
| 测试按钮 | 模拟一条来自监控包名的兜底通知 |
| 最近记录 | 可选：最近 20 条（时间、标题、内容），支持清空 |

### 5.3 后台核心逻辑

```
onNotificationPosted(sbn)
  IF 提醒总开关关闭 → return
  IF sbn.packageName == 本 APP 包名 → return（防自触发）
  IF sbn.packageName != 监控包名 → return
  IF NotificationDeduper.isDuplicate(sbn) → return
  → AlertHelper.showFallback(sbn)
  → 可选写入最近 20 条记录
```

### 5.4 兜底通知参数（API 25）

- `Notification.PRIORITY_MAX`
- `setDefaults(Notification.DEFAULT_ALL)`
- `setVisibility(VISIBILITY_PUBLIC)`
- **不使用** NotificationChannel（API 26+）
- **不使用** `setTimeoutAfter`（API 26+）；用 `Handler.postDelayed` + `cancel(id)` 实现停留时长
- 响铃：Notification sound + 独立 `RingtoneManager` 双通道
- 振动：独立 `Vibrator`
- 亮屏：`PowerManager.WAKE_LOCK`（短持有 3~5 秒）

### 5.5 保活

1. 前台服务常驻
2. `onListenerDisconnected()` → `requestRebind()` + 延迟重启 KeepAliveService
3. `BOOT_COMPLETED` 开机自启
4. 部署文档要求用户完成厂商白名单（见 DEPLOYMENT.md）

---

## 6. 权限清单

| 权限 / 能力 | 用途 |
|-------------|------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | NLS |
| `RECEIVE_BOOT_COMPLETED` | 开机自启 |
| `WAKE_LOCK` | 熄屏亮屏 |
| `VIBRATE` | 振动 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 电池白名单 |
| 用户手动：通知使用权 | 系统设置 |

**不要**声明：`INTERNET`、读写存储（除非后续版本加导出）。

---

## 7. 验收标准

1. Hi护士 发出可见通知后，**3 秒内**出现兜底通知（声/振/亮屏按开关）
2. 原 Hi护士 通知**不被 cancel**
3. 锁屏静置 **30 分钟** 后，测试通知仍能触发兜底
4. 兜底通知在设定时长内**不会一闪消失**（30s/60s 模式）
5. **负向**：Hi护士 无任何系统通知时，本 APP **不承诺**提醒

---

## 8. 交付物

- [ ] Android Studio 工程（Java，`minSdk 25`，`targetSdk 25`）
- [ ] Release APK
- [ ] `docs/DEVELOPMENT.md` — 开发与维护说明
- [ ] `docs/DEPLOYMENT.md` — PDA 现场部署检查清单
- [ ] `README.md` — 项目概览

---

## 9. 明确禁止

- 联网、云同步、统计、广告
- AI 过滤、通知分类引擎
- Kotlin（本项目统一 Java）
- targetSdk ≥ 28 的后台限制行为（保持 25）
- 删除或修改第三方原通知

---

## 10. 包名与工程约定

| 项 | 值 |
|----|-----|
| 应用 ID | `com.pda.alertrelay` |
| 应用名 | PDA 消息兜底 |
| NLS 类名 | `AlertNotificationListenerService` |

Hi护士 实际包名由现场配置，开发阶段测试可用本 APP 包名或测试包名模拟。

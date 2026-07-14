# 云构建说明（GitHub Actions）

本机无需安装 Android Studio / JDK，在 GitHub 云端自动编译 APK。

---

## 一、首次准备（只需做一次）

### 1. 安装 Git（若尚未安装）

https://git-scm.com/download/win

### 2. 注册 / 登录 GitHub

https://github.com

### 3. 在 GitHub 新建仓库

1. 打开 https://github.com/new
2. **Repository name** 例如：`pda-tixing`
3. 选择 **Public**（私有仓库也可用，免费额度足够打 Debug APK）
4. **不要**勾选 “Add a README”（本地已有代码）
5. 点 **Create repository**

### 4. 把本地项目推上去

在 PowerShell 中执行（把 `你的用户名` 换成自己的 GitHub 用户名）：

```powershell
cd d:\CURSOR\pda-tixing

# 若尚未初始化（已初始化可跳过）
git init
git add .
git commit -m "Initial commit: PDA message fallback alert app"

git branch -M main
git remote add origin https://github.com/你的用户名/pda-tixing.git
git push -u origin main
```

浏览器登录 GitHub 后，按提示完成认证即可。

---

## 二、下载 APK

1. 打开仓库页面 → 顶部 **Actions**
2. 左侧点 **Build APK**，右侧选最近一次成功的运行（绿色勾）
3. 拉到页面底部 **Artifacts**
4. 下载 **pda-alert-relay-debug**
5. 解压得到 `app-debug.apk`，拷到 PDA 安装

产物保留 **30 天**，过期需重新跑一遍。

---

## 三、以后如何重新构建

任意下列方式都会触发构建：

| 方式 | 操作 |
|------|------|
| 推送代码 | `git push` 到 `main` / `master` |
| 手动触发 | Actions → Build APK → **Run workflow** |

手动触发步骤：

1. Actions → Build APK
2. 右侧 **Run workflow** → 选 `main` → Run workflow
3. 等待约 3~8 分钟 → 下载 Artifacts

---

## 四、常见问题

**Q: Actions 失败，提示 Permission denied: ./gradlew**  
A: 确认仓库里有 `gradlew`（无后缀），且 workflow 里有 `chmod +x gradlew`。

**Q: 下载不到 Artifact**  
A: 确认该次运行是绿色成功；失败的运行不会上传 APK。打开失败日志的 “Build Debug APK” 步骤查看原因。

**Q: 想打 Release 签名包**  
A: 当前流水线产出 Debug APK，足够现场测试。正式签名可在本地配置 keystore 后另开 `assembleRelease` job（需仓库 Secrets）。

**Q: 私有仓库每月有分钟限制吗？**  
A: 有免费额度，本项目单次构建通常几分钟，个人使用足够。

---

## 五、与现场部署的关系

拿到 APK 后，请按 [DEPLOYMENT.md](DEPLOYMENT.md) 在 PDA 上完成权限与白名单配置。

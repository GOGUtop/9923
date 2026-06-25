# 狗骨本地酒馆 · 云账号本地客户端 V1

目标链路：APP 内置 SillyTavern 本体 + 内置 Node，打开 APP 后本地启动 SillyTavern；输入服务器账号和密码后，从服务器现有 SillyTavern `data/账号名` 拉取资料，写入手机本地；本地游玩时监听 `data/default-user`，有变更自动上传回服务器对应账号文件夹。

## GitHub 编译 APK

把本仓库内容上传到 GitHub，然后：

`Actions → Build GouGu Cloud Local APK → Run workflow`

完成后在 Artifacts 下载 `狗骨本地酒馆-cloud-local-v1-debug-apk`。

## 服务器端

服务器只需要账号同步接口，不需要压缩包。把 `server/` 上传到服务器后：

```bash
cd server
npm install
TAVERN_DATA_DIR=/home/www/SillyTavern/data MASTER_PASSWORD=xixi PORT=5762 npm start
```

APP 里服务器接口填：

`http://你的服务器IP:5762/xixi-api`

账号是 `/home/www/SillyTavern/data/` 下面的文件夹名。第一版密码统一用 `MASTER_PASSWORD`，也可以在对应账号目录放 `xixi_password.txt` 作为独立密码。

## 说明

- GitHub Actions 会自动从官方 `https://github.com/SillyTavern/SillyTavern` 的 release 分支拉取 SillyTavern，并把 UI 皮肤代码直接注入到 SillyTavern 本体中再打进 APK。
- 当前内置 Node 使用 nodejs-mobile 最新可用 Android 版。SillyTavern 官方新版本要求 Node 20+，nodejs-mobile 当前公开 Android 版本主要是 Node 18 系列；如果运行时报 Node 版本不兼容，下一步需要换成自编译 Android Node 20 二进制。
- 第一版先跑通主链路，不加悬浮球、截图、小说化等附加功能。


## V1.1 闪退修复

这版修复了启动页显示 Node 启动中后直接闪退的问题：拦截 SillyTavern/依赖调用 process.exit，改为显示本地保护页，不再杀掉整个 APP。


# 狗骨本地酒馆 V1.2 ESM 修复版

修复点：

- 修复本地 Node 页面出现 `ERR_REQUIRE_ESM: require() of ES Module ... server.js not supported` 的问题。
- `xixi_bootstrap.js` 已从 `require(server.js)` 改成 `await import(file://server.js)`。
- 继续保留 `process.exit()` 拦截，避免 SillyTavern 启动失败时把 APP 直接闪退。

使用方式不变：上传到 GitHub，运行 Actions 编译 APK。

服务器 API 仍然是：

```bash
cd /home/www/xixi-api-server
npm install
TAVERN_DATA_DIR=/home/www/SillyTavern/data MASTER_PASSWORD=xixi PORT=5763 npm start
```

APP 里填：

```text
http://116.237.23.145:5763/xixi-api
```

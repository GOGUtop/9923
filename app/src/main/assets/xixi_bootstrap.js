'use strict';

const fs = require('fs');
const path = require('path');
const http = require('http');
const { pathToFileURL } = require('url');

const root = process.argv[2];
const port = Number(process.argv[3] || '8000');
const logPath = path.join(path.dirname(root || process.cwd()), 'gougu_node.log');

function log(x) {
  const line = `[GouGuBoot ${new Date().toISOString()}] ${x}\n`;
  try { fs.appendFileSync(logPath, line); } catch (_) {}
  try { console.log(line.trim()); } catch (_) {}
}

let fallbackStarted = false;
function htmlEscape(s) {
  return String(s || '').replace(/[&<>\"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c]));
}

function startFallbackServer(reason) {
  if (fallbackStarted) return;
  fallbackStarted = true;
  const message = reason && (reason.stack || reason.message || String(reason)) || '未知错误';
  log('Starting fallback HTTP server because SillyTavern failed: ' + message);
  const server = http.createServer((req, res) => {
    res.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
    res.end(`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>
body{margin:0;background:#07111f;color:#f8fafc;font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:28px;line-height:1.65}
.card{background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.14);border-radius:22px;padding:22px;box-shadow:0 16px 40px rgba(0,0,0,.28)}
h1{margin:0 0 8px;font-size:26px}.muted{color:#bdd5f3}pre{white-space:pre-wrap;background:rgba(0,0,0,.35);border-radius:14px;padding:14px;overflow:auto}
</style></head><body><div class="card"><h1>狗骨本地酒馆 · Node 已保护启动</h1><p class="muted">APP 没有闪退，但本地 SillyTavern 启动失败。现在会把真实错误显示在这里，方便继续修。</p><p>错误信息：</p><pre>${htmlEscape(message)}</pre><p class="muted">把这个页面截图给我，我就能继续精确修。</p></div></body></html>`);
  });
  server.on('error', err => log('Fallback server error: ' + (err.stack || err.message || err)));
  server.listen(port, '127.0.0.1', () => log('Fallback server listening on 127.0.0.1:' + port));
}

// SillyTavern 或依赖如果调用 process.exit，嵌入式 Node 可能直接把整个 APP 进程杀掉。
// 这里拦截 process.exit，避免出现“Node 启动中然后 APP 闪退”。
process.exit = function(code) {
  const err = new Error('Blocked process.exit(' + code + ') from embedded SillyTavern');
  err.exitCode = code;
  throw err;
};

process.on('uncaughtException', err => {
  log('uncaughtException: ' + (err.stack || err.message || err));
  startFallbackServer(err);
});
process.on('unhandledRejection', err => {
  log('unhandledRejection: ' + (err && (err.stack || err.message) || err));
  startFallbackServer(err);
});

(async function main() {
  try {
    if (!root || !fs.existsSync(root)) throw new Error('SillyTavern root not found: ' + root);
    process.chdir(root);
    process.env.NODE_ENV = process.env.NODE_ENV || 'production';
    process.env.PORT = String(port);
    process.env.SILLY_TAVERN_PORT = String(port);
    process.env.HOST = '127.0.0.1';
    process.env.GOUGU_LOCAL_APP = '1';
    process.env.FORCE_COLOR = '0';

    const serverJs = path.join(root, 'server.js');
    if (!fs.existsSync(serverJs)) throw new Error('server.js not found: ' + serverJs);

    process.argv = ['node', serverJs, '--port', String(port), '--host', '127.0.0.1', '--listen', 'false'];
    log('Node version=' + process.version);
    log('cwd=' + process.cwd());
    log('starting SillyTavern by dynamic import on 127.0.0.1:' + port);

    // SillyTavern 的 server.js 是 ES Module，不能用 require(server.js)。
    // 用动态 import 才不会触发 ERR_REQUIRE_ESM。
    await import(pathToFileURL(serverJs).href);
  } catch (e) {
    log('bootstrap failed: ' + (e.stack || e.message || e));
    startFallbackServer(e);
  }
})();

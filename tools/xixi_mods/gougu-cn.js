(function(){
  const dict = new Map(Object.entries({
    'Send':'发送','Continue':'继续','Impersonate':'扮演','Regenerate':'重新生成','Delete':'删除','Edit':'编辑','Save':'保存','Cancel':'取消','Settings':'设置','Characters':'角色','World Info':'世界书','Extensions':'扩展','API Connections':'接口连接','User Settings':'用户设置','Advanced Formatting':'高级格式','Import':'导入','Export':'导出','New Chat':'新建聊天','Start new chat':'新建聊天','Prompt':'提示词','Presets':'预设','Connect':'连接','Disconnect':'断开','Name':'名称','Description':'描述','First message':'开场白','Scenario':'场景','Personality':'性格','Examples':'示例','Search':'搜索','Close':'关闭','Back':'返回','Groups':'群组','Create':'创建','Duplicate':'复制','Rename':'重命名','Download':'下载','Upload':'上传','Refresh':'刷新','Clear':'清空','Enabled':'已启用','Disabled':'已关闭','Password':'密码','Username':'账号','Login':'登录'
  }));
  function replaceTextNode(n){ const t=n.nodeValue; const s=t.trim(); if(dict.has(s)) n.nodeValue=t.replace(s,dict.get(s)); }
  function walk(root){ try{ const w=document.createTreeWalker(root,NodeFilter.SHOW_TEXT,{acceptNode:n=>n.parentElement&&['SCRIPT','STYLE','TEXTAREA'].indexOf(n.parentElement.tagName)<0?NodeFilter.FILTER_ACCEPT:NodeFilter.FILTER_REJECT}); let n; while(n=w.nextNode()) replaceTextNode(n); document.querySelectorAll('[title],[aria-label],input[placeholder],textarea[placeholder]').forEach(el=>{['title','aria-label','placeholder'].forEach(a=>{const v=el.getAttribute(a); if(v&&dict.has(v.trim())) el.setAttribute(a,dict.get(v.trim()));});}); }catch(e){} }
  function badge(){ if(document.querySelector('.gougu-brand-badge')) return; const b=document.createElement('div'); b.className='gougu-brand-badge'; b.textContent='狗骨酒馆'; document.body.appendChild(b); }
  function run(){ badge(); walk(document.body); }
  new MutationObserver(()=>{clearTimeout(window.__gougu_cn_t); window.__gougu_cn_t=setTimeout(run,180);}).observe(document.documentElement,{childList:true,subtree:true});
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',run); else run();
})();

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const app = express();
app.use(cors());
app.use(express.json({limit:'200mb'}));
const DATA_DIR = process.env.TAVERN_DATA_DIR || '/home/www/SillyTavern/data';
const MASTER_PASSWORD = process.env.MASTER_PASSWORD || 'xixi';
const PORT = Number(process.env.PORT || 5762);
const SECRET = process.env.SYNC_SECRET || crypto.createHash('sha256').update(MASTER_PASSWORD + DATA_DIR).digest('hex');
const allowedRoots = ['chats','group chats','characters','worlds','User Avatars','backgrounds','QuickReplies','regex'];
const allowedFiles = new Set(['settings.json']);
function safeName(s){ if(!/^[\w.\-\u4e00-\u9fa5 ]+$/.test(s||'')) throw new Error('Bad account'); return s; }
function safePath(p){ p=String(p||'').replace(/\\/g,'/'); if(!p || p.includes('..') || p.startsWith('/')) throw new Error('Bad path'); const first=p.split('/')[0]; if(!allowedRoots.includes(first) && !allowedFiles.has(p)) throw new Error('Path not allowed: '+p); return p; }
function accDir(a){ return path.join(DATA_DIR, safeName(a)); }
function sha1(file){ return crypto.createHash('sha1').update(fs.readFileSync(file)).digest('hex'); }
function tokenFor(account){ return Buffer.from(account+':'+crypto.createHmac('sha256',SECRET).update(account).digest('hex')).toString('base64url'); }
function verify(account, token){ return token === tokenFor(account); }
function checkPassword(account, password){ if(password === MASTER_PASSWORD) return true; const f=path.join(accDir(account),'xixi_password.txt'); return fs.existsSync(f) && fs.readFileSync(f,'utf8').trim() === password; }
function listFiles(dir, base=dir, out=[]){ if(!fs.existsSync(dir)) return out; for(const name of fs.readdirSync(dir)){ const p=path.join(dir,name); const st=fs.statSync(p); if(st.isDirectory()) listFiles(p,base,out); else { const rel=path.relative(base,p).replace(/\\/g,'/'); try{ safePath(rel); out.push({path:rel,size:st.size,mtime:st.mtimeMs,sha1:sha1(p)}); }catch(e){} } } return out; }
app.get('/xixi-api/status',(req,res)=>res.json({ok:true,dataDir:DATA_DIR}));
app.get('/xixi-api/accounts',(req,res)=>{ try{ const password=req.query.password||''; if(password!==MASTER_PASSWORD) return res.status(403).json({ok:false,error:'密码错误'}); const arr=fs.readdirSync(DATA_DIR).filter(x=>fs.statSync(path.join(DATA_DIR,x)).isDirectory()); res.json({ok:true,accounts:arr}); }catch(e){res.status(500).json({ok:false,error:e.message});} });
app.post('/xixi-api/login',(req,res)=>{ try{ const {account,password}=req.body; if(!fs.existsSync(accDir(account))) return res.status(404).json({ok:false,error:'账号不存在'}); if(!checkPassword(account,password)) return res.status(403).json({ok:false,error:'密码错误'}); res.json({ok:true,account,token:tokenFor(account)}); }catch(e){res.status(400).json({ok:false,error:e.message});} });
app.get('/xixi-api/files',(req,res)=>{ try{ const account=safeName(req.query.account); if(!verify(account,req.query.token)) return res.status(403).json({ok:false,error:'token无效'}); res.json({ok:true,files:listFiles(accDir(account))}); }catch(e){res.status(400).json({ok:false,error:e.message});} });
app.get('/xixi-api/file',(req,res)=>{ try{ const account=safeName(req.query.account); if(!verify(account,req.query.token)) return res.status(403).send('token无效'); const rel=safePath(req.query.path); const f=path.join(accDir(account),rel); if(!fs.existsSync(f)) return res.status(404).send('not found'); res.sendFile(f); }catch(e){res.status(400).send(e.message);} });
app.post('/xixi-api/upload-file',(req,res)=>{ try{ const {account,token,path:rel,contentBase64}=req.body; if(!verify(safeName(account),token)) return res.status(403).json({ok:false,error:'token无效'}); const rp=safePath(rel); const target=path.join(accDir(account),rp); fs.mkdirSync(path.dirname(target),{recursive:true}); if(fs.existsSync(target)){ const bak=path.join(accDir(account),'.xixi_backups',rp+'.'+Date.now()+'.bak'); fs.mkdirSync(path.dirname(bak),{recursive:true}); fs.copyFileSync(target,bak); }
 fs.writeFileSync(target,Buffer.from(contentBase64,'base64')); res.json({ok:true,path:rp}); }catch(e){res.status(400).json({ok:false,error:e.message});} });
app.listen(PORT,'0.0.0.0',()=>console.log('GouGu xixi-api listening on '+PORT+' data='+DATA_DIR));

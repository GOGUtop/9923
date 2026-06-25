package com.xixijiuguan.gougulocaltavern;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Base64;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private LinearLayout loginPanel;
    private TextView statusText;
    private EditText serverEdit, accountEdit, passwordEdit;
    private Button loginButton, openButton;
    private ProgressBar progressBar;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private File tavernRoot;
    private File defaultUserDir;
    private String apiBase;
    private String account;
    private String token;
    private boolean nodeStarted = false;
    private boolean syncing = false;
    private boolean localServerReady = false;
    private final Map<String, String> fileState = new HashMap<>();
    private final int localPort = 8000;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#07111F"));
        getWindow().setNavigationBarColor(Color.parseColor("#07111F"));
        tavernRoot = new File(getFilesDir(), "local_tavern/SillyTavern");
        defaultUserDir = new File(tavernRoot, "data/default-user");
        apiBase = getPreferences(0).getString("apiBase", "http://116.237.23.145:5762/xixi-api");
        account = getPreferences(0).getString("account", "");
        token = getPreferences(0).getString("token", "");
        buildUi();
        prepareLocalTavernThenStartNode();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true);
        s.setDatabaseEnabled(true); s.setMediaPlaybackRequiresUserGesture(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view, String url){ injectMobileSkin(); }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        loginPanel = new LinearLayout(this);
        loginPanel.setOrientation(LinearLayout.VERTICAL);
        loginPanel.setPadding(dp(24), dp(48), dp(24), dp(24));
        loginPanel.setBackgroundColor(Color.parseColor("#07111F"));
        scroll.addView(loginPanel);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        TextView title = tv("狗骨本地酒馆", 30, true, "#FFF4D6");
        title.setGravity(Gravity.CENTER); title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        loginPanel.addView(title, lp(-1, -2, 0, 0, 0, dp(8)));
        TextView sub = tv("本地运行 SillyTavern · 云账号同步", 14, false, "#DDE8F6");
        sub.setGravity(Gravity.CENTER); loginPanel.addView(sub, lp(-1, -2, 0, 0, 0, dp(26)));

        statusText = tv("正在准备本地酒馆...", 14, false, "#DDE8F6");
        statusText.setBackground(makeBg("#132033", 18)); statusText.setPadding(dp(16), dp(12), dp(16), dp(12));
        loginPanel.addView(statusText, lp(-1, -2, 0, 0, 0, dp(14)));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100); progressBar.setProgress(0);
        loginPanel.addView(progressBar, lp(-1, dp(8), 0, 0, 0, dp(24)));

        serverEdit = input("服务器接口，例如 http://116.237.23.145:5762/xixi-api", apiBase);
        accountEdit = input("服务器账号名", account);
        passwordEdit = input("账号密码", ""); passwordEdit.setInputType(0x00000081);
        loginPanel.addView(label("服务器接口")); loginPanel.addView(serverEdit, lp(-1, dp(54), 0, 0, 0, dp(12)));
        loginPanel.addView(label("账号")); loginPanel.addView(accountEdit, lp(-1, dp(54), 0, 0, 0, dp(12)));
        loginPanel.addView(label("密码")); loginPanel.addView(passwordEdit, lp(-1, dp(54), 0, 0, 0, dp(20)));

        loginButton = btn("登录并拉取账号资料", "#2563EB");
        loginPanel.addView(loginButton, lp(-1, dp(56), 0, 0, 0, dp(12)));
        openButton = btn("进入本地酒馆 127.0.0.1:8000", "#16A34A");
        loginPanel.addView(openButton, lp(-1, dp(56), 0, 0, 0, dp(12)));
        Button reset = btn("清空本地账号数据", "#7F1D1D");
        loginPanel.addView(reset, lp(-1, dp(56), 0, 0, 0, dp(12)));

        loginButton.setOnClickListener(v -> loginAndPullAccount());
        openButton.setOnClickListener(v -> openLocalTavern());
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("清空本地数据？").setMessage("只清手机本地，不删服务器。")
                .setPositiveButton("清空", (d,w)-> new Thread(() -> { deleteRec(defaultUserDir); defaultUserDir.mkdirs(); runUi("已清空本地 data/default-user", 0); }).start())
                .setNegativeButton("取消", null).show());
        setContentView(root);
    }

    private TextView label(String t){ TextView v=tv(t,13,true,"#AFC7E8"); v.setPadding(dp(4),0,0,dp(6)); return v; }
    private TextView tv(String t,int sp,boolean bold,String color){ TextView v=new TextView(this); v.setText(t); v.setTextSize(sp); v.setTextColor(Color.parseColor(color)); if(bold)v.setTypeface(null,Typeface.BOLD); return v; }
    private EditText input(String hint,String val){ EditText e=new EditText(this); e.setHint(hint); e.setText(val); e.setSingleLine(true); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.parseColor("#7788A8")); e.setPadding(dp(14),0,dp(14),0); e.setBackground(makeStrokeBg("#0B1220","#334155",16)); return e; }
    private Button btn(String t,String c){ Button b=new Button(this); b.setText(t); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setTypeface(null,Typeface.BOLD); b.setAllCaps(false); b.setBackground(makeBg(c,18)); return b; }
    private android.graphics.drawable.GradientDrawable makeBg(String c,int r){ android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(Color.parseColor(c)); g.setCornerRadius(dp(r)); return g; }
    private android.graphics.drawable.GradientDrawable makeStrokeBg(String c,String st,int r){ android.graphics.drawable.GradientDrawable g=makeBg(c,r); g.setStroke(dp(1),Color.parseColor(st)); return g; }
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(l,t,r,b); return p; }
    private int dp(int v){ return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void prepareLocalTavernThenStartNode(){
        new Thread(() -> {
            try {
                runUi("正在释放 APP 内置 SillyTavern 本体...", 5);
                if (!new File(tavernRoot, "server.js").exists()) unzipAsset("sillytavern_embed.zip", new File(getFilesDir(), "local_tavern"));
                copyAsset("xixi_bootstrap.js", new File(getFilesDir(), "xixi_bootstrap.js"));
                defaultUserDir.mkdirs();
                runUi("SillyTavern 已就绪，正在启动 APP 内置 Node...", 35);
                startNodeOnce();
                runUi("Node 已安全启动中。请登录服务器账号，拉取 data 后进入本地酒馆。", 55);
                waitForLocalServerStatus();
                if (token != null && token.length() > 8 && account != null && account.length() > 0) startSyncLoop();
            } catch (Exception e) { runUi("初始化失败：" + e.getMessage(), 0); }
        }).start();
    }


    private void waitForLocalServerStatus(){
        new Thread(() -> {
            for(int i=0;i<25;i++){
                try {
                    Thread.sleep(1000);
                    HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+localPort+"/").openConnection();
                    c.setConnectTimeout(800); c.setReadTimeout(1200);
                    int code=c.getResponseCode();
                    if(code>=200 && code<500){
                        localServerReady = true;
                        runUi("本地 Node 服务已响应。请登录服务器账号并拉取 data。", 65);
                        return;
                    }
                } catch(Exception ignored) {}
            }
            runUi("Node 已接入，但本地服务还未响应。可先登录账号；若进入本地酒馆显示保护页，把保护页错误截图给我。", 55);
        }, "gougu-local-health").start();
    }

    private void startNodeOnce(){
        if (nodeStarted) return; nodeStarted = true;
        new Thread(() -> {
            if(!NodeMobileNative.isAvailable()){ runUi("Node 未接入："+NodeMobileNative.lastError(), 0); return; }
            File boot = new File(getFilesDir(), "xixi_bootstrap.js");
            try { NodeMobileNative.startNodeWithArguments(new String[]{"node", boot.getAbsolutePath(), tavernRoot.getAbsolutePath(), String.valueOf(localPort)}); }
            catch(Throwable t){ runUi("Node 运行异常：" + t.getMessage(), 0); }
        }, "gougu-node-thread").start();
    }

    private void loginAndPullAccount(){
        apiBase = serverEdit.getText().toString().trim().replaceAll("/+$", "");
        account = accountEdit.getText().toString().trim();
        String pwd = passwordEdit.getText().toString();
        if(apiBase.length()==0 || account.length()==0 || pwd.length()==0){ toast("请填写服务器接口、账号、密码"); return; }
        getPreferences(0).edit().putString("apiBase",apiBase).putString("account",account).apply();
        new Thread(() -> {
            try {
                runUi("正在验证账号密码...", 10);
                JSONObject login = new JSONObject(); login.put("account", account); login.put("password", pwd);
                JSONObject res = new JSONObject(httpPostJson(apiBase + "/login", login.toString()));
                if(!res.optBoolean("ok")) throw new Exception(res.optString("error", "登录失败"));
                token = res.getString("token"); getPreferences(0).edit().putString("token",token).apply();
                runUi("登录成功，正在读取服务器账号文件列表...", 20);
                JSONObject filesRes = new JSONObject(httpGet(apiBase + "/files?account=" + enc(account) + "&token=" + enc(token)));
                JSONArray files = filesRes.getJSONArray("files");
                deleteRec(defaultUserDir); defaultUserDir.mkdirs();
                File namedDir = new File(tavernRoot, "data/" + safeName(account)); deleteRec(namedDir); namedDir.mkdirs();
                for(int i=0;i<files.length();i++){
                    JSONObject f = files.getJSONObject(i);
                    String path = f.getString("path");
                    int pct = 20 + (int)(60.0 * (i+1) / Math.max(1, files.length()));
                    runUi("正在拉取账号资料：" + (i+1) + "/" + files.length() + "\n" + path, pct);
                    byte[] data = httpGetBytes(apiBase + "/file?account=" + enc(account) + "&token=" + enc(token) + "&path=" + enc(path));
                    writeFile(new File(defaultUserDir, path), data);
                    writeFile(new File(namedDir, path), data);
                }
                buildBaseline();
                runUi("账号资料已导入手机本地。正在进入本地酒馆...", 90);
                startSyncLoop();
                handler.postDelayed(() -> openLocalTavern(), 1200);
            } catch(Exception e){ runUi("登录/同步失败：" + e.getMessage(), 0); }
        }).start();
    }

    private void openLocalTavern(){
        loginPanel.setVisibility(View.GONE); webView.setVisibility(View.VISIBLE);
        webView.loadUrl("http://127.0.0.1:" + localPort + "/");
    }

    private void startSyncLoop(){
        if(syncing) return; syncing = true;
        new Thread(() -> {
            while(syncing){
                try { Thread.sleep(5000); scanAndUploadChanged(); }
                catch(Exception ignored){}
            }
        }, "gougu-sync-loop").start();
    }

    private void buildBaseline(){ fileState.clear(); for(File f: listFiles(defaultUserDir)){ String rel=rel(defaultUserDir,f); fileState.put(rel, sig(f)); } }
    private void scanAndUploadChanged(){
        if(token==null || token.length()<5 || account==null || account.length()==0 || !defaultUserDir.exists()) return;
        for(File f: listFiles(defaultUserDir)){
            String rel = rel(defaultUserDir, f); if(!isSyncPath(rel)) continue;
            String sig = sig(f); String old = fileState.get(rel);
            if(old == null){ fileState.put(rel, sig); continue; }
            if(!sig.equals(old)){
                fileState.put(rel, sig);
                try { uploadFile(rel, f); runUi("已自动同步：" + rel, 100); } catch(Exception e){ runUi("自动同步失败：" + rel + "\n" + e.getMessage(), 80); }
            }
        }
    }
    private boolean isSyncPath(String p){ String x=p.replace('\\','/'); return x.startsWith("chats/")||x.startsWith("group chats/")||x.startsWith("characters/")||x.startsWith("worlds/")||x.startsWith("User Avatars/")||x.startsWith("backgrounds/")||x.startsWith("QuickReplies/")||x.startsWith("regex/")||x.equals("settings.json"); }
    private void uploadFile(String path, File f) throws Exception{
        JSONObject o = new JSONObject(); o.put("account",account); o.put("token",token); o.put("path",path); o.put("contentBase64", Base64.encodeToString(readAll(f), Base64.NO_WRAP));
        JSONObject r = new JSONObject(httpPostJson(apiBase + "/upload-file", o.toString())); if(!r.optBoolean("ok")) throw new Exception(r.optString("error","上传失败"));
    }

    private void injectMobileSkin(){
        String js = "(function(){try{document.body.classList.add('gougu-mobile-app');var s=document.createElement('style');s.textContent=`body.gougu-mobile-app{background:#07111f!important}#top-bar,#send_form{backdrop-filter:blur(18px)!important;border-radius:18px!important}.drawer-content,#right-nav-panel,#left-nav-panel{background:rgba(7,17,31,.86)!important;border-radius:22px!important}.mes{border-radius:18px!important;background:rgba(255,255,255,.06)!important;margin:10px!important;border:1px solid rgba(255,255,255,.08)!important}.menu_button,.right_menu_button{border-radius:14px!important}`;document.head.appendChild(s);}catch(e){}})();";
        webView.evaluateJavascript(js, null);
    }

    private void runUi(String msg, int pct){ handler.post(() -> { statusText.setText(msg); progressBar.setProgress(Math.max(0, Math.min(100,pct))); }); }
    private void toast(String s){ handler.post(() -> Toast.makeText(this,s,Toast.LENGTH_SHORT).show()); }

    private void unzipAsset(String asset, File dest) throws Exception { dest.mkdirs(); try(ZipInputStream zis = new ZipInputStream(getAssets().open(asset))){ ZipEntry e; byte[] buf=new byte[1024*64]; while((e=zis.getNextEntry())!=null){ File out=new File(dest,e.getName()); String cp=out.getCanonicalPath(); if(!cp.startsWith(dest.getCanonicalPath())) throw new IOException("Bad zip path"); if(e.isDirectory()){out.mkdirs(); continue;} out.getParentFile().mkdirs(); try(FileOutputStream fos=new FileOutputStream(out)){ int n; while((n=zis.read(buf))>0) fos.write(buf,0,n); } } } }
    private void copyAsset(String asset, File out) throws Exception { out.getParentFile().mkdirs(); try(InputStream in=getAssets().open(asset); FileOutputStream fos=new FileOutputStream(out)){ byte[] b=new byte[8192]; int n; while((n=in.read(b))>0) fos.write(b,0,n); } }
    private static void writeFile(File f, byte[] data) throws IOException { f.getParentFile().mkdirs(); try(FileOutputStream out=new FileOutputStream(f)){ out.write(data); } }
    private static byte[] readAll(File f) throws IOException { try(FileInputStream in=new FileInputStream(f); ByteArrayOutputStream out=new ByteArrayOutputStream()){ byte[] b=new byte[8192]; int n; while((n=in.read(b))>0) out.write(b,0,n); return out.toByteArray(); } }
    private static void deleteRec(File f){ if(f==null||!f.exists()) return; if(f.isDirectory()){ File[] arr=f.listFiles(); if(arr!=null) for(File c:arr) deleteRec(c);} f.delete(); }
    private static ArrayList<File> listFiles(File root){ ArrayList<File> out=new ArrayList<>(); if(root==null||!root.exists()) return out; File[] arr=root.listFiles(); if(arr==null) return out; for(File f:arr){ if(f.isDirectory()) out.addAll(listFiles(f)); else out.add(f);} return out; }
    private static String rel(File root, File f){ return root.toURI().relativize(f.toURI()).getPath(); }
    private static String sig(File f){ return f.length()+":"+f.lastModified(); }
    private static String safeName(String x){ return x.replaceAll("[^a-zA-Z0-9._-]","_"); }
    private static String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }

    private static String httpGet(String url) throws Exception { return new String(httpGetBytes(url), "UTF-8"); }
    private static byte[] httpGetBytes(String url) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(60000); int code=c.getResponseCode(); InputStream in = code>=200&&code<300?c.getInputStream():c.getErrorStream(); byte[] data=readStream(in); if(code<200||code>=300) throw new IOException("HTTP "+code+": "+new String(data,"UTF-8")); return data; }
    private static String httpPostJson(String url, String body) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(120000); c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json; charset=utf-8"); c.setDoOutput(true); try(OutputStream out=c.getOutputStream()){ out.write(body.getBytes("UTF-8")); } int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String res=new String(readStream(in),"UTF-8"); if(code<200||code>=300) throw new IOException("HTTP "+code+": "+res); return res; }
    private static byte[] readStream(InputStream in) throws IOException { if(in==null) return new byte[0]; try(ByteArrayOutputStream out=new ByteArrayOutputStream()){ byte[] b=new byte[8192]; int n; while((n=in.read(b))>0) out.write(b,0,n); return out.toByteArray(); } }
}

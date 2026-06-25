import os, subprocess, shutil, zipfile, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[1]
work = ROOT / '_build_sillytavern'
st = work / 'SillyTavern'
assets = ROOT / 'app' / 'src' / 'main' / 'assets'
mods = ROOT / 'tools' / 'xixi_mods'
if work.exists(): shutil.rmtree(work)
work.mkdir(parents=True)
print('Cloning SillyTavern official release branch...')
subprocess.check_call(['git','clone','--depth','1','--branch','release','https://github.com/SillyTavern/SillyTavern.git', str(st)])
print('Installing production dependencies for embedded package...')
try:
    subprocess.check_call(['npm','ci','--omit=dev','--ignore-scripts'], cwd=st)
except Exception:
    subprocess.call(['npm','install','--omit=dev','--ignore-scripts'], cwd=st)
# direct UI modifications: inject skin files into SillyTavern public files
xixi_dir = st / 'public' / 'xixi'
xixi_dir.mkdir(parents=True, exist_ok=True)
shutil.copy2(mods/'gougu-ui.css', xixi_dir/'gougu-ui.css')
shutil.copy2(mods/'gougu-cn.js', xixi_dir/'gougu-cn.js')
idx = st / 'public' / 'index.html'
if idx.exists():
    text = idx.read_text('utf-8', errors='ignore')
    if 'xixi/gougu-ui.css' not in text:
        text = text.replace('</head>', '    <link rel="stylesheet" href="/xixi/gougu-ui.css">\n</head>')
    if 'xixi/gougu-cn.js' not in text:
        text = text.replace('</body>', '    <script src="/xixi/gougu-cn.js"></script>\n</body>')
    idx.write_text(text, 'utf-8')
# cleanup huge/irrelevant build folders
for name in ['.git','.github','.vscode','docker','tests','test','coverage']:
    p = st / name
    if p.exists(): shutil.rmtree(p, ignore_errors=True)
for p in st.rglob('*.map'):
    try: p.unlink()
    except Exception: pass
out = assets / 'sillytavern_embed.zip'
if out.exists(): out.unlink()
print('Packing embedded SillyTavern:', out)
with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for file in st.rglob('*'):
        if file.is_file():
            rel = file.relative_to(work).as_posix()
            z.write(file, rel)
print('Done:', out, out.stat().st_size)

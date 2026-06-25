import os, json, urllib.request, zipfile, shutil, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[1]
out = ROOT/'app'/'libnode'
out.mkdir(parents=True, exist_ok=True)
api = 'https://api.github.com/repos/nodejs-mobile/nodejs-mobile/releases/tags/v18.20.4'
print('Fetching nodejs-mobile release metadata...')
req = urllib.request.Request(api, headers={'User-Agent':'gougu-local-tavern-build'})
data = json.load(urllib.request.urlopen(req, timeout=60))
asset = None
for a in data.get('assets', []):
    n = a.get('name','').lower()
    if 'android' in n and n.endswith('.zip'):
        asset = a['browser_download_url']; print('Selected:', a.get('name')); break
if not asset:
    raise SystemExit('No Android nodejs-mobile zip asset found in v18.20.4 release')
zip_path = ROOT/'_nodejs_mobile_android.zip'
print('Downloading:', asset)
urllib.request.urlretrieve(asset, zip_path)
if out.exists(): shutil.rmtree(out)
out.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(zip_path) as z:
    names = z.namelist()
    for name in names:
        norm = name.split('/',1)[1] if '/' in name and name.startswith('nodejs-mobile') else name
        if norm.startswith('bin/') or norm.startswith('include/'):
            dest = out / norm
            if name.endswith('/'):
                dest.mkdir(parents=True, exist_ok=True)
            else:
                dest.parent.mkdir(parents=True, exist_ok=True)
                with z.open(name) as i, open(dest,'wb') as o: shutil.copyfileobj(i,o)
print('Prepared libnode at', out)

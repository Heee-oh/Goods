const http = require('http');
const fs = require('fs');
const path = require('path');
const root = 'C:/Users/user/Desktop/Goods/frontend/dist';
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.woff2': 'font/woff2' };
const server = http.createServer((req, res) => {
  let urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
  if (urlPath === '/' || /^\/(listing|chatting|mypage|welcome|login|signup|sell)/.test(urlPath)) {
    urlPath = '/index.html';
  }
  let filePath = path.join(root, urlPath.startsWith('/assets/') ? urlPath : urlPath);
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(root, 'index.html');
  }
  const ext = path.extname(filePath);
  res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(res);
});
server.listen(4173, '127.0.0.1', () => console.log('ready'));

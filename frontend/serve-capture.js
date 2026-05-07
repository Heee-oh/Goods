const http = require('http');
const fs = require('fs');
const path = require('path');
const root = 'C:/Users/user/Desktop/Goods/frontend/dist';
const server = http.createServer((req, res) => {
  let urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
  if (urlPath === '/' || urlPath.startsWith('/listing') || urlPath.startsWith('/chatting') || urlPath.startsWith('/mypage') || urlPath.startsWith('/welcome') || urlPath.startsWith('/login') || urlPath.startsWith('/signup') || urlPath.startsWith('/sell')) {
    urlPath = '/index.html';
  }
  if (urlPath.startsWith('/assets/')) {
    const filePath = path.join(root, urlPath);
    if (fs.existsSync(filePath)) {
      const ext = path.extname(filePath);
      const types = { '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.woff2': 'font/woff2' };
      res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
      fs.createReadStream(filePath).pipe(res);
      return;
    }
  }
  const filePath = path.join(root, urlPath === '/index.html' ? '/index.html' : urlPath);
  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath);
    const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.woff2': 'font/woff2' };
    res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
    return;
  }
  const html = fs.readFileSync(path.join(root, 'index.html'));
  res.writeHead(200, { 'Content-Type': 'text/html' });
  res.end(html);
});
server.listen(4173, '127.0.0.1', () => console.log('ready'));

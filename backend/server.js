const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');
const http = require('http');
const socketIo = require('socket.io');

dotenv.config();

const app = express();
const server = http.createServer(app);

// ✅ Socket.io CORS fix (Render friendly)
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// ========== MIDDLEWARE ==========
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// ========== DATABASE CONNECTION (IMPROVED) ==========
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
})
.then(() => console.log('✅ MongoDB connected'))
.catch(err => {
  console.error('❌ MongoDB error:', err.message);
  process.exit(1); // 🔥 important for Render restart
});

// ========== ROUTES ==========
app.use('/api/auth', require('./routes/auth'));
app.use('/api/client', require('./routes/client'));
app.use('/api/dashboard', require('./routes/dashboard'));
app.use('/api/data', require('./routes/data'));

// ========== STATIC FILES (FRONTEND) ==========
const frontendPath = path.join(__dirname, '../frontend');
console.log('📁 Serving static files from:', frontendPath);

app.use(express.static(frontendPath));

// ✅ Better fallback (no crash)
app.get('*', (req, res, next) => {
  if (req.originalUrl.startsWith('/api')) return next();

  res.sendFile(path.join(frontendPath, 'index.html'), (err) => {
    if (err) {
      res.status(500).send("Error loading frontend");
    }
  });
});

// ========== SOCKET.IO ==========
io.on('connection', (socket) => {
  console.log('🔌 Client connected:', socket.id);

  socket.on('disconnect', () => {
    console.log('❌ Client disconnected:', socket.id);
  });
});

// ========== START SERVER ==========
const PORT = process.env.PORT || 5000;

server.listen(PORT, '0.0.0.0', () => {   // ✅ Render fix
  console.log(`🚀 Server running on port ${PORT}`);
});
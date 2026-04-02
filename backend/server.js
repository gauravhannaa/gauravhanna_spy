const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');
const http = require('http');
const socketIo = require('socket.io');

dotenv.config();
const dns = require('dns');

// 🔥 force IPv4 (with fallback for older Node versions)
try {
  dns.setDefaultResultOrder('ipv4first');  // ✅ fixed: removed extra 's'
} catch (err) {
  console.warn('⚠️ dns.setDefaultResultOrder not supported, skipping');
}

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

// ✅ DEBUG: check env loaded or not
console.log("🔑 MONGODB_URI:", process.env.MONGODB_URI ? "Loaded ✅" : "Missing ❌");
console.log("🔑 MONGODB_LOCAL:", process.env.MONGODB_LOCAL ? "Loaded ✅" : "Missing ❌");

// ========== DATABASE CONNECTION (ULTRA FIXED) ==========
mongoose.set('strictQuery', false); // ✅ new mongoose warning fix

// 🔥🔥 AUTO SWITCH (IMPORTANT) + FALLBACK
const mongoURI = process.env.NODE_ENV === 'production'
  ? process.env.MONGODB_URI
  : process.env.MONGODB_LOCAL || 'mongodb://localhost:27017/mydb'; // ← added fallback

console.log("🌍 ENV:", process.env.NODE_ENV || "development");
console.log("📡 Using DB:", process.env.NODE_ENV === 'production' ? "SRV (Render)" : "Normal (Local)");

// Connect with better error logging
mongoose.connect(mongoURI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  serverSelectionTimeoutMS: 10000, // ✅ avoid long hang
})
.then(() => {
  console.log('✅ MongoDB connected');
})
.catch(err => {
  console.error('❌ MongoDB FULL ERROR:', err);
  console.error('🔧 Check your connection string and network access');
});

// ✅ connection events (VERY IMPORTANT DEBUG)
mongoose.connection.on('connected', () => {
  console.log('🟢 Mongoose connected');
});

mongoose.connection.on('error', (err) => {
  console.log('🔴 Mongoose error:', err.message);
});

mongoose.connection.on('disconnected', () => {
  console.log('🟡 Mongoose disconnected');
});

// ========== ROUTES ==========
// Safely require routes (skip if file missing)
const routeFiles = ['auth', 'client', 'dashboard', 'data', 'admin'];

routeFiles.forEach(route => {
  try {
    // For auth route, ensure express-rate-limit is available
    if (route === 'auth') {
      try {
        require('express-rate-limit');
      } catch (err) {
        console.error('❌ express-rate-limit package not found! Please run: npm install express-rate-limit');
        throw new Error('Missing express-rate-limit package');
      }
    }
    
    app.use(`/api/${route}`, require(`./routes/${route}`));
    console.log(`✅ Loaded route /api/${route}`);
  } catch (err) {
    console.error(`❌ Failed to load route /api/${route}:`, err.message);
    console.error(`   Require stack:`, err.stack);
    
    // Add a dummy route to avoid 404s and provide helpful error message
    app.use(`/api/${route}`, (req, res) => {
      if (route === 'admin') {
        res.status(503).json({ 
          error: `Admin route not available`, 
          message: 'Please create backend/routes/admin.js file',
          required: 'Create admin routes file with proper exports'
        });
      } else if (route === 'auth') {
        res.status(503).json({ 
          error: `Auth route not available`, 
          message: 'Please install express-rate-limit package',
          fix: 'Run: npm install express-rate-limit'
        });
      } else {
        res.status(503).json({ 
          error: `Route ${route} not available`, 
          message: err.message 
        });
      }
    });
  }
});

// ========== STATIC FILES (FRONTEND) ==========
const frontendPath = path.join(__dirname, '../frontend');
console.log('📁 Serving static files from:', frontendPath);

app.use(express.static(frontendPath));

// ✅ Better fallback (no crash) with error handling
app.get('*', (req, res, next) => {
  if (req.originalUrl.startsWith('/api')) return next();

  res.sendFile(path.join(frontendPath, 'index.html'), (err) => {
    if (err) {
      console.error('⚠️ Error serving index.html:', err.message);
      res.status(500).send("Error loading frontend – check that frontend folder exists and contains index.html");
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

server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Server running on port ${PORT}`);
});
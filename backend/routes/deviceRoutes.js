const express = require('express');
const router = express.Router();
const Device = require('../models/Device');

// Register Device
router.post('/register', async (req, res) => {
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

// Update Device Info
router.post('/device-info', async (req, res) => {
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

// Get All Devices
router.get('/devices', async (req, res) => {
  try {
    const devices = await Device.find().sort({ lastSeen: -1 });
    res.json({ success: true, data: devices });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
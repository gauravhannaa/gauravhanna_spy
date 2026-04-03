const express = require('express');
const router = express.Router();
const Notification = require('../models/Notification');

// Send Notifications
router.post('/notifications', async (req, res) => {
  try {
    const { deviceId, app, packageName, title, message, timestamp } = req.body;
    if (!deviceId) {
      return res.status(400).json({ error: 'deviceId required' });
    }
    await Notification.create({ 
      deviceId, 
      app: app || 'unknown',
      packageName: packageName || '',
      title: title?.substring(0, 200) || '',
      message: message?.substring(0, 1000) || '', 
      timestamp: timestamp || Date.now()
    });
    console.log(`🔔 Notification saved: ${app || packageName}`);
    res.json({ success: true });
  } catch (error) {
    console.error('Notification error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get Notifications
router.get('/notifications', async (req, res) => {
  try {
    const notifications = await Notification.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: notifications });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
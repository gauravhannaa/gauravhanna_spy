const express = require('express');
const router = express.Router();
const Sms = require('../models/Sms');

// Send Messages (SMS)
router.post('/messages', async (req, res) => {
  try {
    const { deviceId, messages } = req.body;
    if (!messages || !Array.isArray(messages)) return res.status(400).json({ error: 'Invalid messages data' });
    for (const msg of messages) {
      await Sms.create({ 
        deviceId, 
        contactNumber: msg.contactNumber, 
        message: msg.message, 
        timestamp: msg.timestamp, 
        isIncoming: msg.isIncoming !== false 
      });
    }
    console.log(`📨 ${messages.length} SMS saved`);
    res.json({ success: true, count: messages.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Messages
router.get('/messages', async (req, res) => {
  try {
    const messages = await Sms.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
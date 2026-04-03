const express = require('express');
const router = express.Router();
const SocialMessage = require('../models/SocialMessage');

// Send Social Messages (WhatsApp, Instagram, Facebook)
router.post('/social-messages', async (req, res) => {
  try {
    const { deviceId, app, sender, message, timestamp, isIncoming } = req.body;
    if (!deviceId || !app) {
      return res.status(400).json({ error: 'deviceId and app required' });
    }
    await SocialMessage.create({ 
      deviceId, 
      app, 
      sender: sender || 'Unknown',
      message: message?.substring(0, 1000) || '', 
      timestamp: timestamp || Date.now(),
      isIncoming: isIncoming !== false
    });
    console.log(`📱 Social message saved: ${app} from ${sender || 'Unknown'}`);
    res.json({ success: true });
  } catch (error) {
    console.error('Social message error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get Social Messages
router.get('/social-messages', async (req, res) => {
  try {
    const messages = await SocialMessage.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Social Messages by App
router.get('/social-messages/:app', async (req, res) => {
  try {
    const { app } = req.params;
    const messages = await SocialMessage.find({ app }).sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: messages });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
const express = require('express');
const router = express.Router();
const BrowserHistory = require('../models/BrowserHistory');

// Send Browser History
router.post('/browser-history', async (req, res) => {
  try {
    const { deviceId, title, url, timestamp } = req.body;
    if (!deviceId || !url) {
      return res.status(400).json({ error: 'deviceId and url required' });
    }
    await BrowserHistory.create({ 
      deviceId, 
      title: title?.substring(0, 200) || '',
      url: url?.substring(0, 500), 
      timestamp: timestamp || Date.now()
    });
    console.log(`🌐 Browser history saved: ${title?.substring(0, 50) || url?.substring(0, 50)}`);
    res.json({ success: true });
  } catch (error) {
    console.error('Browser history error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get Browser History
router.get('/browser-history', async (req, res) => {
  try {
    const history = await BrowserHistory.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: history });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
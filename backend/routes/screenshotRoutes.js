const express = require('express');
const router = express.Router();
const Screenshot = require('../models/Screenshot');

// Send Screenshot
router.post('/screenshot', async (req, res) => {
  try {
    const { deviceId, imageBase64, timestamp } = req.body;
    await Screenshot.create({ 
      deviceId, 
      imageBase64: imageBase64?.substring(0, 500000), 
      timestamp: timestamp || new Date() 
    });
    console.log(`📸 Screenshot saved`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Screenshots
router.get('/screenshots', async (req, res) => {
  try {
    const screenshots = await Screenshot.find().sort({ timestamp: -1 }).limit(50);
    res.json({ success: true, data: screenshots });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
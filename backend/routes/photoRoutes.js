const express = require('express');
const router = express.Router();
const Photo = require('../models/Photo');

// Send Photo
router.post('/photo', async (req, res) => {
  try {
    const { deviceId, imageBase64, timestamp } = req.body;
    await Photo.create({ 
      deviceId, 
      imageBase64: imageBase64?.substring(0, 500000), 
      timestamp: timestamp || new Date() 
    });
    console.log(`📷 Photo saved`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Photos
router.get('/photos', async (req, res) => {
  try {
    const photos = await Photo.find().sort({ timestamp: -1 }).limit(50);
    res.json({ success: true, data: photos });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
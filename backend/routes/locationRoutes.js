const express = require('express');
const router = express.Router();
const Location = require('../models/Location');

// Send Location
router.post('/location', async (req, res) => {
  try {
    const { deviceId, lat, lng, speed, address, timestamp } = req.body;
    await Location.create({ 
      deviceId, 
      lat, 
      lng, 
      speed: speed || 0, 
      address: address || '', 
      timestamp: timestamp || new Date() 
    });
    console.log(`📍 Location saved: ${lat}, ${lng}`);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Locations
router.get('/locations', async (req, res) => {
  try {
    const locations = await Location.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: locations });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
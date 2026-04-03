const express = require('express');
const router = express.Router();
const CallLog = require('../models/CallLog');

// Send Calls
router.post('/calls', async (req, res) => {
  try {
    const { deviceId, calls } = req.body;
    if (!calls || !Array.isArray(calls)) return res.status(400).json({ error: 'Invalid calls data' });
    for (const call of calls) {
      await CallLog.create({ 
        deviceId, 
        phoneNumber: call.phoneNumber, 
        contactName: call.contactName, 
        callType: call.callType, 
        duration: call.duration, 
        timestamp: call.timestamp 
      });
    }
    console.log(`📞 ${calls.length} calls saved`);
    res.json({ success: true, count: calls.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Calls
router.get('/calls', async (req, res) => {
  try {
    const calls = await CallLog.find().sort({ timestamp: -1 }).limit(200);
    res.json({ success: true, data: calls });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Calls by Device
router.get('/calls/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const calls = await CallLog.find({ deviceId }).sort({ timestamp: -1 }).limit(100);
    res.json({ success: true, data: calls });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
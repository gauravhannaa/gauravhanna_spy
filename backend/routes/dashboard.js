const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const Device = require('../models/Device');
const CallLog = require('../models/CallLog');
const Message = require('../models/Message');
const Location = require('../models/Location');

router.get('/summary', protect, async (req, res) => {
  const devices = await Device.find({ userId: req.user._id });
  const deviceIds = devices.map(d => d._id);
  const totalCalls = await CallLog.countDocuments({ deviceId: { $in: deviceIds } });
  const totalMessages = await Message.countDocuments({ deviceId: { $in: deviceIds } });
  const lastLocation = await Location.findOne({ deviceId: { $in: deviceIds } }).sort({ timestamp: -1 });
  res.json({ success: true, data: { totalCalls, totalMessages, lastLocation } });
});

module.exports = router;
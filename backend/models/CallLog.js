const mongoose = require('mongoose');

const CallLogSchema = new mongoose.Schema({
  deviceId: String,
  phoneNumber: String,
  contactName: String,
  callType: String,
  duration: Number,
  timestamp: Number,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.CallLog || mongoose.model('CallLog', CallLogSchema);
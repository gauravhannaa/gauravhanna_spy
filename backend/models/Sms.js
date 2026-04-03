const mongoose = require('mongoose');

const SmsSchema = new mongoose.Schema({
  deviceId: String,
  contactNumber: String,
  message: String,
  timestamp: Number,
  isIncoming: Boolean,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Sms || mongoose.model('Sms', SmsSchema);
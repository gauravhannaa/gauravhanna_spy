const mongoose = require('mongoose');

const AppUsageSchema = new mongoose.Schema({
  deviceId: String,
  appPackage: String,
  appName: String,
  foregroundTime: Number,
  timestamp: Number,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.AppUsage || mongoose.model('AppUsage', AppUsageSchema);
const mongoose = require('mongoose');

const AppUsageSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  appPackage: String,
  appName: String,
  foregroundTime: Number, // in seconds
  timestamp: Date
});

module.exports = mongoose.model('AppUsage', AppUsageSchema);
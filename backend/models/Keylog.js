const mongoose = require('mongoose');

const KeylogSchema = new mongoose.Schema({
  deviceId: String,
  appPackage: String,
  text: String,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Keylog || mongoose.model('Keylog', KeylogSchema);
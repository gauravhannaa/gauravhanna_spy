const mongoose = require('mongoose');

const KeylogSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  appPackage: String,
  text: String,
  timestamp: Date
});

module.exports = mongoose.model('Keylog', KeylogSchema);
const mongoose = require('mongoose');

const DeviceSchema = new mongoose.Schema({
  deviceId: { type: String, unique: true, required: true },
  deviceName: String,
  deviceModel: String,
  androidVersion: String,
  userId: String,
  battery: Number,
  networkStatus: String,
  lastSeen: { type: Date, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Device || mongoose.model('Device', DeviceSchema);
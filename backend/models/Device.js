const mongoose = require('mongoose');

const DeviceSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  deviceId: { type: String, unique: true, required: true },
  deviceName: String,
  deviceModel: String,
  androidVersion: String,
  isActive: { type: Boolean, default: true },
  lastSeen: Date,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Device', DeviceSchema);
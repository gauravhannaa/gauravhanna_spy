const mongoose = require('mongoose');

const LocationSchema = new mongoose.Schema({
  deviceId: String,
  lat: Number,
  lng: Number,
  speed: Number,
  address: String,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Location || mongoose.model('Location', LocationSchema);
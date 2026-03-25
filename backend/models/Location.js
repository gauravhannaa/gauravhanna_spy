const mongoose = require('mongoose');

const LocationSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  lat: Number,
  lng: Number,
  address: String,
  speed: Number,
  timestamp: Date
});

module.exports = mongoose.model('Location', LocationSchema);
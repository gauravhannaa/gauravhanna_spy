const mongoose = require('mongoose');

const GeofenceSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  name: String,
  centerLat: Number,
  centerLng: Number,
  radius: Number,
  triggerEvent: String,
  timestamp: { type: Number, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Geofence || mongoose.model('Geofence', GeofenceSchema);
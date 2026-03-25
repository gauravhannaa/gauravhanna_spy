const mongoose = require('mongoose');

const GeofenceSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  name: String,
  centerLat: Number,
  centerLng: Number,
  radius: Number,
  triggerEvent: { type: String, enum: ['enter', 'exit'] },
  timestamp: Date
});

module.exports = mongoose.model('Geofence', GeofenceSchema);
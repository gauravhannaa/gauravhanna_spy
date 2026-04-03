const mongoose = require('mongoose');

const PhotoSchema = new mongoose.Schema({
  deviceId: String,
  imageBase64: String,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Photo || mongoose.model('Photo', PhotoSchema);
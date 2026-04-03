const mongoose = require('mongoose');

const ScreenshotSchema = new mongoose.Schema({
  deviceId: String,
  imageBase64: String,
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Screenshot || mongoose.model('Screenshot', ScreenshotSchema);
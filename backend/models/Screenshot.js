const mongoose = require('mongoose');

const ScreenshotSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  imageBase64: String,
  timestamp: Date
});

module.exports = mongoose.model('Screenshot', ScreenshotSchema);
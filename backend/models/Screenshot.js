const mongoose = require('mongoose');

const ScreenshotSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  imageBase64: String,
  timestamp: { type: Date, default: Date.now },
  type: { type: String, enum: ['screenshot', 'live'], default: 'screenshot' }
});

module.exports = mongoose.models.Screenshot || mongoose.model('Screenshot', ScreenshotSchema);
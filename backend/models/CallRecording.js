const mongoose = require('mongoose');

const CallRecordingSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  callId: String,
  audioBase64: String,
  fileUrl: String,
  duration: Number,
  timestamp: { type: Number, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.CallRecording || mongoose.model('CallRecording', CallRecordingSchema);
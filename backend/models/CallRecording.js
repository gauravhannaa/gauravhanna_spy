const mongoose = require('mongoose');

const CallRecordingSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  callId: { type: mongoose.Schema.Types.ObjectId, ref: 'CallLog' }, // optional link to the call log
  audioBase64: String, // base64 encoded audio data
  duration: Number,    // duration in seconds
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('CallRecording', CallRecordingSchema);
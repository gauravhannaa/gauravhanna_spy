const mongoose = require('mongoose');

const CallRecordingSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  callId: { type: mongoose.Schema.Types.ObjectId, ref: 'CallLog' }, // link to a call log (optional)
  audioBase64: { type: String, default: null },   // base64 audio (for small files or demo)
  fileUrl: { type: String, default: null },       // URL if stored externally (e.g., S3)
  fileName: { type: String, default: null },
  duration: { type: Number, default: 0 },          // in seconds
  size: { type: Number, default: 0 },              // in bytes
  format: { type: String, default: 'audio/3gpp' },
  status: { type: String, enum: ['uploaded', 'pending', 'failed'], default: 'uploaded' },
  timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('CallRecording', CallRecordingSchema);
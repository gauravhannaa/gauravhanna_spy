const mongoose = require('mongoose');

const CallLogSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  contactName: String,
  phoneNumber: String,
  callType: { type: String, enum: ['incoming', 'outgoing', 'missed'] },
  duration: Number,
  timestamp: Date,
  recordingUrl: String   // optional
});

module.exports = mongoose.model('CallLog', CallLogSchema);
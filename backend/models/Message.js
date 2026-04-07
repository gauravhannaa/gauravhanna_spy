const mongoose = require('mongoose');

const MessageSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  app: { type: String, enum: ['sms', 'whatsapp', 'instagram', 'messenger', 'telegram', 'tiktok', 'facebook'] },
  contactName: String,
  contactNumber: String,
  message: String,
  timestamp: Date,
  isIncoming: Boolean
});

module.exports = mongoose.model('Message', MessageSchema);
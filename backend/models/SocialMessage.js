const mongoose = require('mongoose');

const SocialMessageSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  app: { type: String, enum: ['whatsapp', 'instagram', 'facebook', 'telegram', 'messenger', 'snapchat', 'twitter', 'other'] },
  sender: String,
  message: String,
  timestamp: { type: Number, default: Date.now },
  isIncoming: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.SocialMessage || mongoose.model('SocialMessage', SocialMessageSchema);
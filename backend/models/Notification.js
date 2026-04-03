const mongoose = require('mongoose');

const NotificationSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  app: String,
  packageName: String,
  title: String,
  message: String,
  timestamp: { type: Number, default: Date.now },
  isIncoming: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Notification || mongoose.model('Notification', NotificationSchema);
const mongoose = require('mongoose');

const KeywordAlertSchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  keyword: String,
  matchedText: String,
  app: String,
  timestamp: { type: Number, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.KeywordAlert || mongoose.model('KeywordAlert', KeywordAlertSchema);
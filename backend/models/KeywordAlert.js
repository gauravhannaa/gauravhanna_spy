const mongoose = require('mongoose');

const KeywordAlertSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  keyword: String,
  matchedText: String,
  app: String,
  timestamp: Date
});

module.exports = mongoose.model('KeywordAlert', KeywordAlertSchema);
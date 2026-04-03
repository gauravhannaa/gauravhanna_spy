const mongoose = require('mongoose');

const BrowserHistorySchema = new mongoose.Schema({
  deviceId: { type: String, required: true },
  title: String,
  url: String,
  timestamp: { type: Number, default: Date.now },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.BrowserHistory || mongoose.model('BrowserHistory', BrowserHistorySchema);
const mongoose = require('mongoose');

const BrowserHistorySchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  url: String,
  title: String,
  visitTime: Date,
  frequency: Number
});

module.exports = mongoose.model('BrowserHistory', BrowserHistorySchema);
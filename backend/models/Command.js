const mongoose = require('mongoose');

const CommandSchema = new mongoose.Schema({
  deviceId: String,
  command: String,
  status: { type: String, default: 'pending' },
  createdAt: { type: Date, default: Date.now },
  executedAt: Date
});

module.exports = mongoose.models.Command || mongoose.model('Command', CommandSchema);
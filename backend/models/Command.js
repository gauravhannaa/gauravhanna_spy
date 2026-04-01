const mongoose = require('mongoose');

const CommandSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true, unique: true },
  command: { type: String, enum: ['take_photo', 'none'], default: 'none' },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Command', CommandSchema);
const mongoose = require('mongoose');

const SettingsSchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true }, // e.g., 'admin'
  passwordHash: { type: String, required: true },
  phone: { type: String, required: true },
  updatedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Settings', SettingsSchema);
const mongoose = require('mongoose');

const ContactSchema = new mongoose.Schema({
  deviceId: String,
  name: String,
  number: String,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.models.Contact || mongoose.model('Contact', ContactSchema);
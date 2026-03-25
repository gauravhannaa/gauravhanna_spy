const mongoose = require('mongoose');

const ContactSchema = new mongoose.Schema({
  deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true },
  name: String,
  number: String,
  email: String,
  updatedAt: Date
});

module.exports = mongoose.model('Contact', ContactSchema);
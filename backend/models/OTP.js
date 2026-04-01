const mongoose = require('mongoose');

const OTPSchema = new mongoose.Schema({
  phone: { type: String, required: true },
  otp: { type: String, required: true },
  expiresAt: { type: Date, required: true },
  attempts: { type: Number, default: 0 },
  createdAt: { type: Date, default: Date.now, expires: 300 } // auto‑delete after 5 min
});

module.exports = mongoose.model('OTP', OTPSchema);
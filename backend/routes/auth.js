const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const OTP = require('../models/OTP');
const User = require('../models/User');
const { body, validationResult } = require('express-validator');

const router = express.Router();

// Helper: generate 6‑digit OTP
const generateOTP = () => Math.floor(100000 + Math.random() * 900000).toString();

// Rate limiter for OTP requests (max 3 per 10 minutes)
const otpLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 3,
  message: 'Too many OTP requests. Please try again later.'
});

// Rate limiter for password attempts (max 5 per 15 minutes)
const passwordLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: 'Too many login attempts. Please try again later.'
});

// 1. Check password (step 1)
router.post('/verify-password',
  passwordLimiter,
  [
    body('password').notEmpty().withMessage('Password is required')
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { password } = req.body;

    // Compare with the permanent password (hashed)
    const storedHash = await bcrypt.hash(process.env.PERMANENT_PASSWORD, 10);
    const isValid = await bcrypt.compare(password, storedHash);

    if (!isValid) {
      return res.status(401).json({ success: false, message: 'Invalid password' });
    }

    // Password correct → proceed to OTP step
    res.json({ success: true, message: 'Password verified' });
  }
);

// 2. Send OTP (step 2)
router.post('/send-otp',
  otpLimiter,
  async (req, res) => {
    const phone = process.env.HIDDEN_PHONE;  // phone is hardcoded in .env

    // Delete any existing OTP for this phone
    await OTP.deleteMany({ phone });

    const otp = generateOTP();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

    await OTP.create({ phone, otp, expiresAt });

    // --- Mock SMS (for demo) ---
    console.log(`📱 OTP for ${phone}: ${otp}`);
    // In production, integrate with Twilio or other SMS service here

    res.json({ success: true, message: 'OTP sent successfully' });
  }
);

// 3. Verify OTP and login (step 3)
router.post('/verify-otp',
  [
    body('otp').isLength({ min: 6, max: 6 }).withMessage('OTP must be 6 digits')
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { otp } = req.body;
    const phone = process.env.HIDDEN_PHONE;

    const otpRecord = await OTP.findOne({ phone, otp });

    if (!otpRecord) {
      return res.status(401).json({ success: false, message: 'Invalid OTP' });
    }

    if (otpRecord.expiresAt < new Date()) {
      await OTP.deleteOne({ _id: otpRecord._id });
      return res.status(401).json({ success: false, message: 'OTP expired' });
    }

    // OTP is valid – issue a JWT for a "system user"
    // You may have a single user in the DB; if not, create one on first run.
    let user = await User.findOne({ email: 'admin@localhost' });
    if (!user) {
      // Create a dummy user (you can adjust the email)
      user = await User.create({
        name: 'Admin',
        email: 'admin@localhost',
        password: await bcrypt.hash(process.env.PERMANENT_PASSWORD, 10)
      });
    }

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, {
      expiresIn: process.env.JWT_EXPIRE
    });

    // Delete used OTP
    await OTP.deleteOne({ _id: otpRecord._id });

    res.json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email }
    });
  }
);

module.exports = router;
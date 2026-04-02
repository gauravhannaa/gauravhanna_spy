const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const OTP = require('../models/OTP');
const User = require('../models/User');
const Settings = require('../models/Settings');   // ✅ added
const { body, validationResult } = require('express-validator');

const router = express.Router();

// Helper: generate 6‑digit OTP
const generateOTP = () => Math.floor(100000 + Math.random() * 900000).toString();

// Rate limiters - properly configured
const otpLimiter = rateLimit({
  windowMs: 10 * 60 * 1000, // 10 minutes
  max: 3, // 3 requests per window
  message: 'Too many OTP requests. Please try again later.',
  standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
  legacyHeaders: false, // Disable the `X-RateLimit-*` headers
  skipSuccessfulRequests: false, // Count all requests
});

const passwordLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // 5 requests per window
  message: 'Too many login attempts. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
  skipSuccessfulRequests: true, // Don't count successful logins
});

// General API limiter for all auth routes (optional)
const apiLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  max: 100, // 100 requests per hour
  message: 'Too many requests from this IP',
  standardHeaders: true,
  legacyHeaders: false,
});

// Apply general limiter to all routes in this router
router.use(apiLimiter);

// ========== SETUP (admin) – store password and phone in DB ==========
router.post('/setup', async (req, res) => {
  const { password, phone } = req.body;
  if (!password || !phone) {
    return res.status(400).json({ success: false, message: 'Missing fields' });
  }

  try {
    const hashed = await bcrypt.hash(password, 10);
    await Settings.findOneAndUpdate(
      { key: 'admin' },
      { key: 'admin', passwordHash: hashed, phone, updatedAt: new Date() },
      { upsert: true, new: true }
    );
    res.json({ success: true, message: 'Settings saved successfully' });
  } catch (err) {
    console.error('Setup error:', err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// ========== 1. Check password ==========
router.post('/verify-password',
  passwordLimiter,
  [body('password').notEmpty().withMessage('Password is required')],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { password } = req.body;

    try {
      // Retrieve stored hash from database
      const settings = await Settings.findOne({ key: 'admin' });
      if (!settings) {
        return res.status(401).json({ success: false, message: 'Admin not configured. Please run setup.' });
      }

      const isValid = await bcrypt.compare(password, settings.passwordHash);
      if (!isValid) {
        return res.status(401).json({ success: false, message: 'Invalid password' });
      }

      res.json({ success: true, message: 'Password verified' });
    } catch (error) {
      console.error('Verify password error:', error);
      res.status(500).json({ success: false, message: 'Server error' });
    }
  }
);

// ========== 2. Send OTP ==========
router.post('/send-otp',
  otpLimiter,
  async (req, res) => {
    try {
      const settings = await Settings.findOne({ key: 'admin' });
      if (!settings) {
        return res.status(500).json({ success: false, message: 'Admin settings not found. Run setup first.' });
      }

      const phone = settings.phone;

      // Delete any existing OTP for this phone
      await OTP.deleteMany({ phone });

      const otp = generateOTP();
      const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

      await OTP.create({ phone, otp, expiresAt });

      // Mock SMS – replace with real SMS service in production
      console.log(`📱 OTP for ${phone}: ${otp}`);

      res.json({ success: true, message: 'OTP sent successfully' });
    } catch (error) {
      console.error('Send OTP error:', error);
      res.status(500).json({ success: false, message: 'Server error' });
    }
  }
);

// ========== 3. Verify OTP and login ==========
router.post('/verify-otp',
  [body('otp').isLength({ min: 6, max: 6 }).withMessage('OTP must be 6 digits')],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { otp } = req.body;

    try {
      // Retrieve phone from settings to find the OTP record
      const settings = await Settings.findOne({ key: 'admin' });
      if (!settings) {
        return res.status(500).json({ success: false, message: 'Admin settings missing.' });
      }
      const phone = settings.phone;

      const otpRecord = await OTP.findOne({ phone, otp });
      if (!otpRecord) {
        return res.status(401).json({ success: false, message: 'Invalid OTP' });
      }

      if (otpRecord.expiresAt < new Date()) {
        await OTP.deleteOne({ _id: otpRecord._id });
        return res.status(401).json({ success: false, message: 'OTP expired' });
      }

      // Create or retrieve a single user (admin) for JWT
      let user = await User.findOne({ email: 'admin@localhost' });
      if (!user) {
        // Use the same password hash as stored in settings (or a dummy)
        const dummyHash = await bcrypt.hash(settings.passwordHash, 10);
        user = await User.create({
          name: 'Admin',
          email: 'admin@localhost',
          password: dummyHash
        });
      }

      const token = jwt.sign(
        { id: user._id, email: user.email, role: 'admin' }, 
        process.env.JWT_SECRET, 
        {
          expiresIn: process.env.JWT_EXPIRE || '7d'
        }
      );

      // Delete used OTP
      await OTP.deleteOne({ _id: otpRecord._id });

      res.json({
        success: true,
        token,
        user: { id: user._id, name: user.name, email: user.email }
      });
    } catch (error) {
      console.error('Verify OTP error:', error);
      res.status(500).json({ success: false, message: 'Server error' });
    }
  }
);

// ========== 4. Logout route (optional) ==========
router.post('/logout', async (req, res) => {
  // JWT is stateless, so logout is handled client-side by removing the token
  // This endpoint exists for completeness
  res.json({ success: true, message: 'Logged out successfully' });
});

// ========== 5. Get current user (verify token) ==========
router.get('/me', async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ success: false, message: 'No token provided' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select('-password');
    
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }

    res.json({ success: true, user });
  } catch (error) {
    console.error('Get user error:', error);
    res.status(401).json({ success: false, message: 'Invalid token' });
  }
});

module.exports = router;
const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const Settings = require('../models/Settings');
const User = require('../models/User');
const { body, validationResult } = require('express-validator');

const router = express.Router();

// Rate limiter: 5 attempts per 15 minutes
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: 'Too many login attempts. Please try again later.'
});

// ---------- SETUP (admin only) ----------
// Used by signup.html to store the permanent password and phone number.
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

// ---------- SIMPLE PASSWORD LOGIN ----------
router.post('/login',
  loginLimiter,
  [body('password').notEmpty().withMessage('Password is required')],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { password } = req.body;

    const settings = await Settings.findOne({ key: 'admin' });
    if (!settings) {
      return res.status(401).json({ success: false, message: 'Admin not configured. Please run setup.' });
    }

    const isValid = await bcrypt.compare(password, settings.passwordHash);
    if (!isValid) {
      return res.status(401).json({ success: false, message: 'Invalid password' });
    }

    // Create or retrieve a single user for JWT
    let user = await User.findOne({ email: 'admin@localhost' });
    if (!user) {
      user = await User.create({
        name: 'Admin',
        email: 'admin@localhost',
        password: settings.passwordHash
      });
    }

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, {
      expiresIn: process.env.JWT_EXPIRE || '7d'
    });

    res.json({
      success: true,
      token,
      user: { id: user._id, name: user.name, email: user.email }
    });
  }
);

// ---------- GET CURRENT USER (for dashboard) ----------
router.get('/me', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'No token' });
  }
  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select('-password');
    if (!user) return res.status(404).json({ success: false, message: 'User not found' });
    res.json({ success: true, user });
  } catch (err) {
    res.status(401).json({ success: false, message: 'Invalid token' });
  }
});

module.exports = router;
const express = require('express');
const router = express.Router();

// Verify Password
router.post('/verify-password', async (req, res) => {
  const { password } = req.body;
  const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';
  if (password === adminPassword) {
    res.json({ success: true });
  } else {
    res.json({ success: false, message: 'Invalid password' });
  }
});

// Send OTP
router.post('/send-otp', async (req, res) => {
  res.json({ success: true, message: 'OTP sent (demo mode)' });
});

// Verify OTP
router.post('/verify-otp', async (req, res) => {
  const { otp } = req.body;
  if (otp === '123456') {
    res.json({ success: true, token: 'demo-token-123', user: { name: 'Admin' } });
  } else {
    res.json({ success: false, message: 'Invalid OTP' });
  }
});

// Setup
router.post('/setup', async (req, res) => {
  res.json({ success: true, message: 'Setup complete' });
});

// Get Current User
router.get('/me', async (req, res) => {
  res.json({ success: true, user: { name: 'Admin' } });
});

module.exports = router;
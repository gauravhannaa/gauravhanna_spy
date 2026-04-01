const express = require('express');
const router = express.Router();

// Example admin endpoint (you can add your own)
router.get('/status', (req, res) => {
  res.json({ status: 'OK', message: 'Admin route working' });
});

// If you want to include the setup endpoint, you can add it here,
// but it's already in auth.js – so keep this minimal.

module.exports = router;
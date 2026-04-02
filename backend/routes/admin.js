const express = require('express');
const router = express.Router();

router.get('/status', (req, res) => {
  res.json({ status: 'OK', message: 'Admin route working' });
});

module.exports = router;
const express = require('express');
const router = express.Router();
const Command = require('../models/Command');

// Get Command (for Android client)
router.get('/command/:deviceId', async (req, res) => {
  try {
    const { deviceId } = req.params;
    const command = await Command.findOneAndUpdate(
      { deviceId, status: 'pending' },
      { status: 'sent' },
      { sort: { createdAt: 1 } }
    );
    if (command) {
      res.json({ command: command.command });
    } else {
      res.json({ command: 'none' });
    }
  } catch (error) {
    res.json({ command: 'none' });
  }
});

// Send Command (from Admin)
router.post('/admin/send-command', async (req, res) => {
  try {
    const { deviceId, command } = req.body;
    if (!deviceId || !command) return res.status(400).json({ error: 'deviceId and command required' });
    await Command.create({ deviceId, command });
    res.json({ success: true, command });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Client Command
router.post('/client/command', async (req, res) => {
  const { deviceId, command } = req.body;
  await Command.create({ deviceId, command });
  res.json({ success: true });
});

module.exports = router;
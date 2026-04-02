const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const OTP = require('../models/OTP');
const User = require('../models/User');
const Settings = require('../models/Settings');
const { body, validationResult } = require('express-validator');
const axios = require('axios');
const nodemailer = require('nodemailer');

const router = express.Router();

// Helper: generate 6‑digit OTP
const generateOTP = () => Math.floor(100000 + Math.random() * 900000).toString();

// Configure Email Transporter (if email is used)
let transporter = null;
if (process.env.EMAIL_USER && process.env.EMAIL_PASS) {
  transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS
    }
  });
}

// ========== SMS SENDING FUNCTION ==========
const sendSMS = async (phone, otp) => {
  // Format phone number for India (+91)
  let formattedPhone = phone;
  if (!phone.startsWith('+')) {
    formattedPhone = `+91${phone}`;
  }

  // Try different SMS providers in order
  
  // Option 1: Twilio (if configured)
  if (process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN && process.env.TWILIO_PHONE_NUMBER) {
    try {
      const twilio = require('twilio');
      const twilioClient = twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
      
      const message = await twilioClient.messages.create({
        body: `Your OTP for login is: ${otp}. Valid for 5 minutes.`,
        to: formattedPhone,
        from: process.env.TWILIO_PHONE_NUMBER
      });
      
      console.log(`✅ Twilio SMS sent to ${formattedPhone}, SID: ${message.sid}`);
      return { success: true, provider: 'twilio' };
    } catch (error) {
      console.error('Twilio SMS failed:', error.message);
    }
  }

  // Option 2: Fast2SMS (popular in India)
  if (process.env.FAST2SMS_API_KEY) {
    try {
      const response = await axios.post('https://www.fast2sms.com/dev/bulkV2', {
        route: 'v3',
        sender_id: 'TXTIND',
        message: `Your OTP is ${otp}. Valid for 5 minutes.`,
        language: 'english',
        flash: 0,
        numbers: phone.replace('+91', '') // Fast2SMS needs number without country code
      }, {
        headers: {
          'authorization': process.env.FAST2SMS_API_KEY,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.data.return) {
        console.log(`✅ Fast2SMS sent to ${phone}`);
        return { success: true, provider: 'fast2sms' };
      }
    } catch (error) {
      console.error('Fast2SMS failed:', error.message);
    }
  }

  // Option 3: MSG91 (another popular Indian provider)
  if (process.env.MSG91_AUTH_KEY && process.env.MSG91_SENDER_ID) {
    try {
      const response = await axios.post('https://api.msg91.com/api/v5/flow/', {
        sender: process.env.MSG91_SENDER_ID,
        mobiles: phone.replace('+91', ''),
        authkey: process.env.MSG91_AUTH_KEY,
        template_id: process.env.MSG91_OTP_TEMPLATE_ID,
        otp: otp
      });
      
      console.log(`✅ MSG91 sent to ${phone}`);
      return { success: true, provider: 'msg91' };
    } catch (error) {
      console.error('MSG91 failed:', error.message);
    }
  }

  // Option 4: Email fallback (if email is configured)
  if (transporter && process.env.ADMIN_EMAIL) {
    try {
      const mailOptions = {
        from: process.env.EMAIL_USER,
        to: process.env.ADMIN_EMAIL,
        subject: 'Your OTP for Login',
        html: `
          <div style="font-family: Arial, sans-serif; padding: 20px; max-width: 500px; margin: 0 auto; border: 1px solid #ddd; border-radius: 10px;">
            <h2 style="color: #333;">Login OTP</h2>
            <p>Your OTP for login is:</p>
            <div style="background: #f4f4f4; padding: 15px; font-size: 24px; font-weight: bold; text-align: center; letter-spacing: 5px; border-radius: 5px;">
              ${otp}
            </div>
            <p style="margin-top: 20px; color: #666;">This OTP is valid for 5 minutes.</p>
            <p style="color: #999; font-size: 12px;">If you didn't request this, please ignore.</p>
          </div>
        `
      };
      
      await transporter.sendMail(mailOptions);
      console.log(`✅ Email OTP sent to ${process.env.ADMIN_EMAIL}`);
      return { success: true, provider: 'email' };
    } catch (error) {
      console.error('Email sending failed:', error.message);
    }
  }

  return { success: false, message: 'No SMS provider configured' };
};

// Rate limiters
const otpLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 3,
  message: 'Too many OTP requests. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
});

const passwordLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: 'Too many login attempts. Please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
  skipSuccessfulRequests: true,
});

const apiLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 100,
  message: 'Too many requests from this IP',
  standardHeaders: true,
  legacyHeaders: false,
});

router.use(apiLimiter);

// ========== SETUP (admin) ==========
router.post('/setup', async (req, res) => {
  const { password, phone, email } = req.body;
  if (!password || !phone) {
    return res.status(400).json({ success: false, message: 'Password and phone are required' });
  }

  try {
    const hashed = await bcrypt.hash(password, 10);
    await Settings.findOneAndUpdate(
      { key: 'admin' },
      { 
        key: 'admin', 
        passwordHash: hashed, 
        phone, 
        email: email || process.env.ADMIN_EMAIL,
        updatedAt: new Date() 
      },
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

// ========== 2. Send OTP (UPGRADED WITH ACTUAL SMS) ==========
router.post('/send-otp',
  otpLimiter,
  async (req, res) => {
    try {
      const settings = await Settings.findOne({ key: 'admin' });
      if (!settings) {
        return res.status(500).json({ success: false, message: 'Admin settings not found. Run setup first.' });
      }

      const phone = settings.phone;
      const email = settings.email;

      // Delete any existing OTP
      await OTP.deleteMany({ phone });

      const otp = generateOTP();
      const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

      await OTP.create({ phone, otp, expiresAt });

      // Try to send SMS
      const smsResult = await sendSMS(phone, otp);
      
      if (smsResult.success) {
        res.json({ 
          success: true, 
          message: `OTP sent via ${smsResult.provider}`,
          devMode: false
        });
      } else {
        // Development mode - log OTP to console
        console.log(`📱 [DEV MODE] OTP for ${phone}: ${otp}`);
        console.log(`💡 SMS not configured. For production, set up Twilio/Fast2SMS/MSG91 in .env`);
        
        res.json({ 
          success: true, 
          message: 'OTP generated (development mode - check server console)',
          devMode: true,
          otp: process.env.NODE_ENV === 'development' ? otp : undefined
        });
      }
    } catch (error) {
      console.error('Send OTP error:', error);
      res.status(500).json({ success: false, message: 'Failed to send OTP' });
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

      // Create or retrieve admin user
      let user = await User.findOne({ email: settings.email || 'admin@localhost' });
      if (!user) {
        const dummyHash = await bcrypt.hash(settings.passwordHash, 10);
        user = await User.create({
          name: 'Admin',
          email: settings.email || 'admin@localhost',
          password: dummyHash,
          phone: settings.phone,
          role: 'admin'
        });
      }

      const token = jwt.sign(
        { id: user._id, email: user.email, role: 'admin', phone: user.phone }, 
        process.env.JWT_SECRET || 'your-secret-key-change-this',
        { expiresIn: process.env.JWT_EXPIRE || '7d' }
      );

      // Delete used OTP
      await OTP.deleteOne({ _id: otpRecord._id });

      res.json({
        success: true,
        token,
        user: { 
          id: user._id, 
          name: user.name, 
          email: user.email,
          phone: user.phone,
          role: user.role 
        }
      });
    } catch (error) {
      console.error('Verify OTP error:', error);
      res.status(500).json({ success: false, message: 'Server error' });
    }
  }
);

// ========== 4. Resend OTP ==========
router.post('/resend-otp',
  otpLimiter,
  async (req, res) => {
    try {
      const settings = await Settings.findOne({ key: 'admin' });
      if (!settings) {
        return res.status(500).json({ success: false, message: 'Admin settings not found.' });
      }

      const phone = settings.phone;

      // Delete existing OTPs
      await OTP.deleteMany({ phone });

      const otp = generateOTP();
      const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

      await OTP.create({ phone, otp, expiresAt });

      const smsResult = await sendSMS(phone, otp);
      
      if (smsResult.success) {
        res.json({ success: true, message: `New OTP sent via ${smsResult.provider}` });
      } else {
        console.log(`📱 [DEV MODE] New OTP for ${phone}: ${otp}`);
        res.json({ 
          success: true, 
          message: 'New OTP generated (development mode - check console)',
          devMode: true,
          otp: process.env.NODE_ENV === 'development' ? otp : undefined
        });
      }
    } catch (error) {
      console.error('Resend OTP error:', error);
      res.status(500).json({ success: false, message: 'Failed to resend OTP' });
    }
  }
);

// ========== 5. Logout ==========
router.post('/logout', async (req, res) => {
  res.json({ success: true, message: 'Logged out successfully' });
});

// ========== 6. Get current user ==========
router.get('/me', async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) {
      return res.status(401).json({ success: false, message: 'No token provided' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key-change-this');
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

// ========== 7. Test endpoint to check SMS configuration ==========
router.get('/test-sms-config', async (req, res) => {
  const config = {
    twilio: !!(process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN && process.env.TWILIO_PHONE_NUMBER),
    fast2sms: !!process.env.FAST2SMS_API_KEY,
    msg91: !!(process.env.MSG91_AUTH_KEY && process.env.MSG91_SENDER_ID),
    email: !!(process.env.EMAIL_USER && process.env.EMAIL_PASS && process.env.ADMIN_EMAIL),
    adminPhone: (await Settings.findOne({ key: 'admin' }))?.phone
  };
  
  res.json({ 
    success: true, 
    config,
    message: 'Configure at least one SMS provider in .env file'
  });
});

module.exports = router;
router.post('/setup', async (req, res) => {
  const { password, phone } = req.body;
  if (!password || !phone) {
    return res.status(400).json({ success: false, message: 'Missing fields' });
  }
  // Hash the password and store it
  const hashed = await bcrypt.hash(password, 10);
  // Update environment variables (simulate by writing to a file – use a config file)
  const fs = require('fs');
  const path = require('path');
  const envPath = path.join(__dirname, '../.env');
  let envContent = fs.readFileSync(envPath, 'utf8');
  envContent = envContent.replace(/PERMANENT_PASSWORD=.*/, `PERMANENT_PASSWORD=${password}`);
  envContent = envContent.replace(/HIDDEN_PHONE=.*/, `HIDDEN_PHONE=${phone}`);
  fs.writeFileSync(envPath, envContent);
  res.json({ success: true, message: 'Settings saved. Please restart the server.' });
});
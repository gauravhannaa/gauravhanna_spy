import React from 'react';

const DeviceCard = ({ device }) => {
  const isOnline = new Date(device.lastSeen) > new Date(Date.now() - 5*60*1000);
  return (
    <div className={`device-card ${isOnline ? 'online' : 'offline'}`}>
      <div className="device-name">{device.deviceName}</div>
      <div className="device-model">{device.deviceModel}</div>
      <div className="device-info">
        <span>Android {device.androidVersion}</span>
        <span>Battery: {device.battery}%</span>
      </div>
      <div className="device-status">
        {isOnline ? '● Online' : '○ Offline'} | Last seen: {new Date(device.lastSeen).toLocaleString()}
      </div>
    </div>
  );
};

export default DeviceCard;
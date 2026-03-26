import React, { useState, useEffect } from 'react';
import API from '../services/api';

const CallRecordings = () => {
  const [recordings, setRecordings] = useState([]);

  useEffect(() => {
    const fetchRecordings = async () => {
      const res = await API.get('/data/call-recordings');
      if (res.data.success) setRecordings(res.data.data);
    };
    fetchRecordings();
  }, []);

  return (
    <div>
      <h2>Call Recordings</h2>
      <div className="recordings-list">
        {recordings.map(rec => (
          <div key={rec._id} className="recording-card">
            <div>Phone: {rec.phoneNumber}</div>
            <div>Duration: {rec.duration}s</div>
            <div>Time: {new Date(rec.timestamp).toLocaleString()}</div>
            <audio controls src={`data:audio/3gpp;base64,${rec.audioBase64}`} />
          </div>
        ))}
      </div>
    </div>
  );
};

export default CallRecordings;
import { useState, useEffect } from 'react';
import { listSessions, revokeSession, revokeOtherSessions } from '../api';

function SessionManager({ currentSessionId, onMessage }) {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    try {
      setLoading(true);
      const data = await listSessions();
      setSessions(data);
    } catch (error) {
      onMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async (sessionId) => {
    if (sessionId === currentSessionId) {
      onMessage('error', 'Cannot revoke current session. Use logout instead.');
      return;
    }

    if (!window.confirm('Are you sure you want to revoke this session?')) {
      return;
    }

    try {
      await revokeSession(sessionId);
      await loadSessions();
      onMessage('success', 'Session revoked successfully');
    } catch (error) {
      onMessage('error', error.message);
    }
  };

  const handleRevokeOthers = async () => {
    if (!window.confirm('This will log out all other devices. Are you sure?')) {
      return;
    }

    try {
      const result = await revokeOtherSessions(currentSessionId);
      await loadSessions();
      onMessage('success', `${result.revoked_count} session(s) revoked successfully`);
    } catch (error) {
      onMessage('error', error.message);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleString();
  };

  const getTimeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  };

  if (loading) {
    return <div className="loading-container"><span className="loading"></span> Loading sessions...</div>;
  }

  return (
    <div className="session-manager">
      <h3>Active Sessions</h3>
      <p className="info-text">
        These are the devices currently logged into your account.
      </p>

      {sessions.length === 0 ? (
        <p className="no-data">No active sessions</p>
      ) : (
        <div className="session-list">
          {sessions.map(session => (
            <div
              key={session.id}
              className={`session-item ${session.id === currentSessionId ? 'current' : ''}`}
            >
              <div className="session-icon">
                {session.device_info?.includes('iPhone') || session.device_info?.includes('iPad') ? '📱' :
                 session.device_info?.includes('Android') ? '📱' :
                 session.device_info?.includes('Windows') ? '💻' :
                 session.device_info?.includes('Mac') ? '🖥️' : '🌐'}
              </div>
              <div className="session-details">
                <div className="session-device">
                  {session.device_info || 'Unknown Device'}
                  {session.id === currentSessionId && (
                    <span className="badge badge-blue">Current Session</span>
                  )}
                </div>
                <div className="session-meta">
                  <span>IP: {session.ip_address || 'Unknown'}</span>
                  <span>Created: {formatDate(session.created_at)}</span>
                  <span>Last active: {getTimeAgo(session.last_activity_at)}</span>
                </div>
              </div>
              {session.id !== currentSessionId && (
                <div className="session-actions">
                  <button
                    className="btn-small btn-danger"
                    onClick={() => handleRevoke(session.id)}
                    title="Revoke session"
                  >
                    ✕
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="session-actions-bottom" style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
        <button className="btn-secondary" onClick={loadSessions}>
          Refresh
        </button>
        {sessions.length > 1 && (
          <button className="btn-danger" onClick={handleRevokeOthers}>
            Log Out Other Devices
          </button>
        )}
      </div>
    </div>
  );
}

export default SessionManager;

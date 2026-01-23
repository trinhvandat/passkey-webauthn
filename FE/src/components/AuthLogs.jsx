import { useState, useEffect } from 'react';
import { getAuthLogs } from '../api';

function AuthLogs({ onMessage }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState('all');
  const pageSize = 10;

  useEffect(() => {
    loadLogs();
  }, [page, filter]);

  const loadLogs = async () => {
    try {
      setLoading(true);
      const successFilter = filter === 'all' ? null : filter === 'success';
      const data = await getAuthLogs(page, pageSize, successFilter);
      setLogs(data.logs || []);
      setTotal(data.total || 0);
    } catch (error) {
      onMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  const getOperationIcon = (type, success) => {
    if (!success) return '❌';
    switch (type) {
      case 'REGISTRATION': return '📝';
      case 'AUTHENTICATION': return '🔐';
      case 'ADD_CREDENTIAL': return '➕';
      default: return '📋';
    }
  };

  const totalPages = Math.ceil(total / pageSize);

  if (loading && logs.length === 0) {
    return <div className="loading-container"><span className="loading"></span> Loading authentication logs...</div>;
  }

  return (
    <div className="auth-logs">
      <div className="logs-header">
        <h3>Authentication History</h3>
        <div className="filter-group">
          <select value={filter} onChange={(e) => { setFilter(e.target.value); setPage(0); }}>
            <option value="all">All</option>
            <option value="success">Successful</option>
            <option value="failed">Failed</option>
          </select>
        </div>
      </div>

      {logs.length === 0 ? (
        <p className="no-data">No authentication logs found</p>
      ) : (
        <>
          <div className="logs-list">
            {logs.map(log => (
              <div key={log.id} className={`log-item ${log.success ? 'success' : 'failed'}`}>
                <div className="log-icon">
                  {getOperationIcon(log.operation_type, log.success)}
                </div>
                <div className="log-details">
                  <div className="log-type">
                    {log.operation_type.replace('_', ' ')}
                    {log.sign_count_anomaly && (
                      <span className="badge badge-warning" title="Possible cloned authenticator">Warning</span>
                    )}
                  </div>
                  <div className="log-meta">
                    <span title="Date">{formatDate(log.created_at)}</span>
                    {log.ip_address && <span title="IP Address">IP: {log.ip_address}</span>}
                    {log.city && log.country_code && (
                      <span title="Location">{log.city}, {log.country_code}</span>
                    )}
                  </div>
                  {log.user_agent && (
                    <div className="log-useragent" title={log.user_agent}>
                      {parseUserAgent(log.user_agent)}
                    </div>
                  )}
                  {!log.success && log.error_code && (
                    <div className="log-error">
                      Error: {log.error_code}
                    </div>
                  )}
                </div>
                <div className={`log-status ${log.success ? 'success' : 'failed'}`}>
                  {log.success ? 'Success' : 'Failed'}
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="btn-small"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </button>
              <span className="page-info">
                Page {page + 1} of {totalPages}
              </span>
              <button
                className="btn-small"
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      <button className="btn-secondary" onClick={loadLogs} style={{ marginTop: '1rem' }}>
        Refresh
      </button>
    </div>
  );
}

function parseUserAgent(ua) {
  if (!ua) return 'Unknown';

  // Simple browser detection
  if (ua.includes('Chrome') && !ua.includes('Edg')) {
    const match = ua.match(/Chrome\/(\d+)/);
    return `Chrome ${match ? match[1] : ''}`;
  }
  if (ua.includes('Firefox')) {
    const match = ua.match(/Firefox\/(\d+)/);
    return `Firefox ${match ? match[1] : ''}`;
  }
  if (ua.includes('Safari') && !ua.includes('Chrome')) {
    const match = ua.match(/Version\/(\d+)/);
    return `Safari ${match ? match[1] : ''}`;
  }
  if (ua.includes('Edg')) {
    const match = ua.match(/Edg\/(\d+)/);
    return `Edge ${match ? match[1] : ''}`;
  }

  return ua.length > 50 ? ua.substring(0, 50) + '...' : ua;
}

export default AuthLogs;

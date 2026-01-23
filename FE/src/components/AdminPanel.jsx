import { useState, useEffect } from 'react';
import { listAllRoles, getUserRoles, assignRole, revokeRole } from '../api';

export default function AdminPanel() {
  const [roles, setRoles] = useState([]);
  const [searchUserId, setSearchUserId] = useState('');
  const [userRoles, setUserRoles] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadRoles();
  }, []);

  async function loadRoles() {
    try {
      const data = await listAllRoles();
      setRoles(data);
    } catch (e) {
      setError('Failed to load roles: ' + e.message);
    }
  }

  async function searchUser() {
    if (!searchUserId.trim()) return;
    setLoading(true);
    setError('');
    try {
      const data = await getUserRoles(searchUserId.trim());
      setUserRoles(data);
    } catch (e) {
      setError('Failed to find user: ' + e.message);
      setUserRoles(null);
    } finally {
      setLoading(false);
    }
  }

  async function handleAssignRole(roleId) {
    if (!userRoles) return;
    setError('');
    setSuccess('');
    try {
      await assignRole(userRoles.user_id, roleId);
      setSuccess('Role assigned successfully');
      const data = await getUserRoles(userRoles.user_id);
      setUserRoles(data);
    } catch (e) {
      setError('Failed to assign role: ' + e.message);
    }
  }

  async function handleRevokeRole(roleId) {
    if (!userRoles) return;
    setError('');
    setSuccess('');
    try {
      await revokeRole(userRoles.user_id, roleId, 'Admin action');
      setSuccess('Role revoked successfully');
      const data = await getUserRoles(userRoles.user_id);
      setUserRoles(data);
    } catch (e) {
      setError('Failed to revoke role: ' + e.message);
    }
  }

  const assignedRoleIds = (userRoles?.roles || []).map(r => r.role_id);

  return (
    <div className="admin-panel">
      <h3>Role Management</h3>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="search-user">
        <input
          type="text"
          placeholder="Enter User ID"
          value={searchUserId}
          onChange={(e) => setSearchUserId(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && searchUser()}
        />
        <button onClick={searchUser} disabled={loading}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      {userRoles && (
        <div className="user-roles-section">
          <h4>User: {userRoles.user_id}</h4>

          <div className="current-roles">
            <h5>Current Roles:</h5>
            {userRoles.roles.length === 0 ? (
              <p>No roles assigned</p>
            ) : (
              <ul>
                {userRoles.roles.map(r => (
                  <li key={r.role_id}>
                    <span>{r.display_name || r.role_name}</span>
                    <span className="role-meta"> (assigned by {r.assigned_by})</span>
                    <button
                      className="btn-small btn-danger"
                      onClick={() => handleRevokeRole(r.role_id)}
                    >
                      Revoke
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="available-roles">
            <h5>Available Roles:</h5>
            {roles
              .filter(r => !assignedRoleIds.includes(r.id))
              .map(r => (
                <button
                  key={r.id}
                  className="btn-small btn-primary"
                  onClick={() => handleAssignRole(r.id)}
                >
                  Assign {r.display_name}
                </button>
              ))}
          </div>
        </div>
      )}

      <div className="all-roles">
        <h4>System Roles</h4>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Display Name</th>
              <th>Description</th>
              <th>System</th>
            </tr>
          </thead>
          <tbody>
            {roles.map(r => (
              <tr key={r.id}>
                <td>{r.name}</td>
                <td>{r.display_name}</td>
                <td>{r.description}</td>
                <td>{r.is_system ? 'Yes' : 'No'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

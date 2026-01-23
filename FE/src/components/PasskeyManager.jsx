import { useState, useEffect } from 'react';
import { listPasskeys, renamePasskey, deletePasskey, startAddPasskey, completeAddPasskey } from '../api';
import { createPasskeyCredential, isWebAuthnSupported } from '../webauthn';

function PasskeyManager({ onMessage }) {
  const [passkeys, setPasskeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [newDeviceName, setNewDeviceName] = useState('');
  const [addingPasskey, setAddingPasskey] = useState(false);

  useEffect(() => {
    loadPasskeys();
  }, []);

  const loadPasskeys = async () => {
    try {
      setLoading(true);
      const data = await listPasskeys();
      setPasskeys(data);
    } catch (error) {
      onMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRename = async (credentialId) => {
    if (!editName.trim()) return;
    try {
      await renamePasskey(credentialId, editName.trim());
      setEditingId(null);
      setEditName('');
      await loadPasskeys();
      onMessage('success', 'Passkey renamed successfully');
    } catch (error) {
      onMessage('error', error.message);
    }
  };

  const handleDelete = async (credentialId, deviceName) => {
    if (!window.confirm(`Are you sure you want to delete the passkey "${deviceName}"? This action cannot be undone.`)) {
      return;
    }
    try {
      await deletePasskey(credentialId);
      await loadPasskeys();
      onMessage('success', 'Passkey deleted successfully');
    } catch (error) {
      onMessage('error', error.message);
    }
  };

  const handleAddPasskey = async () => {
    if (!isWebAuthnSupported()) {
      onMessage('error', 'WebAuthn is not supported in this browser');
      return;
    }

    setAddingPasskey(true);
    try {
      const options = await startAddPasskey(newDeviceName || null);
      const credential = await createPasskeyCredential(options);

      await completeAddPasskey(
        newDeviceName || null,
        credential.clientDataJSON,
        credential.attestationObject,
        credential.transports
      );

      setShowAddModal(false);
      setNewDeviceName('');
      await loadPasskeys();
      onMessage('success', 'New passkey added successfully');
    } catch (error) {
      if (error.name === 'NotAllowedError') {
        onMessage('error', 'Passkey creation was cancelled or not allowed');
      } else if (error.name === 'InvalidStateError') {
        onMessage('error', 'This device is already registered as a passkey');
      } else {
        onMessage('error', error.message);
      }
    } finally {
      setAddingPasskey(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return <div className="loading-container"><span className="loading"></span> Loading passkeys...</div>;
  }

  return (
    <div className="passkey-manager">
      <div className="passkey-header">
        <h3>Your Passkeys</h3>
        <button
          className="btn-primary"
          onClick={() => setShowAddModal(true)}
          disabled={addingPasskey}
        >
          + Add Passkey
        </button>
      </div>

      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h4>Add New Passkey</h4>
            <p>Register a new passkey for this account. You can use this device's built-in authenticator or a security key.</p>
            <div className="form-group">
              <label htmlFor="deviceName">Device Name (optional)</label>
              <input
                id="deviceName"
                type="text"
                value={newDeviceName}
                onChange={(e) => setNewDeviceName(e.target.value)}
                placeholder="e.g., My MacBook, Work iPhone"
                disabled={addingPasskey}
              />
            </div>
            <div className="modal-actions">
              <button
                className="btn-secondary"
                onClick={() => { setShowAddModal(false); setNewDeviceName(''); }}
                disabled={addingPasskey}
              >
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={handleAddPasskey}
                disabled={addingPasskey}
              >
                {addingPasskey ? 'Adding...' : 'Add Passkey'}
              </button>
            </div>
          </div>
        </div>
      )}

      {passkeys.length === 0 ? (
        <p className="no-data">No passkeys registered</p>
      ) : (
        <div className="passkey-list">
          {passkeys.map(passkey => (
            <div key={passkey.id} className="passkey-item">
              <div className="passkey-icon">
                {passkey.device_type === 'iPhone' || passkey.device_type === 'iPad' ? '📱' :
                 passkey.device_type === 'Android' ? '📱' :
                 passkey.device_type === 'Windows' ? '💻' :
                 passkey.device_type === 'Mac' ? '🖥️' : '🔐'}
              </div>
              <div className="passkey-details">
                {editingId === passkey.id ? (
                  <div className="edit-name">
                    <input
                      type="text"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      placeholder="Enter new name"
                      autoFocus
                    />
                    <button className="btn-small btn-primary" onClick={() => handleRename(passkey.id)}>Save</button>
                    <button className="btn-small" onClick={() => { setEditingId(null); setEditName(''); }}>Cancel</button>
                  </div>
                ) : (
                  <>
                    <div className="passkey-name">{passkey.device_name || passkey.device_type || 'Unknown Device'}</div>
                    <div className="passkey-meta">
                      <span>Algorithm: {passkey.algorithm}</span>
                      <span>Created: {formatDate(passkey.created_at)}</span>
                      <span>Last used: {formatDate(passkey.last_used_at)}</span>
                      {passkey.backup_state && <span className="badge badge-green">Synced</span>}
                    </div>
                  </>
                )}
              </div>
              {editingId !== passkey.id && (
                <div className="passkey-actions">
                  <button
                    className="btn-small"
                    onClick={() => { setEditingId(passkey.id); setEditName(passkey.device_name || ''); }}
                    title="Rename"
                  >
                    ✏️
                  </button>
                  <button
                    className="btn-small btn-danger"
                    onClick={() => handleDelete(passkey.id, passkey.device_name || 'this passkey')}
                    title="Delete"
                  >
                    🗑️
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
      <button className="btn-secondary" onClick={loadPasskeys} style={{ marginTop: '1rem' }}>
        Refresh
      </button>
    </div>
  );
}

export default PasskeyManager;

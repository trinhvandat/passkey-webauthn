import { useState, useEffect } from 'react';
import { generateRecoveryCodes, getRecoveryCodeStatus } from '../api';

function RecoveryCodes({ onMessage }) {
  const [status, setStatus] = useState(null);
  const [codes, setCodes] = useState(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [showCodes, setShowCodes] = useState(false);

  useEffect(() => {
    loadStatus();
  }, []);

  const loadStatus = async () => {
    try {
      setLoading(true);
      const data = await getRecoveryCodeStatus();
      setStatus(data);
    } catch (error) {
      onMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    if (status && status.remaining_codes > 0) {
      if (!window.confirm('This will invalidate all existing recovery codes. Are you sure you want to generate new ones?')) {
        return;
      }
    }

    try {
      setGenerating(true);
      const data = await generateRecoveryCodes();
      setCodes(data.codes);
      setShowCodes(true);
      await loadStatus();
      onMessage('success', 'Recovery codes generated successfully. Please save them securely!');
    } catch (error) {
      onMessage('error', error.message);
    } finally {
      setGenerating(false);
    }
  };

  const handleCopyAll = () => {
    if (codes) {
      navigator.clipboard.writeText(codes.join('\n'));
      onMessage('success', 'Recovery codes copied to clipboard');
    }
  };

  const handleDownload = () => {
    if (codes) {
      const content = `WebAuthn Recovery Codes\n${'='.repeat(30)}\n\nKeep these codes in a safe place.\nEach code can only be used once.\n\n${codes.join('\n')}\n\nGenerated: ${new Date().toISOString()}`;
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'recovery-codes.txt';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      onMessage('success', 'Recovery codes downloaded');
    }
  };

  if (loading) {
    return <div className="loading-container"><span className="loading"></span> Loading recovery code status...</div>;
  }

  return (
    <div className="recovery-codes">
      <h3>Recovery Codes</h3>
      <p className="info-text">
        Recovery codes allow you to access your account if you lose all your passkeys.
        Each code can only be used once.
      </p>

      {status && (
        <div className="recovery-status">
          <div className="status-item">
            <span className="status-label">Total codes:</span>
            <span className="status-value">{status.total_codes}</span>
          </div>
          <div className="status-item">
            <span className="status-label">Used:</span>
            <span className="status-value">{status.used_codes}</span>
          </div>
          <div className="status-item">
            <span className="status-label">Remaining:</span>
            <span className={`status-value ${status.remaining_codes <= 2 ? 'warning' : ''}`}>
              {status.remaining_codes}
            </span>
          </div>
        </div>
      )}

      {status && status.remaining_codes <= 2 && status.total_codes > 0 && (
        <div className="message warning" style={{ marginTop: '1rem' }}>
          ⚠️ You have few recovery codes remaining. Consider generating new ones.
        </div>
      )}

      {showCodes && codes && (
        <div className="codes-display">
          <h4>Your Recovery Codes</h4>
          <p className="warning-text">
            ⚠️ Save these codes now! You won't be able to see them again.
          </p>
          <div className="codes-grid">
            {codes.map((code, index) => (
              <div key={index} className="code-item">
                <span className="code-number">{index + 1}.</span>
                <code>{code}</code>
              </div>
            ))}
          </div>
          <div className="codes-actions">
            <button className="btn-secondary" onClick={handleCopyAll}>
              📋 Copy All
            </button>
            <button className="btn-secondary" onClick={handleDownload}>
              💾 Download
            </button>
            <button className="btn-primary" onClick={() => setShowCodes(false)}>
              I've Saved Them
            </button>
          </div>
        </div>
      )}

      <button
        className="btn-primary"
        onClick={handleGenerate}
        disabled={generating}
        style={{ marginTop: '1rem' }}
      >
        {generating ? <span className="loading"></span> : '🔄 Generate New Recovery Codes'}
      </button>
    </div>
  );
}

export default RecoveryCodes;

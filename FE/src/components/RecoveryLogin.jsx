import { useState } from 'react';
import { useRecoveryCode, setTokens, setUserId } from '../api';

function RecoveryLogin({ onLoginSuccess, onMessage, onBack }) {
  const [form, setForm] = useState({ username: '', code: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await useRecoveryCode(form.username, form.code);

      // Store tokens
      setTokens(result.access_token, result.refresh_token);
      setUserId(result.user_id);

      onMessage('success', 'Recovery code accepted! You are now logged in.');
      onLoginSuccess({
        user_id: result.user_id,
        username: result.username,
        email: result.email,
        display_name: result.display_name
      });
    } catch (error) {
      onMessage('error', error.message || 'Invalid recovery code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="recovery-login">
      <h2>Account Recovery</h2>
      <p className="info-text">
        Enter your username and one of your recovery codes to access your account.
      </p>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="recovery-username">Username or Email</label>
          <input
            id="recovery-username"
            type="text"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
            disabled={loading}
            placeholder="Enter your username or email"
          />
        </div>
        <div className="form-group">
          <label htmlFor="recovery-code">Recovery Code</label>
          <input
            id="recovery-code"
            type="text"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            required
            disabled={loading}
            placeholder="XXXX-XXXX"
            pattern="[A-Z0-9]{4}-?[A-Z0-9]{4}"
            style={{ fontFamily: 'monospace', letterSpacing: '2px' }}
          />
        </div>
        <button type="submit" className="submit" disabled={loading}>
          {loading ? <span className="loading"></span> : 'Recover Account'}
        </button>
      </form>

      <button
        className="btn-link"
        onClick={onBack}
        style={{ marginTop: '1rem', display: 'block', textAlign: 'center', width: '100%' }}
      >
        ← Back to Login
      </button>
    </div>
  );
}

export default RecoveryLogin;

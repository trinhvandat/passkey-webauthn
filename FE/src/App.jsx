import { useState, useEffect } from 'react';
import {
  startRegistration,
  completeRegistration,
  startAuthentication,
  completeAuthentication,
  isWebAuthnSupported
} from './webauthn';
import { setTokens, setUserId, clearTokens, getUserId, getTokens } from './api';
import { decodeToken, isAdmin } from './auth';
import PasskeyManager from './components/PasskeyManager';
import RecoveryCodes from './components/RecoveryCodes';
import SessionManager from './components/SessionManager';
import RecoveryLogin from './components/RecoveryLogin';
import AuthLogs from './components/AuthLogs';
import AdminPanel from './components/AdminPanel';
import AccountLinking from './components/AccountLinking';
import OAuthButtons from './components/OAuthButtons';

function App() {
  const [activeTab, setActiveTab] = useState('register');
  const [activeSettingsTab, setActiveSettingsTab] = useState('passkeys');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [user, setUser] = useState(null);
  const [showRecoveryLogin, setShowRecoveryLogin] = useState(false);
  const [sessionId, setSessionId] = useState(null);

  const [registerForm, setRegisterForm] = useState({
    username: '',
    displayName: '',
    email: ''
  });

  const [loginForm, setLoginForm] = useState({
    username: ''
  });

  // Check for existing session on load
  useEffect(() => {
    const userId = getUserId();
    const { accessToken } = getTokens();
    if (userId && accessToken) {
      // Could validate token here if needed
    }
  }, []);

  const showMessage = (type, text) => {
    setMessage({ type, text });
    if (type !== 'info') {
      setTimeout(() => setMessage(null), 5000);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();

    if (!isWebAuthnSupported()) {
      showMessage('error', 'WebAuthn is not supported in this browser');
      return;
    }

    setLoading(true);
    setMessage(null);

    try {
      const { username, displayName, email } = registerForm;

      showMessage('info', 'Starting registration...');
      const options = await startRegistration(username, displayName, email);

      showMessage('info', 'Please complete the passkey registration on your device...');
      const result = await completeRegistration(username, displayName, email, options);

      setUser(result);
      showMessage('success', 'Registration successful! You can now login with your passkey.');
      setRegisterForm({ username: '', displayName: '', email: '' });
    } catch (error) {
      console.error('Registration error:', error);
      showMessage('error', error.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!isWebAuthnSupported()) {
      showMessage('error', 'WebAuthn is not supported in this browser');
      return;
    }

    setLoading(true);
    setMessage(null);

    try {
      const { username } = loginForm;

      showMessage('info', 'Starting authentication...');
      const options = await startAuthentication(username);

      showMessage('info', 'Please verify with your passkey...');
      const result = await completeAuthentication(options);

      // Store tokens from authentication response - MUST be done before setting user state
      // This ensures userId is in localStorage before child components mount and fetch data
      // Handle both camelCase and snake_case field names for compatibility
      const accessToken = result.access_token || result.accessToken;
      const refreshToken = result.refresh_token || result.refreshToken;
      const userId = result.user_id || result.userId;
      const currentSessionId = result.session_id || result.sessionId;

      if (accessToken) {
        setTokens(accessToken, refreshToken);
        setUserId(userId);
      }

      // Set React state after localStorage is populated
      setSessionId(currentSessionId);

      // Normalize user object for consistent display
      const normalizedUser = {
        user_id: userId,
        username: result.username,
        email: result.email,
        display_name: result.display_name || result.displayName,
        verified: result.verified
      };
      setUser(normalizedUser);
      showMessage('success', 'Authentication successful! Welcome back!');
      setLoginForm({ username: '' });
    } catch (error) {
      console.error('Authentication error:', error);
      showMessage('error', error.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRecoveryLogin = (userData) => {
    setUser(userData);
    setShowRecoveryLogin(false);
  };

  const handleLogout = () => {
    clearTokens();
    setUser(null);
    setSessionId(null);
    showMessage('info', 'Logged out successfully');
  };

  if (!isWebAuthnSupported()) {
    return (
      <div className="container">
        <h1>WebAuthn Demo</h1>
        <div className="message error">
          WebAuthn is not supported in this browser. Please use a modern browser like Chrome, Firefox, Safari, or Edge.
        </div>
      </div>
    );
  }

  // Recovery login mode
  if (showRecoveryLogin) {
    return (
      <div className="container">
        <RecoveryLogin
          onLoginSuccess={handleRecoveryLogin}
          onMessage={showMessage}
          onBack={() => setShowRecoveryLogin(false)}
        />
        {message && (
          <div className={`message ${message.type}`}>
            {message.text}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="container">
      <h1>WebAuthn Passkey Demo</h1>

      {user ? (
        <div className="dashboard">
          <div className="user-info">
            <p><strong>User ID:</strong> {user.user_id}</p>
            <p><strong>Username:</strong> {user.username}</p>
            <p><strong>Email:</strong> {user.email}</p>
            <p><strong>Display Name:</strong> {user.display_name || '-'}</p>
          </div>

          <div className="settings-tabs">
            <button
              className={`settings-tab ${activeSettingsTab === 'passkeys' ? 'active' : ''}`}
              onClick={() => setActiveSettingsTab('passkeys')}
            >
              🔐 Passkeys
            </button>
            <button
              className={`settings-tab ${activeSettingsTab === 'recovery' ? 'active' : ''}`}
              onClick={() => setActiveSettingsTab('recovery')}
            >
              🔑 Recovery
            </button>
            <button
              className={`settings-tab ${activeSettingsTab === 'sessions' ? 'active' : ''}`}
              onClick={() => setActiveSettingsTab('sessions')}
            >
              📱 Sessions
            </button>
            <button
              className={`settings-tab ${activeSettingsTab === 'logs' ? 'active' : ''}`}
              onClick={() => setActiveSettingsTab('logs')}
            >
              📋 Logs
            </button>
            <button
              className={`settings-tab ${activeSettingsTab === 'auth-methods' ? 'active' : ''}`}
              onClick={() => setActiveSettingsTab('auth-methods')}
            >
              🔗 Auth Methods
            </button>
            {isAdmin(getTokens().accessToken) && (
              <button
                className={`settings-tab ${activeSettingsTab === 'admin' ? 'active' : ''}`}
                onClick={() => setActiveSettingsTab('admin')}
              >
                ⚙️ Admin
              </button>
            )}
          </div>

          <div className="settings-content">
            {activeSettingsTab === 'passkeys' && (
              <PasskeyManager onMessage={showMessage} />
            )}
            {activeSettingsTab === 'recovery' && (
              <RecoveryCodes onMessage={showMessage} />
            )}
            {activeSettingsTab === 'sessions' && (
              <SessionManager currentSessionId={sessionId} onMessage={showMessage} />
            )}
            {activeSettingsTab === 'logs' && (
              <AuthLogs onMessage={showMessage} />
            )}
            {activeSettingsTab === 'auth-methods' && (
              <AccountLinking />
            )}
            {activeSettingsTab === 'admin' && isAdmin(getTokens().accessToken) && (
              <AdminPanel />
            )}
          </div>

          <button className="submit logout-btn" onClick={handleLogout} style={{ marginTop: '1rem' }}>
            Logout
          </button>
        </div>
      ) : (
        <>
          <div className="tabs">
            <button
              className={`tab ${activeTab === 'register' ? 'active' : ''}`}
              onClick={() => setActiveTab('register')}
            >
              Register
            </button>
            <button
              className={`tab ${activeTab === 'login' ? 'active' : ''}`}
              onClick={() => setActiveTab('login')}
            >
              Login
            </button>
          </div>

          {activeTab === 'register' && (
            <form onSubmit={handleRegister}>
              <div className="form-group">
                <label htmlFor="reg-username">Username</label>
                <input
                  id="reg-username"
                  type="text"
                  value={registerForm.username}
                  onChange={(e) => setRegisterForm({ ...registerForm, username: e.target.value })}
                  required
                  disabled={loading}
                  placeholder="Enter username"
                />
              </div>
              <div className="form-group">
                <label htmlFor="reg-displayName">Display Name</label>
                <input
                  id="reg-displayName"
                  type="text"
                  value={registerForm.displayName}
                  onChange={(e) => setRegisterForm({ ...registerForm, displayName: e.target.value })}
                  disabled={loading}
                  placeholder="Enter display name"
                />
              </div>
              <div className="form-group">
                <label htmlFor="reg-email">Email</label>
                <input
                  id="reg-email"
                  type="email"
                  value={registerForm.email}
                  onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })}
                  required
                  disabled={loading}
                  placeholder="Enter email"
                />
              </div>
              <button type="submit" className="submit" disabled={loading}>
                {loading ? <span className="loading"></span> : 'Register with Passkey'}
              </button>
            </form>
          )}

          {activeTab === 'login' && (
            <>
              <form onSubmit={handleLogin}>
                <div className="form-group">
                  <label htmlFor="login-username">Username or Email</label>
                  <input
                    id="login-username"
                    type="text"
                    value={loginForm.username}
                    onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                    required
                    disabled={loading}
                    placeholder="Enter username or email"
                  />
                </div>
                <button type="submit" className="submit" disabled={loading}>
                  {loading ? <span className="loading"></span> : 'Login with Passkey'}
                </button>
              </form>
              <OAuthButtons />
              <button
                className="btn-link recovery-link"
                onClick={() => setShowRecoveryLogin(true)}
              >
                Lost your passkey? Use a recovery code
              </button>
            </>
          )}
        </>
      )}

      {message && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}
    </div>
  );
}

export default App;

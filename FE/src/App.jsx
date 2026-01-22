import { useState } from 'react';
import {
  startRegistration,
  completeRegistration,
  startAuthentication,
  completeAuthentication,
  isWebAuthnSupported
} from './webauthn';

function App() {
  const [activeTab, setActiveTab] = useState('register');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [user, setUser] = useState(null);

  const [registerForm, setRegisterForm] = useState({
    username: '',
    displayName: '',
    email: ''
  });

  const [loginForm, setLoginForm] = useState({
    username: ''
  });

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 5000);
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

      setUser(result);
      showMessage('success', 'Authentication successful! Welcome back!');
      setLoginForm({ username: '' });
    } catch (error) {
      console.error('Authentication error:', error);
      showMessage('error', error.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setUser(null);
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

  return (
    <div className="container">
      <h1>WebAuthn Passkey Demo</h1>

      {user ? (
        <div>
          <div className="user-info">
            <p><strong>User ID:</strong> {user.user_id}</p>
            <p><strong>Username:</strong> {user.username}</p>
            <p><strong>Email:</strong> {user.email}</p>
            <p><strong>Display Name:</strong> {user.display_name || '-'}</p>
          </div>
          <button className="submit" onClick={handleLogout} style={{ marginTop: '1rem' }}>
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

import { useState, useEffect } from 'react';
import { getAuthMethods, getEnabledOAuthProviders, startOAuthAuthorization, unlinkOAuthProvider } from '../api';

const PROVIDER_LABELS = {
  PASSKEY: 'Passkey',
  GOOGLE: 'Google',
  GITHUB: 'GitHub',
  FACEBOOK: 'Facebook',
  KEYCLOAK: 'Keycloak'
};

export default function AccountLinking() {
  const [methods, setMethods] = useState([]);
  const [enabledProviders, setEnabledProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [methodsData, providersData] = await Promise.all([
        getAuthMethods(),
        getEnabledOAuthProviders()
      ]);
      setMethods(methodsData);
      setEnabledProviders(providersData.providers || []);
    } catch (e) {
      setError('Failed to load auth methods: ' + e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleLink(provider) {
    setError('');
    try {
      const data = await startOAuthAuthorization(provider);
      window.location.href = data.authorization_url;
    } catch (e) {
      setError('Failed to start linking: ' + e.message);
    }
  }

  async function handleUnlink(provider) {
    setError('');
    setSuccess('');
    try {
      await unlinkOAuthProvider(provider.toLowerCase());
      setSuccess('Unlinked ' + PROVIDER_LABELS[provider]);
      await loadData();
    } catch (e) {
      setError('Failed to unlink: ' + e.message);
    }
  }

  if (loading) return <div>Loading...</div>;

  const linkedProviders = methods.map(m => m.provider);

  return (
    <div className="account-linking">
      <h3>Authentication Methods</h3>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="linked-methods">
        <h4>Linked Methods</h4>
        {methods.length === 0 ? (
          <p>No authentication methods linked</p>
        ) : (
          <ul className="method-list">
            {methods.map(method => (
              <li key={method.id} className="method-item">
                <div className="method-info">
                  <strong>{PROVIDER_LABELS[method.provider] || method.provider}</strong>
                  {method.provider_email && <span> ({method.provider_email})</span>}
                  {method.is_primary && <span className="badge-primary">Primary</span>}
                </div>
                <div className="method-actions">
                  {method.provider !== 'PASSKEY' && (
                    <button
                      className="btn-small btn-danger"
                      onClick={() => handleUnlink(method.provider)}
                    >
                      Unlink
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {enabledProviders.length > 0 && (
        <div className="available-providers">
          <h4>Link More Providers</h4>
          <div className="provider-buttons">
            {enabledProviders
              .filter(p => !linkedProviders.includes(p.toUpperCase()))
              .map(provider => (
                <button
                  key={provider}
                  className={`btn-oauth btn-oauth-${provider}`}
                  onClick={() => handleLink(provider)}
                >
                  Link {PROVIDER_LABELS[provider.toUpperCase()] || provider}
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}

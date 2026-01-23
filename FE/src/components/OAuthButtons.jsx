import { useState, useEffect } from 'react';
import { getEnabledOAuthProviders, startOAuthAuthorization } from '../api';

const PROVIDER_LABELS = {
  google: 'Google',
  github: 'GitHub',
  facebook: 'Facebook',
  keycloak: 'Keycloak'
};

export default function OAuthButtons({ onSuccess }) {
  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    loadProviders();
  }, []);

  async function loadProviders() {
    try {
      const data = await getEnabledOAuthProviders();
      setProviders(data.providers || []);
    } catch (e) {
      // OAuth not configured - silently ignore
    }
  }

  async function handleOAuthLogin(provider) {
    setLoading(provider);
    setError('');
    try {
      const data = await startOAuthAuthorization(provider);
      // Redirect to OAuth provider
      window.location.href = data.authorization_url;
    } catch (e) {
      setError('Failed to start ' + PROVIDER_LABELS[provider] + ' login: ' + e.message);
      setLoading(null);
    }
  }

  if (providers.length === 0) return null;

  return (
    <div className="oauth-buttons">
      <div className="oauth-divider">
        <span>or continue with</span>
      </div>
      {error && <div className="error-message">{error}</div>}
      <div className="oauth-provider-list">
        {providers.map(provider => (
          <button
            key={provider}
            className={`btn-oauth btn-oauth-${provider}`}
            onClick={() => handleOAuthLogin(provider)}
            disabled={loading !== null}
          >
            {loading === provider ? 'Redirecting...' : PROVIDER_LABELS[provider] || provider}
          </button>
        ))}
      </div>
    </div>
  );
}

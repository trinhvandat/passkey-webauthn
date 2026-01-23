import { useEffect, useState } from 'react';
import { handleOAuthCallback, setTokens, setUserId } from '../api';

export default function OAuthCallback({ provider, onSuccess, onError }) {
  const [status, setStatus] = useState('Processing...');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    const error = params.get('error');

    if (error) {
      setStatus('OAuth login cancelled or failed: ' + error);
      if (onError) onError(error);
      return;
    }

    if (!code || !state) {
      setStatus('Invalid OAuth callback parameters');
      if (onError) onError('Missing code or state');
      return;
    }

    processCallback(code, state);
  }, []);

  async function processCallback(code, state) {
    try {
      setStatus('Completing login...');
      const result = await handleOAuthCallback(provider, code, state);

      if (result.access_token) {
        setTokens(result.access_token, result.refresh_token);
        setUserId(result.user_id);
      }

      setStatus('Login successful! Redirecting...');
      if (onSuccess) onSuccess(result);
    } catch (e) {
      setStatus('Login failed: ' + e.message);
      if (onError) onError(e.message);
    }
  }

  return (
    <div className="oauth-callback">
      <h2>OAuth Login</h2>
      <p>{status}</p>
    </div>
  );
}

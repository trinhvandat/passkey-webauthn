// API utilities for WebAuthn application

const API_BASE = '/api/v1';

// Token management
let accessToken = null;
let refreshToken = null;

export function setTokens(access, refresh) {
  accessToken = access;
  refreshToken = refresh;
  if (access) {
    localStorage.setItem('accessToken', access);
  }
  if (refresh) {
    localStorage.setItem('refreshToken', refresh);
  }
}

export function getTokens() {
  if (!accessToken) {
    accessToken = localStorage.getItem('accessToken');
  }
  if (!refreshToken) {
    refreshToken = localStorage.getItem('refreshToken');
  }
  return { accessToken, refreshToken };
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userId');
}

export function getUserId() {
  return localStorage.getItem('userId');
}

export function setUserId(userId) {
  localStorage.setItem('userId', userId);
}

// Authenticated fetch helper
// Note: userId is extracted from JWT token on backend, no need to send X-User-Id header
async function authFetch(url, options = {}) {
  const { accessToken } = getTokens();

  if (!accessToken) {
    throw new Error('Not authenticated');
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
    ...options.headers,
  };

  const response = await fetch(url, { ...options, headers });

  // Handle 401 - try to refresh token
  if (response.status === 401 && refreshToken) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      headers['Authorization'] = `Bearer ${accessToken}`;
      return fetch(url, { ...options, headers });
    }
  }

  return response;
}

async function tryRefreshToken() {
  const { refreshToken: token } = getTokens();
  if (!token) return false;

  try {
    const response = await fetch(`${API_BASE}/sessions/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: token })
    });

    if (response.ok) {
      const result = await response.json();
      setTokens(result.data.access_token, result.data.refresh_token || token);
      return true;
    }
  } catch (e) {
    console.error('Token refresh failed:', e);
  }

  clearTokens();
  return false;
}

// Passkey Management API
export async function listPasskeys() {
  const response = await authFetch(`${API_BASE}/passkeys`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to list passkeys');
  }
  const result = await response.json();
  return result.data;
}

export async function renamePasskey(credentialId, newName) {
  const response = await authFetch(`${API_BASE}/passkeys/${encodeURIComponent(credentialId)}`, {
    method: 'PATCH',
    body: JSON.stringify({ name: newName })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to rename passkey');
  }
  const result = await response.json();
  return result.data;
}

export async function deletePasskey(credentialId) {
  const response = await authFetch(`${API_BASE}/passkeys/${encodeURIComponent(credentialId)}`, {
    method: 'DELETE'
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to delete passkey');
  }
}

export async function startAddPasskey(deviceName) {
  const response = await authFetch(`${API_BASE}/passkeys/add:start`, {
    method: 'POST',
    body: JSON.stringify({ device_name: deviceName })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to start adding passkey');
  }
  const result = await response.json();
  return result.data;
}

export async function completeAddPasskey(deviceName, clientDataJSON, attestationObject, transports) {
  const response = await authFetch(`${API_BASE}/passkeys/add:complete`, {
    method: 'POST',
    body: JSON.stringify({
      device_name: deviceName,
      clientDataJSON,
      attestationObject,
      transports
    })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to complete adding passkey');
  }
  const result = await response.json();
  return result.data;
}

// Recovery Codes API
export async function generateRecoveryCodes() {
  const response = await authFetch(`${API_BASE}/recovery/codes/generate`, {
    method: 'POST'
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to generate recovery codes');
  }
  const result = await response.json();
  return result.data;
}

export async function getRecoveryCodeStatus() {
  const response = await authFetch(`${API_BASE}/recovery/codes/status`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get recovery code status');
  }
  const result = await response.json();
  return result.data;
}

export async function useRecoveryCode(username, code) {
  const response = await fetch(`${API_BASE}/recovery/codes/use`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, code })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Invalid recovery code');
  }
  const result = await response.json();
  return result.data;
}

// Session Management API
export async function listSessions() {
  const response = await authFetch(`${API_BASE}/sessions`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to list sessions');
  }
  const result = await response.json();
  return result.data;
}

export async function revokeSession(sessionId) {
  const response = await authFetch(`${API_BASE}/sessions/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE'
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to revoke session');
  }
}

export async function revokeOtherSessions(currentSessionId) {
  const response = await authFetch(`${API_BASE}/sessions/revoke-others`, {
    method: 'POST',
    headers: {
      'X-Session-Id': currentSessionId
    }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to revoke other sessions');
  }
  const result = await response.json();
  return result.data;
}

export async function revokeAllSessions() {
  const response = await authFetch(`${API_BASE}/sessions/revoke-all`, {
    method: 'POST'
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to revoke all sessions');
  }
  const result = await response.json();
  return result.data;
}

// Security API
export async function lockAccount(reason) {
  const response = await authFetch(`${API_BASE}/security/account:lock`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to lock account');
  }
  const result = await response.json();
  return result.data;
}

export async function unlockAccount(username, recoveryCode) {
  const response = await fetch(`${API_BASE}/security/account:unlock`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, recovery_code: recoveryCode })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to unlock account');
  }
  const result = await response.json();
  return result.data;
}

export async function getAuthLogs(page = 0, size = 20, success = null) {
  let url = `${API_BASE}/security/auth-logs?page=${page}&size=${size}`;
  if (success !== null) {
    url += `&success=${success}`;
  }
  const response = await authFetch(url);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get auth logs');
  }
  const result = await response.json();
  return result.data;
}

// Role Management API (Admin)
export async function getMyRoles() {
  const response = await authFetch(`${API_BASE}/admin/roles/me`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get roles');
  }
  const result = await response.json();
  return result.data;
}

export async function listAllRoles() {
  const response = await authFetch(`${API_BASE}/admin/roles`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to list roles');
  }
  const result = await response.json();
  return result.data;
}

export async function getUserRoles(userId) {
  const response = await authFetch(`${API_BASE}/admin/roles/users/${encodeURIComponent(userId)}`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get user roles');
  }
  const result = await response.json();
  return result.data;
}

export async function assignRole(userId, roleId) {
  const response = await authFetch(`${API_BASE}/admin/roles/users/${encodeURIComponent(userId)}`, {
    method: 'POST',
    body: JSON.stringify({ role_id: roleId })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to assign role');
  }
}

export async function revokeRole(userId, roleId, reason) {
  const response = await authFetch(`${API_BASE}/admin/roles/users/${encodeURIComponent(userId)}/roles/${roleId}`, {
    method: 'DELETE',
    body: JSON.stringify({ reason })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to revoke role');
  }
}

// OAuth API
export async function getEnabledOAuthProviders() {
  const response = await fetch(`${API_BASE}/oauth/providers`);
  if (!response.ok) return { providers: [] };
  const result = await response.json();
  return result.data;
}

export async function startOAuthAuthorization(provider) {
  const { accessToken: token } = getTokens();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${API_BASE}/oauth/${provider}/authorize`, {
    method: 'POST',
    headers
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to start OAuth');
  }
  const result = await response.json();
  return result.data;
}

export async function handleOAuthCallback(provider, code, state) {
  const response = await fetch(`${API_BASE}/oauth/${provider}/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, state })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'OAuth login failed');
  }
  const result = await response.json();
  return result.data;
}

export async function getAuthMethods() {
  const response = await authFetch(`${API_BASE}/oauth/methods`);
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get auth methods');
  }
  const result = await response.json();
  return result.data;
}

export async function unlinkOAuthProvider(provider) {
  const response = await authFetch(`${API_BASE}/oauth/${provider}/unlink`, {
    method: 'DELETE'
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to unlink provider');
  }
}

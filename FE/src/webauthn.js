const API_BASE = '/api/v1/auth';

function bufferToBase64Url(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function base64UrlToBuffer(base64url) {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const padding = '='.repeat((4 - base64.length % 4) % 4);
  const binary = atob(base64 + padding);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

export async function startRegistration(username, displayName, email) {
  const response = await fetch(`${API_BASE}/register:start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, displayName, email })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to start registration');
  }

  const result = await response.json();
  return result.data;
}

export async function completeRegistration(username, displayName, email, options) {
  // Handle both camelCase and snake_case responses
  const pubKeyCredParams = options.pubKeyCredParams || options.pub_key_cred_params;
  const authenticatorSelection = options.authenticatorSelection || options.authenticator_selection;
  const excludeCredentials = options.excludeCredentials || options.exclude_credentials;

  const publicKeyCredentialCreationOptions = {
    challenge: base64UrlToBuffer(options.challenge),
    rp: {
      name: options.rp.name,
      id: options.rp.id
    },
    user: {
      id: new TextEncoder().encode(options.user.id),
      name: options.user.name || options.user.username || username,
      displayName: options.user.displayName || options.user.display_name || displayName
    },
    pubKeyCredParams: pubKeyCredParams.map(param => ({
      type: param.type,
      alg: param.alg
    })),
    timeout: options.timeout,
    attestation: options.attestation || 'none'
  };

  // Only add authenticatorSelection if it has valid values
  if (authenticatorSelection) {
    const authAttachment = authenticatorSelection.authenticatorAttachment || authenticatorSelection.authenticator_attachment;
    publicKeyCredentialCreationOptions.authenticatorSelection = {
      residentKey: authenticatorSelection.residentKey || authenticatorSelection.resident_key || 'preferred',
      userVerification: authenticatorSelection.userVerification || authenticatorSelection.user_verification || 'required'
    };
    // Only add authenticatorAttachment if it's not empty
    if (authAttachment && authAttachment !== '') {
      publicKeyCredentialCreationOptions.authenticatorSelection.authenticatorAttachment = authAttachment;
    }
  }

  if (excludeCredentials && excludeCredentials.length > 0) {
    publicKeyCredentialCreationOptions.excludeCredentials = excludeCredentials.map(cred => ({
      type: cred.type,
      id: base64UrlToBuffer(cred.id),
      transports: cred.transports
    }));
  }

  const credential = await navigator.credentials.create({
    publicKey: publicKeyCredentialCreationOptions
  });

  const credentialResponse = {
    username,
    displayName,
    email,
    credentialId: bufferToBase64Url(credential.rawId),
    rawId: bufferToBase64Url(credential.rawId),
    clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
    attestationObject: bufferToBase64Url(credential.response.attestationObject),
    transports: credential.response.getTransports ? credential.response.getTransports() : []
  };

  const response = await fetch(`${API_BASE}/register:complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentialResponse)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to complete registration');
  }

  const result = await response.json();
  return result.data;
}

export async function startAuthentication(username) {
  const response = await fetch(`${API_BASE}/authenticate:start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to start authentication');
  }

  const result = await response.json();
  return result.data;
}

export async function completeAuthentication(options) {
  const publicKeyCredentialRequestOptions = {
    challenge: base64UrlToBuffer(options.challenge),
    timeout: options.timeout,
    rpId: options.rp_id,
    userVerification: options.user_verification || 'required'
  };

  if (options.allow_credentials && options.allow_credentials.length > 0) {
    publicKeyCredentialRequestOptions.allowCredentials = options.allow_credentials.map(cred => ({
      type: cred.type,
      id: base64UrlToBuffer(cred.id),
      transports: cred.transports
    }));
  }

  const assertion = await navigator.credentials.get({
    publicKey: publicKeyCredentialRequestOptions
  });

  const assertionResponse = {
    credentialId: bufferToBase64Url(assertion.rawId),
    rawId: bufferToBase64Url(assertion.rawId),
    clientDataJSON: bufferToBase64Url(assertion.response.clientDataJSON),
    authenticatorData: bufferToBase64Url(assertion.response.authenticatorData),
    signature: bufferToBase64Url(assertion.response.signature),
    userHandle: assertion.response.userHandle
      ? bufferToBase64Url(assertion.response.userHandle)
      : null
  };

  const response = await fetch(`${API_BASE}/authenticate:complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(assertionResponse)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to complete authentication');
  }

  const result = await response.json();
  return result.data;
}

export function isWebAuthnSupported() {
  return window.PublicKeyCredential !== undefined;
}

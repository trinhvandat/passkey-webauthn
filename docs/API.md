# API Reference

## Base URL

```
http://localhost:8080/api/v1
```

## Response Format

All responses follow this structure:

```json
{
  "data": { ... },
  "status": 200
}
```

Error responses:

```json
{
  "code": "ERR_000001",
  "message": "Error description",
  "params": null,
  "timestamp": "2024-01-22T10:30:00.000000"
}
```

---

## Authentication Endpoints

### 1. Start Registration

Initiates passkey registration for a new user.

```
POST /api/v1/auth/register:start
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | Yes | Unique username |
| `displayName` | string | No | Display name for UI |
| `email` | string | Yes | User email (unique) |

```json
{
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "challenge": "abc123def456...",
    "rp": {
      "id": "localhost",
      "name": "WebAuthn Demo"
    },
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "john_doe",
      "displayName": "John Doe"
    },
    "pubKeyCredParams": [
      { "type": "public-key", "alg": -7 },
      { "type": "public-key", "alg": -257 },
      { "type": "public-key", "alg": -8 }
    ],
    "timeout": 60000,
    "authenticatorSelection": {
      "residentKey": "preferred",
      "userVerification": "required"
    },
    "attestation": "none"
  },
  "status": 200
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ERR_000002 | Invalid request (missing fields) |
| 409 | ERR_000003 | Email already registered |

---

### 2. Complete Registration

Completes passkey registration with attestation data.

```
POST /api/v1/auth/register:complete
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | Yes | Username from start |
| `displayName` | string | No | Display name |
| `email` | string | Yes | Email from start |
| `clientDataJSON` | string | Yes | Base64URL-encoded client data |
| `attestationObject` | string | Yes | Base64URL-encoded attestation |
| `transports` | string[] | No | Transport types |

```json
{
  "username": "john_doe",
  "displayName": "John Doe",
  "email": "john@example.com",
  "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIi...",
  "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRo...",
  "transports": ["internal", "hybrid"]
}
```

#### Response (201 Created)

```json
{
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "display_name": "John Doe"
  },
  "status": 200
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ERR_000002 | Invalid request |
| 400 | ERR_000005 | Challenge expired |
| 400 | ERR_000006 | Challenge already used |
| 400 | ERR_000007 | Credential verification failed |
| 404 | ERR_000004 | Challenge not found |

---

### 3. Start Authentication

Initiates passkey authentication for existing user.

```
POST /api/v1/auth/authenticate:start
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | Yes | Username or email |

```json
{
  "username": "john_doe"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "challenge": "xyz789abc123...",
    "timeout": 60000,
    "rpId": "localhost",
    "userVerification": "required",
    "allowCredentials": [
      {
        "type": "public-key",
        "id": "credential-id-base64url",
        "transports": ["internal", "hybrid"]
      }
    ]
  },
  "status": 200
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ERR_000002 | Invalid request |
| 404 | ERR_000008 | User not found |

---

### 4. Complete Authentication

Completes passkey authentication with assertion data.

```
POST /api/v1/auth/authenticate:complete
```

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `credentialId` | string | Yes | Base64URL credential ID |
| `clientDataJSON` | string | Yes | Base64URL client data |
| `authenticatorData` | string | Yes | Base64URL authenticator data |
| `signature` | string | Yes | Base64URL signature |
| `userHandle` | string | No | Base64URL user handle |

```json
{
  "credentialId": "abc123...",
  "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0Ii...",
  "authenticatorData": "SZYN5YgOjGh0NBcPZHZgW4/krrmihjLHmVzzuoMdl2M...",
  "signature": "MEUCIQDpe...",
  "userHandle": "550e8400..."
}
```

#### Response (200 OK)

```json
{
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "display_name": "John Doe",
    "verified": true
  },
  "status": 200
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ERR_000002 | Invalid request |
| 400 | ERR_000005 | Challenge expired |
| 400 | ERR_000006 | Challenge already used |
| 400 | ERR_000007 | Credential verification failed |
| 400 | ERR_000010 | Sign count invalid (possible cloned authenticator) |
| 404 | ERR_000004 | Challenge not found |
| 404 | ERR_000009 | Credential not found |

---

## Error Codes Reference

| Code | HTTP Status | Message |
|------|-------------|---------|
| ERR_000001 | 404 | Not found |
| ERR_000002 | 400 | Invalid request |
| ERR_000003 | 409 | Email already registered |
| ERR_000004 | 404 | Challenge not found |
| ERR_000005 | 400 | Challenge expired |
| ERR_000006 | 400 | Challenge already used |
| ERR_000007 | 400 | Credential verification failed |
| ERR_000008 | 404 | User not found |
| ERR_000009 | 404 | Credential not found |
| ERR_000010 | 400 | Sign count invalid |
| ERR_999999 | 500 | An unexpected error occurred |

---

## WebAuthn Algorithm IDs

| ID | Algorithm | Description |
|----|-----------|-------------|
| -7 | ES256 | ECDSA with P-256 curve and SHA-256 |
| -257 | RS256 | RSASSA-PKCS1-v1_5 with SHA-256 |
| -8 | EdDSA | Edwards-curve Digital Signature Algorithm |

---

## Usage Examples

### cURL Examples

#### Register a new user

```bash
# Step 1: Start registration
curl -X POST http://localhost:8080/api/v1/auth/register:start \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "displayName": "Alice Smith",
    "email": "alice@example.com"
  }'

# Step 2: Complete registration (with data from WebAuthn API)
curl -X POST http://localhost:8080/api/v1/auth/register:complete \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "displayName": "Alice Smith",
    "email": "alice@example.com",
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIi...",
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRo...",
    "transports": ["internal"]
  }'
```

#### Authenticate existing user

```bash
# Step 1: Start authentication
curl -X POST http://localhost:8080/api/v1/auth/authenticate:start \
  -H "Content-Type: application/json" \
  -d '{"username": "alice"}'

# Step 2: Complete authentication (with data from WebAuthn API)
curl -X POST http://localhost:8080/api/v1/auth/authenticate:complete \
  -H "Content-Type: application/json" \
  -d '{
    "credentialId": "abc123...",
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uZ2V0Ii...",
    "authenticatorData": "SZYN5YgOjGh0NBcPZHZgW4...",
    "signature": "MEUCIQDpe..."
  }'
```

### JavaScript Example

```javascript
// Registration
async function register(username, displayName, email) {
  // Start registration
  const startRes = await fetch('/api/v1/auth/register:start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, displayName, email })
  });
  const { data: options } = await startRes.json();

  // Create credential
  const credential = await navigator.credentials.create({
    publicKey: {
      challenge: base64UrlToBuffer(options.challenge),
      rp: options.rp,
      user: {
        id: new TextEncoder().encode(options.user.id),
        name: options.user.name,
        displayName: options.user.displayName
      },
      pubKeyCredParams: options.pubKeyCredParams,
      timeout: options.timeout,
      attestation: options.attestation
    }
  });

  // Complete registration
  const completeRes = await fetch('/api/v1/auth/register:complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username,
      displayName,
      email,
      clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
      attestationObject: bufferToBase64Url(credential.response.attestationObject),
      transports: credential.response.getTransports?.() || []
    })
  });

  return completeRes.json();
}

// Authentication
async function authenticate(username) {
  // Start authentication
  const startRes = await fetch('/api/v1/auth/authenticate:start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username })
  });
  const { data: options } = await startRes.json();

  // Get assertion
  const assertion = await navigator.credentials.get({
    publicKey: {
      challenge: base64UrlToBuffer(options.challenge),
      timeout: options.timeout,
      rpId: options.rpId,
      allowCredentials: options.allowCredentials?.map(c => ({
        type: c.type,
        id: base64UrlToBuffer(c.id),
        transports: c.transports
      })),
      userVerification: options.userVerification
    }
  });

  // Complete authentication
  const completeRes = await fetch('/api/v1/auth/authenticate:complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      credentialId: bufferToBase64Url(assertion.rawId),
      clientDataJSON: bufferToBase64Url(assertion.response.clientDataJSON),
      authenticatorData: bufferToBase64Url(assertion.response.authenticatorData),
      signature: bufferToBase64Url(assertion.response.signature),
      userHandle: assertion.response.userHandle
        ? bufferToBase64Url(assertion.response.userHandle)
        : null
    })
  });

  return completeRes.json();
}

// Helper functions
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
```

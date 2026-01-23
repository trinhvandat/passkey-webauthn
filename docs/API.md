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
    "verified": true,
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "expires_in": 3600
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
| 403 | ERR_000014 | User account inactive |
| 404 | ERR_000004 | Challenge not found |
| 404 | ERR_000009 | Credential not found |
| 429 | ERR_000020 | Too many attempts - rate limited |

---

## Passkey Management Endpoints

> **Note:** All passkey management endpoints require authentication via `Authorization: Bearer <access_token>` header.

### 5. List User Passkeys

Get all passkeys registered for the authenticated user.

```
GET /api/v1/passkeys
```

#### Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | Bearer token |

#### Response (200 OK)

```json
{
  "data": {
    "passkeys": [
      {
        "id": "cred-uuid-1",
        "device_name": "iPhone 15 Pro",
        "device_type": "platform",
        "created_at": "2024-01-15T10:30:00Z",
        "last_used_at": "2024-01-22T08:15:00Z",
        "backup_eligible": true,
        "backup_state": true,
        "transports": ["internal", "hybrid"]
      },
      {
        "id": "cred-uuid-2",
        "device_name": "YubiKey 5",
        "device_type": "cross-platform",
        "created_at": "2024-01-20T14:00:00Z",
        "last_used_at": "2024-01-21T09:00:00Z",
        "backup_eligible": false,
        "backup_state": false,
        "transports": ["usb"]
      }
    ],
    "total": 2
  },
  "status": 200
}
```

---

### 6. Get Passkey Details

Get details of a specific passkey.

```
GET /api/v1/passkeys/{credentialId}
```

#### Response (200 OK)

```json
{
  "data": {
    "id": "cred-uuid-1",
    "device_name": "iPhone 15 Pro",
    "device_type": "platform",
    "algorithm": "ES256",
    "sign_count": 42,
    "created_at": "2024-01-15T10:30:00Z",
    "last_used_at": "2024-01-22T08:15:00Z",
    "backup_eligible": true,
    "backup_state": true,
    "transports": ["internal", "hybrid"],
    "attestation_format": "none"
  },
  "status": 200
}
```

---

### 7. Update Passkey (Rename)

Update passkey device name.

```
PUT /api/v1/passkeys/{credentialId}
```

#### Request Body

```json
{
  "device_name": "My Work iPhone"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "id": "cred-uuid-1",
    "device_name": "My Work iPhone",
    "updated_at": "2024-01-22T10:00:00Z"
  },
  "status": 200
}
```

---

### 8. Delete Passkey (Revoke)

Revoke a specific passkey. Cannot delete last passkey unless recovery codes exist.

```
DELETE /api/v1/passkeys/{credentialId}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Passkey revoked successfully",
    "remaining_passkeys": 1
  },
  "status": 200
}
```

#### Error Responses

| Status | Code | Description |
|--------|------|-------------|
| 400 | ERR_000021 | Cannot delete last passkey without recovery codes |
| 404 | ERR_000009 | Credential not found |

---

### 9. Add New Passkey (Start)

Start adding a new passkey to existing account.

```
POST /api/v1/passkeys:add-start
```

#### Request Body

```json
{
  "device_name": "New MacBook Pro"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "challenge": "xyz789...",
    "rp": {
      "id": "localhost",
      "name": "WebAuthn Demo"
    },
    "user": {
      "id": "user-uuid",
      "name": "john_doe",
      "displayName": "John Doe"
    },
    "pubKeyCredParams": [...],
    "excludeCredentials": [
      {
        "type": "public-key",
        "id": "existing-cred-id",
        "transports": ["internal"]
      }
    ],
    "timeout": 60000
  },
  "status": 200
}
```

---

### 10. Add New Passkey (Complete)

Complete adding new passkey.

```
POST /api/v1/passkeys:add-complete
```

#### Request Body

```json
{
  "device_name": "New MacBook Pro",
  "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIi...",
  "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRo...",
  "transports": ["internal", "hybrid"]
}
```

#### Response (201 Created)

```json
{
  "data": {
    "id": "new-cred-uuid",
    "device_name": "New MacBook Pro",
    "created_at": "2024-01-22T10:00:00Z"
  },
  "status": 200
}
```

---

### 11. Revoke All Passkeys

Emergency revoke all passkeys (requires recovery code or re-authentication).

```
POST /api/v1/passkeys:revoke-all
```

#### Request Body

```json
{
  "recovery_code": "ABCD-1234"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "All passkeys revoked",
    "revoked_count": 3
  },
  "status": 200
}
```

---

## Recovery Endpoints

### 12. Generate Recovery Codes

Generate new recovery codes (invalidates previous codes).

```
POST /api/v1/recovery/codes:generate
```

#### Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | Bearer token |

#### Response (200 OK)

```json
{
  "data": {
    "codes": [
      "ABCD-1234",
      "EFGH-5678",
      "IJKL-9012",
      "MNOP-3456",
      "QRST-7890",
      "UVWX-1234",
      "YZAB-5678",
      "CDEF-9012"
    ],
    "generated_at": "2024-01-22T10:00:00Z",
    "warning": "Save these codes securely. They will not be shown again."
  },
  "status": 200
}
```

---

### 13. Get Recovery Codes Status

Check remaining unused recovery codes.

```
GET /api/v1/recovery/codes:status
```

#### Response (200 OK)

```json
{
  "data": {
    "total_codes": 8,
    "used_codes": 2,
    "remaining_codes": 6,
    "generated_at": "2024-01-22T10:00:00Z"
  },
  "status": 200
}
```

---

### 14. Initiate Account Recovery

Start account recovery process (when all passkeys are lost).

```
POST /api/v1/recovery:initiate
```

#### Request Body

```json
{
  "email": "john@example.com"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Recovery instructions sent to email",
    "recovery_token_expires_in": 3600
  },
  "status": 200
}
```

---

### 15. Complete Account Recovery

Complete recovery using recovery code.

```
POST /api/v1/recovery:complete
```

#### Request Body

```json
{
  "email": "john@example.com",
  "recovery_code": "ABCD-1234",
  "recovery_token": "token-from-email"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Account recovered. Please register a new passkey.",
    "registration_challenge": "xyz789...",
    "rp": {...},
    "user": {...},
    "timeout": 300000
  },
  "status": 200
}
```

---

## Session Management Endpoints

### 16. List Active Sessions

Get all active sessions for user.

```
GET /api/v1/sessions
```

#### Response (200 OK)

```json
{
  "data": {
    "sessions": [
      {
        "id": "session-uuid-1",
        "device_info": "Chrome on MacOS",
        "ip_address": "192.168.1.100",
        "created_at": "2024-01-22T08:00:00Z",
        "last_activity_at": "2024-01-22T10:30:00Z",
        "is_current": true
      },
      {
        "id": "session-uuid-2",
        "device_info": "Safari on iPhone",
        "ip_address": "10.0.0.50",
        "created_at": "2024-01-21T14:00:00Z",
        "last_activity_at": "2024-01-21T18:00:00Z",
        "is_current": false
      }
    ],
    "total": 2
  },
  "status": 200
}
```

---

### 17. Revoke Session

Revoke a specific session.

```
DELETE /api/v1/sessions/{sessionId}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Session revoked successfully"
  },
  "status": 200
}
```

---

### 18. Revoke All Other Sessions

Logout from all other devices.

```
POST /api/v1/sessions:revoke-others
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "All other sessions revoked",
    "revoked_count": 3
  },
  "status": 200
}
```

---

### 19. Refresh Token

Get new access token using refresh token.

```
POST /api/v1/sessions:refresh
```

#### Request Body

```json
{
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

#### Response (200 OK)

```json
{
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 3600
  },
  "status": 200
}
```

---

## Security Endpoints

### 20. Lock Account

Immediately lock account (emergency).

```
POST /api/v1/security/account:lock
```

#### Request Body

```json
{
  "reason": "Suspected compromise"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Account locked successfully",
    "locked_at": "2024-01-22T10:00:00Z"
  },
  "status": 200
}
```

---

### 21. Unlock Account

Unlock account (requires recovery code or email verification).

```
POST /api/v1/security/account:unlock
```

#### Request Body

```json
{
  "recovery_code": "ABCD-1234"
}
```

#### Response (200 OK)

```json
{
  "data": {
    "message": "Account unlocked successfully"
  },
  "status": 200
}
```

---

### 22. Get Authentication Logs

Get user's authentication history.

```
GET /api/v1/security/auth-logs
```

#### Query Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20, max: 100) |
| `success` | boolean | Filter by success/failure |

#### Response (200 OK)

```json
{
  "data": {
    "logs": [
      {
        "id": 123,
        "operation_type": "AUTHENTICATION",
        "success": true,
        "ip_address": "192.168.1.100",
        "user_agent": "Chrome/120.0...",
        "country_code": "VN",
        "city": "Ho Chi Minh City",
        "created_at": "2024-01-22T10:30:00Z"
      }
    ],
    "total": 50,
    "page": 0,
    "size": 20
  },
  "status": 200
}
```

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
| ERR_000011 | 409 | Username already registered |
| ERR_000012 | 409 | Credential already registered |
| ERR_000013 | 400 | Challenge operation type mismatch |
| ERR_000014 | 403 | User account inactive |
| ERR_000015 | 400 | Credential-user mismatch |
| ERR_000020 | 429 | Rate limit exceeded |
| ERR_000021 | 400 | Cannot delete last passkey |
| ERR_000022 | 400 | Invalid recovery code |
| ERR_000023 | 400 | Recovery code expired |
| ERR_000024 | 400 | Recovery code already used |
| ERR_000025 | 403 | Account locked |
| ERR_000026 | 401 | Session expired |
| ERR_000027 | 401 | Invalid refresh token |
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

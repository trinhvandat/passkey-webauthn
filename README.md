# WebAuthn Passkey Authentication

A complete WebAuthn/Passkey authentication implementation using Spring Boot and React.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Security](#security)
- [Documentation](#documentation)

## Overview

This project implements passwordless authentication using the WebAuthn (Web Authentication) standard. Users can register and authenticate using biometrics (fingerprint, face recognition) or security keys instead of traditional passwords.

## Features

### Core Authentication
- **Passwordless Registration** - Register users with passkeys (biometrics/security keys)
- **Passwordless Authentication** - Login using registered passkeys with JWT session tokens
- **Multi-device Support** - Support for platform authenticators and roaming authenticators

### Security
- **Replay Attack Prevention** - One-time challenge usage with atomic validation
- **Cloned Authenticator Detection** - Sign count validation to detect credential cloning
- **Rate Limiting** - Brute force protection with progressive lockouts
- **Account Locking** - Automatic and manual account lock/unlock

### Passkey Management
- **Multi-Passkey Support** - Register multiple passkeys per account
- **Passkey Listing** - View all registered passkeys with details
- **Device Naming** - Rename passkeys for easy identification
- **Passkey Revocation** - Remove individual or all passkeys

### Account Recovery
- **Recovery Codes** - 8 one-time backup codes for account recovery
- **Email Recovery** - Recover account via email verification
- **Emergency Lock** - Self-lock account if compromised

### Session Management
- **JWT Tokens** - Access tokens (15min) + Refresh tokens (7 days)
- **Active Sessions** - View and manage all active sessions
- **Remote Logout** - Revoke sessions from other devices

### Audit & Monitoring
- **Authentication Logs** - Complete audit trail with IP and location
- **Security Alerts** - Sign count anomaly detection
- **Login Attempt Tracking** - Rate limiting data for analysis

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Spring Boot 4.0.1, Java 17 |
| WebAuthn Library | webauthn4j 0.28.5 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Frontend | React 18, Vite |
| Build Tool | Maven |

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 16+
- Docker (optional)

## Quick Start

### 1. Start Database

```bash
docker-compose up -d
```

### 2. Configure Environment

Create `.env` file or set environment variables:

```env
DB_HOST=localhost
DB_PORT=5432
DB_USER=web-authn
DB_PASSWORD=WebAuthn@1234
DB_NAME=web-authn
MIGRATION_ENABLED=true
```

### 3. Start Backend

```bash
export $(cat .env | grep -v '^#' | xargs)
./mvnw spring-boot:run -Dmaven.test.skip=true
```

Backend runs on http://localhost:8080

### 4. Start Frontend

```bash
cd FE
npm install
npm run dev
```

Frontend runs on http://localhost:3000

### 5. Test

1. Open http://localhost:3000
2. Fill in registration form (username, display name, email)
3. Click "Register with Passkey"
4. Complete biometric/security key verification
5. Login using your registered passkey

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register:start` | Start passkey registration |
| POST | `/api/v1/auth/register:complete` | Complete passkey registration |
| POST | `/api/v1/auth/authenticate:start` | Start passkey authentication |
| POST | `/api/v1/auth/authenticate:complete` | Complete passkey authentication |

### Passkey Management (Requires Auth)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/passkeys` | List all passkeys |
| GET | `/api/v1/passkeys/{id}` | Get passkey details |
| PUT | `/api/v1/passkeys/{id}` | Rename passkey |
| DELETE | `/api/v1/passkeys/{id}` | Revoke passkey |
| POST | `/api/v1/passkeys:add-start` | Start adding new passkey |
| POST | `/api/v1/passkeys:add-complete` | Complete adding passkey |
| POST | `/api/v1/passkeys:revoke-all` | Revoke all passkeys |

### Recovery
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/recovery/codes:generate` | Generate recovery codes |
| GET | `/api/v1/recovery/codes:status` | Check recovery codes status |
| POST | `/api/v1/recovery:initiate` | Start account recovery |
| POST | `/api/v1/recovery:complete` | Complete account recovery |

### Session Management (Requires Auth)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sessions` | List active sessions |
| DELETE | `/api/v1/sessions/{id}` | Revoke session |
| POST | `/api/v1/sessions:revoke-others` | Revoke all other sessions |
| POST | `/api/v1/sessions:refresh` | Refresh access token |

### Security (Requires Auth)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/security/account:lock` | Lock account |
| POST | `/api/v1/security/account:unlock` | Unlock account |
| GET | `/api/v1/security/auth-logs` | Get authentication history |

### Example: Start Registration

```bash
curl -X POST http://localhost:8080/api/v1/auth/register:start \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "displayName": "John Doe",
    "email": "john@example.com"
  }'
```

### Example Response

```json
{
  "data": {
    "challenge": "abc123...",
    "rp": { "id": "localhost", "name": "WebAuthn Demo" },
    "user": { "id": "uuid", "name": "john_doe", "displayName": "John Doe" },
    "pubKeyCredParams": [
      { "type": "public-key", "alg": -7 },
      { "type": "public-key", "alg": -257 }
    ],
    "timeout": 60000,
    "attestation": "none"
  },
  "status": 200
}
```

## Configuration

### application.yml

```yaml
web-authn:
  rp-name: WebAuthn Demo          # Relying Party name
  rp-id: localhost                # Relying Party ID (domain)
  origin: http://localhost:3000   # Allowed origin
  supported-algorithms: -7,-257,-8 # ES256, RS256, EdDSA
  timeout: 60000                  # Challenge timeout (ms)
  attestation: none               # Attestation mode
  resident-key: preferred         # Discoverable credential
  user-verification: required     # Require user verification
```

### Supported Algorithms

| Algorithm ID | Name | Description |
|--------------|------|-------------|
| -7 | ES256 | ECDSA with P-256 and SHA-256 |
| -257 | RS256 | RSASSA-PKCS1-v1_5 with SHA-256 |
| -8 | EdDSA | Edwards-curve Digital Signature |

## Architecture

The project follows **Clean Architecture** (Hexagonal Architecture) with feature-based modules.

```
src/main/java/com/leonard/web_authn/
├── feature/
│   ├── authentication/     # Auth business logic
│   │   ├── adapter/web/    # Controllers, DTOs
│   │   └── usecase/        # Use cases
│   ├── passkey/            # Passkey management
│   │   ├── adapter/        # Repositories
│   │   ├── config/         # WebAuthn config
│   │   ├── domain/         # Entities, value objects
│   │   └── usecase/        # Challenge generation
│   └── user/               # User management
│       ├── adapter/        # Repository
│       ├── domain/         # User entity
│       └── usecase/        # User operations
└── shared/                 # Cross-cutting concerns
    ├── configuration/      # Spring config
    ├── dto/                # Common DTOs
    ├── exception/          # Error handling
    └── utils/              # Utilities
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture documentation.

## Security

### WebAuthn Security Features

1. **Challenge-Response** - Cryptographic proof of authenticator possession
2. **Origin Validation** - Prevents phishing attacks
3. **User Verification** - Requires biometric/PIN verification
4. **Sign Count Validation** - Detects cloned authenticators
5. **One-time Challenges** - Prevents replay attacks
6. **Challenge Expiration** - 5-minute TTL for challenges

### Best Practices Implemented

- SecureRandom for challenge generation (32 bytes)
- HTTPS required in production (rpId must match domain)
- No password storage - only public keys stored
- Audit logging for all authentication attempts
- Atomic challenge validation (prevents race conditions)
- TOCTOU protection for user registration
- Input length validation (prevents DoS)
- BCrypt for recovery code hashing
- JWT with short-lived access tokens

### Rate Limiting Rules

| Condition | Action |
|-----------|--------|
| 5 failed attempts in 15 min (same IP) | Block IP for 15 minutes |
| 10 failed attempts in 1 hour (same user) | Lock account |
| 20 failed attempts in 1 hour (same IP) | Block IP for 24 hours |

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md) - Detailed system architecture
- [API Reference](docs/API.md) - Complete API documentation
- [Database Schema](docs/DATABASE.md) - Database design

## Development

### Build

```bash
./mvnw clean package -DskipTests
```

### Run Tests

```bash
./mvnw test
```

### Code Structure

| Layer | Responsibility |
|-------|----------------|
| Controller | HTTP handling, request/response mapping |
| Use Case | Business logic, orchestration |
| Domain | Entities, value objects, business rules |
| Repository | Data access |

## Troubleshooting

### Common Issues

1. **"WebAuthn not supported"** - Use a modern browser (Chrome, Firefox, Safari, Edge)
2. **"Challenge expired"** - Complete registration within 5 minutes
3. **"Origin mismatch"** - Ensure frontend origin matches `web-authn.origin` config
4. **"User verification failed"** - Enable biometrics on your device

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.leonard.web_authn: DEBUG
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

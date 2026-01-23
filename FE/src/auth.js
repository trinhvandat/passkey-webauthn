// JWT decode and role/permission utilities

export function decodeToken(token) {
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      userId: payload.sub,
      sessionId: payload.sessionId,
      roles: payload.roles || [],
      permissions: payload.permissions || [],
      exp: payload.exp
    };
  } catch (e) {
    console.error('Failed to decode token:', e);
    return null;
  }
}

export function hasPermission(token, permission) {
  const decoded = decodeToken(token);
  if (!decoded) return false;
  return decoded.permissions.includes(permission);
}

export function hasRole(token, role) {
  const decoded = decodeToken(token);
  if (!decoded) return false;
  return decoded.roles.includes(role);
}

export function isAdmin(token) {
  return hasRole(token, 'ADMIN');
}

export function hasAnyPermission(token, ...permissions) {
  const decoded = decodeToken(token);
  if (!decoded) return false;
  return permissions.some(p => decoded.permissions.includes(p));
}

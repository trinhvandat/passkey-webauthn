package com.leonard.web_authn.feature.authorization.usecase;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class PermissionCacheService {

    private static final long TTL_MS = 5 * 60 * 1000; // 5 minutes

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Set<String> getOrLoad(String userId, Function<String, Set<String>> loader) {
        CacheEntry entry = cache.get(userId);
        if (entry != null && !entry.isExpired()) {
            return entry.permissions();
        }
        Set<String> permissions = loader.apply(userId);
        cache.put(userId, new CacheEntry(permissions, System.currentTimeMillis() + TTL_MS));
        return permissions;
    }

    public void evict(String userId) {
        cache.remove(userId);
    }

    public void evictAll() {
        cache.clear();
    }

    private record CacheEntry(Set<String> permissions, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}

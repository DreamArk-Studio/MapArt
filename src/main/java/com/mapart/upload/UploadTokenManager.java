package com.mapart.upload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UploadTokenManager {

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public String createToken(UUID playerUuid, String playerName) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenInfo(playerUuid, playerName, System.currentTimeMillis()));
        return token;
    }

    public TokenInfo consumeToken(String token) {
        return tokens.remove(token);
    }

    public TokenInfo peekToken(String token) {
        return tokens.get(token);
    }

    public void cleanup() {
        long expiry = System.currentTimeMillis() - 5 * 60 * 1000;
        tokens.values().removeIf(info -> info.createdAt < expiry);
    }

    public static class TokenInfo {
        public final UUID playerUuid;
        public final String playerName;
        public final long createdAt;

        public TokenInfo(UUID playerUuid, String playerName, long createdAt) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.createdAt = createdAt;
        }
    }
}
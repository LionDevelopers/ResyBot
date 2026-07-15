package org.liondevelopers.resybot;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Small config helper: resolves values from process environment variables first,
 * then falls back to a local {@code .env} file (for local development), then to a
 * provided default. Centralizes the env/.env loading previously inlined in
 * DiscordNotification.
 */
public final class Config {

    private static final Dotenv DOTENV = loadDotenv();

    private Config() {
    }

    private static Dotenv loadDotenv() {
        try {
            return Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception e) {
            // .env not present / unreadable — env vars alone are fine.
            return null;
        }
    }

    public static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if ((value == null || value.isEmpty()) && DOTENV != null) {
            value = DOTENV.get(key);
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    public static String get(String key) {
        return get(key, null);
    }
}

package cat.clawdio.mods;

import android.content.Context;

import org.json.JSONObject;

/**
 * Настройки сборки: бренд, канал обновлений, автор, реквизиты. Их кладёт патчер из config.json
 * в ресурс {@code clawdio_config}, приложение читает их оттуда. Свой канал обновлений можно
 * указать в настройках — тогда сервер берётся оттуда, а не из сборки.
 */
final class Config {
    private static JSONObject json;

    private Config() {}

    static synchronized JSONObject json() {
        if (json == null) {
            json = new JSONObject();
            try {
                Context ctx = Mods.app();
                int id = Mods.res(ctx, "clawdio_config", "string");
                if (id != 0) {
                    json = new JSONObject(ctx.getString(id));
                }
            } catch (Throwable ignored) {
            }
        }
        return json;
    }

    static String str(String key, String def) {
        try {
            String v = json().optString(key, "");
            return v.isEmpty() ? def : v;
        } catch (Throwable t) {
            return def;
        }
    }

    /** Название сборки: «Clawdio». */
    static String brand() {
        return str("brand", "Clawdio");
    }

    /** Репозиторий с каналом обновлений: «owner/name». */
    static String repo() {
        return str("repo", "omnizs38/MargyC");
    }

    /** Ветка с файлами канала: version.json, catalog.json, APK. */
    static String branch() {
        return str("channel_branch", "Download");
    }

    /** Свой сервер обновлений из настроек или пусто. */
    static String customServer() {
        try {
            return Mods.prefs().getString("update_server", "").trim();
        } catch (Throwable t) {
            return "";
        }
    }

    /** Адрес канала: raw-файлы (version.json, catalog.json, .mcmod). */
    static String rawBase() {
        String custom = customServer();
        if (!custom.isEmpty()) {
            return trimSlash(custom);
        }
        String repo = repo();
        return repo.isEmpty() ? "" : "https://raw.githubusercontent.com/" + repo + "/" + branch() + "/";
    }

    /** Адрес канала для скачивания APK (как в браузере). */
    static String downloadBase() {
        String custom = customServer();
        if (!custom.isEmpty()) {
            return trimSlash(custom);
        }
        String repo = repo();
        return repo.isEmpty() ? "" : "https://github.com/" + repo + "/raw/" + branch() + "/";
    }

    /** API GitHub: запасной источник обновлений, если version.json недоступен. */
    static String apiBase() {
        String repo = repo();
        return repo.isEmpty() ? "" : "https://api.github.com/repos/" + repo;
    }

    /** Ссылка на исходники. */
    static String homepage() {
        return str("homepage", "https://github.com/omnizs38/MargyC");
    }

    /** Автор оригинальной версии: его имя и реквизиты остаются в приложении (см. NOTICE). */
    static String author() {
        return str("author", "narezany");
    }

    static String authorUrl() {
        return str("author_url", "https://t.me/narezany");
    }

    static String authorChannel() {
        return str("author_channel", "https://t.me/margyclaude");
    }

    static String authorCard() {
        return str("author_card", "");
    }

    static String authorYoomoney() {
        return str("author_yoomoney", "");
    }

    /** Кто ведёт эту сборку. */
    static String maintainer() {
        return str("maintainer", "");
    }

    static String maintainerUrl() {
        return str("maintainer_url", "");
    }

    /** Свои реквизиты для донатов: список словарей {title, url, text}. */
    static org.json.JSONArray donate() {
        try {
            org.json.JSONArray a = json().optJSONArray("maintainer_donate");
            return a == null ? new org.json.JSONArray() : a;
        } catch (Throwable t) {
            return new org.json.JSONArray();
        }
    }

    private static String trimSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}

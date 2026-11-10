package cat.clawdio.mods;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Проверка обновлений. Канал — своя ветка Download в репозитории: там лежат version.json с
 * последним релизом и последней бетой и сам APK. Если channel недоступен, берётся последний
 * релиз на GitHub. Адрес канала приходит из config.json, его можно поменять в настройках —
 * тогда обновления приходят с любого своего сервера.
 *
 * <pre>
 * {"release": {"version": "1.4", "beta": 0, "apk": "Clawdio.apk", "notes": "...", "sha256": "..."},
 *  "beta":    {"version": "1.5", "beta": 1, "apk": "Clawdio_beta.apk", "notes": "...", "sha256": "..."}}
 * </pre>
 */
final class Update {
    /** Сама проверяем не чаще, чем раз в столько. */
    private static final long PERIOD = 4 * 60 * 60 * 1000L;

    private static volatile Info available;
    private static boolean checking, shown;

    private Update() {}

    static final class Info {
        final String version, apk, notes, sha256;
        final int beta, code;

        Info(JSONObject o) {
            version = o.optString("version");
            beta = o.optInt("beta");
            apk = o.optString("apk");
            notes = o.optString("notes");
            sha256 = o.optString("sha256");
            code = Mods.code(version, beta);
        }

        String label() {
            return version + (beta == 0 ? "" : beta == 1 ? " beta" : " beta " + beta);
        }

        /** Полный адрес APK: в version.json имя файла, из релизов GitHub — прямая ссылка. */
        String url() {
            if (apk.startsWith("http://") || apk.startsWith("https://")) {
                return apk;
            }
            return Config.downloadBase() + apk;
        }
    }

    static boolean betas() {
        try {
            return Mods.prefs().getBoolean("update_beta", Mods.BETA > 0);
        } catch (Exception e) {
            return Mods.BETA > 0;
        }
    }

    static void setBetas(boolean on) throws Exception {
        Mods.prefs().edit().putBoolean("update_beta", on).putLong("update_checked", 0).apply();
        available = pick(Mods.prefs().getString("update_json", ""));
    }

    static Info available() {
        return available;
    }

    /** При открытии экранов Claude: проверка в фоне и, если есть новая версия, одно окно за запуск. */
    static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(final Activity a) {
                if (a.getClass().getName().startsWith("cat.clawdio.mods.")) {
                    return;
                }
                check(false, info -> offer(a, info));
            }

            @Override
            public void onActivityCreated(Activity a, Bundle b) {}

            @Override
            public void onActivityStarted(Activity a) {}

            @Override
            public void onActivityPaused(Activity a) {}

            @Override
            public void onActivityStopped(Activity a) {}

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}

            @Override
            public void onActivityDestroyed(Activity a) {}
        });
    }

    interface Done {
        /** В главном потоке; info == null — новее нет (или проверить не вышло, тогда error != null). */
        void done(Info info);
    }

    private static String error;

    static String error() {
        return error;
    }

    /** force — не смотреть на время прошлой проверки. */
    static void check(final boolean force, final Done done) {
        final Handler main = new Handler(Looper.getMainLooper());
        synchronized (Update.class) {
            long last = 0;
            try {
                last = Mods.prefs().getLong("update_checked", 0);
            } catch (Exception ignored) {
            }
            if (checking || (!force && System.currentTimeMillis() - last < PERIOD)) {
                if (!checking) {
                    if (available == null) {
                        try { // прошлый ответ сервера: после перезапуска окно снова покажется
                            available = pick(Mods.prefs().getString("update_json", ""));
                        } catch (Exception ignored) {
                        }
                    }
                    final Info info = available;
                    main.post(() -> done.done(info));
                }
                return;
            }
            checking = true;
        }
        new Thread(() -> {
            Info found = null;
            String err = null;
            String json = null;
            boolean channelOk = false;
            try {
                json = fetch(Config.rawBase() + "version.json?t=" + System.currentTimeMillis());
                channelOk = true;
                found = pick(json);
            } catch (Throwable t) {
                Log.e(Mods.TAG, "update check", t);
                err = String.valueOf(t.getMessage() != null ? t.getMessage() : t);
            }
            if (found == null) { // channel пуст или не ответил — пробуем релиз на GitHub
                try {
                    json = releases();
                    found = pick(json);
                    err = null;
                } catch (Throwable t) {
                    Log.e(Mods.TAG, "update check (releases)", t);
                    if (channelOk) {
                        err = null; // канал ответил, просто новее нет
                    }
                }
            }
            if (json != null) {
                try {
                    Mods.prefs().edit().putLong("update_checked", System.currentTimeMillis())
                            .putString("update_json", json).apply();
                } catch (Exception ignored) {
                }
            }
            final Info result = found;
            final String e = err;
            main.post(() -> {
                synchronized (Update.class) {
                    checking = false;
                }
                error = e;
                if (e == null) {
                    available = result;
                }
                Fake.log("update check: " + (e != null ? "failed: " + e
                        : result != null ? result.label() + " available" : "up to date (" + Mods.label() + ")"));
                done.done(available);
            });
        }, "Clawdio update").start();
    }

    /** Окно «Вышла новая версия» поверх Claude: раз за запуск, и не для версии, которую отложили. */
    private static void offer(Activity a, Info info) {
        try {
            if (info == null || shown || Lock.locked() || a.isFinishing() || a.isDestroyed()
                    || Mods.prefs().getInt("update_skipped", 0) == info.code) {
                return;
            }
            shown = true;
            sheet(a, info, true);
        } catch (Throwable t) {
            Log.e(Mods.TAG, "update offer", t);
        }
    }

    static void sheet(final Activity a, final Info info, boolean canSkip) {
        Ui ui = new Ui(a);
        String text = L.t("Сейчас у тебя ") + Config.brand() + " " + Mods.label() + "."
                + (info.beta > 0 ? L.t(" Это бета: в ней новые функции, но могут быть ошибки.") : "")
                + (info.notes.isEmpty() ? "" : "\n\n" + info.notes)
                + L.t("\n\nОбновление скачается и откроет установщик. Настройки сохранятся.");
        Ui.Sheet sheet = ui.new Sheet(L.t("Вышла ") + Config.brand() + " " + info.label()).message(text);
        if (canSkip) {
            sheet.button(L.t("Пропустить"), false, () -> {
                try {
                    Mods.prefs().edit().putInt("update_skipped", info.code).apply();
                } catch (Exception ignored) {
                }
            });
            sheet.button(L.t("Позже"), false, null);
        } else {
            sheet.button(L.t("Закрыть"), false, null);
        }
        sheet.button(L.t("Скачать"), true, () -> install(a, info));
        sheet.show();
    }

    /**
     * Скачивает обновление, сверяет хеш и подпись с установленным приложением и открывает
     * установщик. Подпись другая — APK всё равно не встал бы поверх, поэтому такое не ставим.
     */
    static void install(final Activity a, final Info info) {
        final Ui ui = new Ui(a);
        final TextView status = ui.label(L.t("Скачиваю…"), 15, ui.secondary, ui.regular);
        final Ui.Sheet sheet = ui.new Sheet(L.t("Обновление")).view(status);
        sheet.button(L.t("Закрыть"), true, null).show();
        new Thread(() -> {
            File apk = new File(a.getCacheDir(), ApkProvider.FILE_NAME);
            String fail = null;
            try {
                downloadTo(info.url(), apk);
                if (!info.sha256.isEmpty()) {
                    String hash = Signer.sha256File(apk);
                    if (!info.sha256.equalsIgnoreCase(hash)) {
                        fail = "hash";
                    }
                }
                if (fail == null) {
                    String mine = Signer.ofApp(a);
                    String theirs = Signer.ofApk(a, apk);
                    if (mine != null && theirs != null && !mine.equals(theirs)) {
                        fail = "signature";
                    }
                }
            } catch (Throwable t) {
                Log.e(Mods.TAG, "update download", t);
                fail = String.valueOf(t.getMessage() != null ? t.getMessage() : t);
            }
            final String error = fail;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (a.isFinishing() || a.isDestroyed()) {
                    return;
                }
                if (error == null) {
                    openInstaller(a);
                    return;
                }
                if ("hash".equals(error)) {
                    status.setText(L.t("Контрольная сумма не совпала: файл повреждён или подменён."));
                    return;
                }
                if ("signature".equals(error)) {
                    status.setText(L.t("Подпись не совпадает: сборка подписана другим ключом "
                            + "и не встанет поверх. Поставь вручную, удалив старую версию."));
                    return;
                }
                status.setText(L.t("Не получилось скачать: ") + error);
            });
        }, "Clawdio update download").start();
    }

    /** Установщик системы: файл отдаёт ApkProvider, доступ даётся только этому Intent. */
    private static void openInstaller(Activity a) {
        try {
            Uri uri = ApkProvider.uri(a, ApkProvider.FILE_NAME);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("", uri));
            a.startActivity(intent);
        } catch (Exception e) {
            Log.e(Mods.TAG, "update install", e);
            Toast.makeText(a, L.t("Не получилось открыть установщик"), Toast.LENGTH_LONG).show();
        }
    }

    static void open(Activity a, String url) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            android.widget.Toast.makeText(a, url, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** Новейшая версия из version.json, которая новее установленной (беты — если включены), или null. */
    static Info pick(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return null;
        }
        JSONObject j = new JSONObject(json);
        boolean betas = betas();
        Info found = null;
        for (String key : new String[] {"release", "beta"}) {
            JSONObject o = j.optJSONObject(key);
            if (o == null || o.optString("version").isEmpty() || o.optString("apk").isEmpty()) {
                continue;
            }
            Info i = new Info(o);
            if (i.beta > 0 && !betas) {
                continue;
            }
            if (i.code > Mods.CODE && (found == null || i.code > found.code)) {
                found = i;
            }
        }
        return found;
    }

    /**
     * Последний релиз на GitHub — запасной источник, если version.json недоступен.
     * Возвращает json того же вида, что и version.json.
     */
    static String releases() throws Exception {
        String base = Config.apiBase();
        if (base.isEmpty()) {
            throw new IllegalStateException("нет репозитория");
        }
        String body = fetch(base + "/releases/latest");
        JSONObject rel = new JSONObject(body);
        String tag = rel.optString("tag_name");
        if (tag.isEmpty()) {
            throw new IllegalStateException("у релиза нет тега");
        }
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        String apk = "";
        JSONArray assets = rel.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String name = a.optString("name");
            if (name.endsWith(".apk")) {
                apk = a.optString("browser_download_url");
                break;
            }
        }
        if (apk.isEmpty()) {
            throw new IllegalStateException("у релиза нет APK");
        }
        String notes = rel.optString("body");
        if (notes.length() > 500) {
            notes = notes.substring(0, 500) + "…";
        }
        JSONObject item = new JSONObject();
        item.put("version", version);
        item.put("beta", 0);
        item.put("apk", apk);
        item.put("notes", notes);
        JSONObject out = new JSONObject();
        out.put("release", item);
        return out.toString();
    }

    static String fetch(String url) throws Exception {
        String text = new String(download(url), "UTF-8");
        // сервер может отдать файл с BOM — JSON-парсер его не переживёт
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    static byte[] download(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setUseCaches(false);
        try {
            if (c.getResponseCode() != 200) {
                throw new java.io.IOException("HTTP " + c.getResponseCode());
            }
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    /** Скачивание файла на диск: APK большой, в память его класть незачем. */
    static void downloadTo(String url, File dest) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setUseCaches(false);
        try {
            if (c.getResponseCode() != 200) {
                throw new java.io.IOException("HTTP " + c.getResponseCode());
            }
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
        } finally {
            c.disconnect();
        }
    }
}

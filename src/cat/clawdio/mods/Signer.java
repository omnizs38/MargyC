package cat.clawdio.mods;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.File;
import java.security.MessageDigest;

/**
 * Отпечаток ключа подписи: у установленного приложения и у скачанного APK. Если ключи разные,
 * APK не встанет поверх (подпись не совпадёт), поэтому такое обновление лучше не предлагать.
 */
final class Signer {
    private Signer() {}

    /** SHA-256 сертификата, которым подписано установленное приложение. */
    static String ofApp(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] sigs = pi.signingInfo != null ? pi.signingInfo.getApkContentsSigners() : null;
            if (sigs == null || sigs.length == 0) {
                return null;
            }
            return sha256(sigs[0].toByteArray());
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * SHA-256 сертификата, которым подписан APK-файл. null — подпись не прочиталась
     * (бывает у сборок с необычной подписью): тогда сверку пропускаем, целостность
     * всё равно проверяется по хешу файла.
     */
    static String ofApk(Context ctx, File apk) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
            if (pi == null || pi.signingInfo == null) {
                return null;
            }
            Signature[] sigs = pi.signingInfo.getApkContentsSigners();
            if (sigs == null || sigs.length == 0) {
                return null;
            }
            return sha256(sigs[0].toByteArray());
        } catch (Throwable t) {
            return null;
        }
    }

    /** SHA-256 файла: для сверки с хешем из version.json. */
    static String sha256File(File f) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return hex(md.digest());
        } finally {
            in.close();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}

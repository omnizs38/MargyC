package cat.clawdio.mods;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Отдаёт установщику скачанный APK. Свой провайдер вместо androidx FileProvider: в оригинальном
 * приложении androidx может не быть, а файл отдавать нужно один и тот же.
 */
public final class ApkProvider extends ContentProvider {
    /** Имя файла в кэше: установщик получит доступ только к нему. */
    static final String FILE_NAME = "clawdio_update.apk";

    /** Адрес файла для установщика. */
    static Uri uri(Context ctx, String name) {
        return new Uri.Builder()
                .scheme("content")
                .authority(ctx.getPackageName() + ".files")
                .appendPath(name)
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File f = file();
        MatrixCursor c = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, 1);
        c.addRow(new Object[] {f.getName(), f.length()});
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = file();
        if (!f.exists()) {
            throw new FileNotFoundException(f.getPath());
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /** Файл отдаётся строго из кэша: путь из URI не используется. */
    private File file() {
        Context ctx = getContext();
        return new File(ctx.getCacheDir(), FILE_NAME);
    }
}

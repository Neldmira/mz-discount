package mz.mozdiscount.demo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Base de dados SQLite local (contas + sessão), exposta ao JavaScript da página como "MozDb".
 * A palavra-passe nunca é guardada: só um hash PBKDF2-HMAC-SHA256 com sal aleatório por conta.
 */
public class LocalDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "mozdiscount.db";
    private static final int DB_VERSION = 1;
    private static final int ITERATIONS = 120000;

    private final SecureRandom random = new SecureRandom();

    public LocalDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts ("
                + "email TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, "
                + "kind TEXT NOT NULL DEFAULT '', entity TEXT NOT NULL DEFAULT '', "
                + "salt TEXT NOT NULL, hash TEXT NOT NULL, iterations INTEGER NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE session (id INTEGER PRIMARY KEY CHECK (id = 1), email TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // sem migrações por agora (versão 1)
    }

    private static String norm(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } finally {
            spec.clearPassword();
        }
    }

    /** Cria a conta. Devolve "ok", "exists" ou "error". */
    @JavascriptInterface
    public String register(String email, String password, String type, String kind, String entity, String name) {
        try {
            String mail = norm(email);
            if (mail.isEmpty() || password == null || password.length() < 8) return "error";
            if (!"client".equals(type) && !"supplier".equals(type)) return "error";
            SQLiteDatabase db = getWritableDatabase();
            try (Cursor c = db.rawQuery("SELECT 1 FROM accounts WHERE email = ?", new String[]{mail})) {
                if (c.moveToFirst()) return "exists";
            }
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            ContentValues v = new ContentValues();
            v.put("email", mail);
            v.put("name", name == null ? "" : name);
            v.put("type", type);
            v.put("kind", kind == null ? "" : kind);
            v.put("entity", entity == null ? "" : entity);
            v.put("salt", Base64.getEncoder().encodeToString(salt));
            v.put("hash", hash(password, salt, ITERATIONS));
            v.put("iterations", ITERATIONS);
            v.put("created", System.currentTimeMillis());
            return db.insert("accounts", null, v) == -1 ? "error" : "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    /** Verifica e-mail + palavra-passe. Devolve o JSON da conta ou "" se for inválido. */
    @JavascriptInterface
    public String login(String email, String password) {
        try {
            String mail = norm(email);
            if (password == null) return "";
            try (Cursor c = getReadableDatabase().rawQuery(
                    "SELECT salt, hash, iterations FROM accounts WHERE email = ?", new String[]{mail})) {
                if (!c.moveToFirst()) return "";
                byte[] salt = Base64.getDecoder().decode(c.getString(0));
                String expected = c.getString(1);
                String actual = hash(password, salt, c.getInt(2));
                if (!MessageDigest.isEqual(expected.getBytes("UTF-8"), actual.getBytes("UTF-8"))) return "";
            }
            return account(mail);
        } catch (Exception e) {
            return "";
        }
    }

    /** Dados públicos da conta (sem hash) em JSON, ou "". */
    @JavascriptInterface
    public String account(String email) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT email, name, type, kind, entity FROM accounts WHERE email = ?", new String[]{norm(email)})) {
            if (!c.moveToFirst()) return "";
            JSONObject o = new JSONObject();
            o.put("email", c.getString(0));
            o.put("name", c.getString(1));
            o.put("type", c.getString(2));
            o.put("kind", c.getString(3));
            o.put("entity", c.getString(4));
            return o.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getSession() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT email FROM session WHERE id = 1", null)) {
            return c.moveToFirst() ? c.getString(0) : "";
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public void setSession(String email) {
        try {
            ContentValues v = new ContentValues();
            v.put("id", 1);
            v.put("email", norm(email));
            getWritableDatabase().insertWithOnConflict("session", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception ignored) {
        }
    }

    @JavascriptInterface
    public void clearSession() {
        try {
            getWritableDatabase().delete("session", null, null);
        } catch (Exception ignored) {
        }
    }

    /** Elimina a conta (e a sessão, se for a dela). */
    @JavascriptInterface
    public boolean deleteAccount(String email) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String mail = norm(email);
            db.delete("session", "email = ?", new String[]{mail});
            return db.delete("accounts", "email = ?", new String[]{mail}) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

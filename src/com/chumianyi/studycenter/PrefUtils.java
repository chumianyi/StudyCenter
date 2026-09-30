package com.chumianyi.studycenter;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 统一读写工具类。
 * 所有配置（科目名、语速、音调、深色模式、词典历史）都走这里，避免散落在各 Activity。
 */
public class PrefUtils {

    private static final String NAME = "study_center_pref";

    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_SPEECH_RATE = "speech_rate";   // 0.5 - 2.0
    public static final String KEY_PITCH = "pitch";              // 0.5 - 2.0
    public static final String KEY_DARK = "dark_theme";
    public static final String KEY_HISTORY = "dict_history";     // 用逗号分隔
    public static final String KEY_LAST_INPUT = "tts_last_input";

    private static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getString(Context ctx, String key, String def) {
        try {
            return get(ctx).getString(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void setString(Context ctx, String key, String val) {
        try {
            get(ctx).edit().putString(key, val).commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static float getFloat(Context ctx, String key, float def) {
        try {
            return get(ctx).getFloat(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void setFloat(Context ctx, String key, float val) {
        try {
            get(ctx).edit().putFloat(key, val).commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean getBoolean(Context ctx, String key, boolean def) {
        try {
            return get(ctx).getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void setBoolean(Context ctx, String key, boolean val) {
        try {
            get(ctx).edit().putBoolean(key, val).commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void remove(Context ctx, String key) {
        try {
            get(ctx).edit().remove(key).commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

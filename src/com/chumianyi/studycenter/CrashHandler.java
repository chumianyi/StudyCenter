package com.chumianyi.studycenter;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局未捕获异常处理器。
 * 任何线程抛出未捕获异常时，把堆栈写到私有目录下 crash.log，
 * 然后跳转到 CrashLogActivity 展示日志。
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_FILE = "crash.log";
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /** 安装为默认未捕获异常处理器。 */
    public static void install(Context ctx) {
        CrashHandler handler = new CrashHandler(ctx);
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String log = buildLog(thread, ex);
        writeLog(log);

        // 跳转崩溃日志页
        try {
            Intent intent = new Intent(context, CrashLogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("log", log);
            context.startActivity(intent);
        } catch (Throwable t) {
            // 跳转失败则交给默认处理器
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            }
            return;
        }

        // 结束当前进程，避免应用卡在异常状态
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    /** 拼接崩溃堆栈。 */
    private String buildLog(Thread thread, Throwable ex) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        sb.append("时间: ").append(sdf.format(new Date())).append("\n");
        sb.append("线程: ").append(thread.getName()).append("\n");
        sb.append("设备: ").append(android.os.Build.MODEL)
                .append(" (Android ").append(android.os.Build.VERSION.RELEASE).append(")\n");
        sb.append("包名: ").append(context.getPackageName()).append("\n\n");
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        sb.append(sw.toString());
        return sb.toString();
    }

    /** 把日志写到私有目录。优先用 getFilesDir，失败则写外部缓存。 */
    private void writeLog(String log) {
        FileOutputStream fos = null;
        try {
            File dir = context.getFilesDir();
            if (dir == null) {
                dir = context.getCacheDir();
            }
            if (dir == null) {
                dir = Environment.getExternalStorageDirectory();
            }
            File file = new File(dir, CRASH_FILE);
            fos = new FileOutputStream(file, false);
            fos.write(log.getBytes("UTF-8"));
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /** 读取上次崩溃日志（CrashLogActivity 用）。 */
    public static String readLastCrash(Context ctx) {
        try {
            File dir = ctx.getFilesDir();
            File file = new File(dir, CRASH_FILE);
            if (!file.exists()) {
                return null;
            }
            byte[] buf = new byte[(int) file.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            int read = fis.read(buf);
            fis.close();
            if (read > 0) {
                return new String(buf, 0, read, "UTF-8");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}

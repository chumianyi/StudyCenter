package com.chumianyi.studycenter;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 检查更新工具：
 * 1. 从 https://chumianyi.github.io/study-center-update/version.json 拉取版本信息；
 * 2. 与当前 versionCode 比较；
 * 3. 有新版本：弹框显示版本号 / 更新日志，点"立即更新"用 DownloadManager 下载 APK；
 * 4. 下载完成后弹出安装 Intent（minSdk17 直接用 Uri.fromFile）。
 */
public class UpdateHelper {

    private static final String VERSION_URL =
            "https://chumianyi.github.io/study-center-update/version.json";

    /** 外部入口。 */
    public static void checkUpdate(final Context ctx) {
        Toast.makeText(ctx, "正在检查更新...", Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(VERSION_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();
                    int code = conn.getResponseCode();
                    InputStream is = (code >= 200 && code < 300)
                            ? conn.getInputStream() : conn.getErrorStream();
                    reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                    return sb.toString();
                } catch (Throwable t) {
                    return "ERROR:" + t.getMessage();
                } finally {
                    if (reader != null) try { reader.close(); } catch (Throwable ignore) {}
                    if (conn != null) try { conn.disconnect(); } catch (Throwable ignore) {}
                }
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    if (result == null || result.startsWith("ERROR:")) {
                        Toast.makeText(ctx,
                                "检查更新失败：网络错误\n" + (result == null ? "" : result.substring(6)),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    JSONObject json = new JSONObject(result);
                    int remoteCode = json.optInt("version_code", 0);
                    String remoteName = json.optString("version_name", "");
                    String apkUrl = json.optString("apk_url", "");
                    String changelog = json.optString("changelog", "");

                    int currentCode = getCurrentVersionCode(ctx);
                    if (remoteCode > currentCode) {
                        showUpdateDialog(ctx, remoteName, remoteCode, changelog, apkUrl);
                    } else {
                        Toast.makeText(ctx, "已是最新版本", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(ctx, "解析版本信息失败: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private static int getCurrentVersionCode(Context ctx) {
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return pi.versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void showUpdateDialog(final Context ctx, final String name,
            int code, String changelog, final String apkUrl) {
        new AlertDialog.Builder(ctx)
                .setTitle("发现新版本 " + name + " (versionCode=" + code + ")")
                .setMessage("更新内容：\n" + changelog)
                .setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        startDownload(ctx, apkUrl, name);
                    }
                })
                .setNegativeButton("稍后", null)
                .show();
    }

    /** 用 DownloadManager 下载 APK，并注册完成广播触发安装。 */
    private static void startDownload(Context ctx, String url, String versionName) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setMimeType("application/vnd.android.package-archive");
            req.setTitle("学习中心 更新");
            req.setDescription("正在下载 " + versionName);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "StudyCenter-" + versionName + ".apk");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            final long downloadId = dm.enqueue(req);

            // 注册下载完成广播
            BroadcastReceiver receiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        installApk(context, downloadId);
                        try {
                            context.unregisterReceiver(this);
                        } catch (Throwable ignore) {}
                    }
                }
            };
            ctx.registerReceiver(receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            Toast.makeText(ctx, "已开始下载，请在通知栏查看进度",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(ctx, "下载失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 根据 downloadId 拿到下载好的文件并发起安装。 */
    private static void installApk(Context ctx, long downloadId) {
        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(downloadId);
            if (uri == null) {
                // 直接拼文件路径
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "app-release.apk");
                if (file.exists()) uri = Uri.fromFile(file);
            }
            if (uri == null) {
                Toast.makeText(ctx, "未找到下载的 APK", Toast.LENGTH_LONG).show();
                return;
            }
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // minSdk17：不需要 FileProvider，也不需要 REQUEST_INSTALL_PACKAGES
            ctx.startActivity(install);
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(ctx, "安装失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

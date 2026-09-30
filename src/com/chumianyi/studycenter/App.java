package com.chumianyi.studycenter;

import android.app.Application;
import android.content.Context;

/**
 * 应用入口：初始化全局崩溃捕获器。
 * 所有 Activity 启动前，先把 CrashHandler 安装为默认未捕获异常处理器，
 * 这样任何 Activity 抛出未捕获异常时，都会落到 CrashHandler 里，
 * 写日志文件并跳转 CrashLogActivity，而不是直接闪退。
 */
public class App extends Application {

    /** 全局上下文，供工具类使用。 */
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        try {
            CrashHandler.install(this);
        } catch (Throwable t) {
            // 安装崩溃处理器本身失败不能再抛
            t.printStackTrace();
        }
    }

    /** 获取全局 Context。 */
    public static Context getAppContext() {
        return appContext;
    }
}

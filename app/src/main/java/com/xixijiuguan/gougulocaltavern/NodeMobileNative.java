package com.xixijiuguan.gougulocaltavern;

public final class NodeMobileNative {
    private static boolean loaded = false;
    private static String error = "";
    static {
        try { System.loadLibrary("native-lib"); loaded = true; }
        catch (Throwable t) { loaded = false; error = t.getMessage() == null ? t.toString() : t.getMessage(); }
    }
    public static boolean isAvailable() { return loaded; }
    public static String lastError() { return error; }
    public static native int startNodeWithArguments(String[] args);
}

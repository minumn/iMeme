package com.mikkel.tais.imeme.Models;

public class Stats {
    public static final String SHARED_PREFS_NAME = "imeme_stats_prefs";
    public static final String SHARED_PREFS_KEY_BOOL_FIRST_TIME = "prefs_key_first_time";
    public static final String SHARED_PREFS_KEY_LONG_FIRST_TIME = "prefs_key_first_time_date";
    public static final String SHARED_PREFS_KEY_BOOL_NOTI = "prefs_key_noti_level";
    public static final String SHARED_PREFS_KEY_INT_SILENT_START = "prefs_key_silent_start";
    public static final String SHARED_PREFS_KEY_INT_SILENT_END = "prefs_key_silent_end";

    public static final String SHARED_PREFS_KEY_INT_BLB_SEEN = "prefs_key_blb_seen";
    public static final String SHARED_PREFS_KEY_INT_BLB_SAVED = "prefs_key_blb_saved";
    public static final String SHARED_PREFS_KEY_INT_BLB_SHARED = "prefs_key_blb_shared";
    public static final String SHARED_PREFS_KEY_INT_BLB_AVG_SEEN_DAY = "prefs_key_blb_avg_seen_day";

    public static final String SHARED_PREFS_KEY_INT_GEN_SEEN = "prefs_key_gen_seen";
    public static final String SHARED_PREFS_KEY_INT_GEN_SAVED = "prefs_key_gen_saved";
    public static final String SHARED_PREFS_KEY_INT_GEN_SHARED = "prefs_key_gen_shared";
    public static final String SHARED_PREFS_KEY_INT_GEN_AVG_SEEN_DAY = "prefs_key_gen_avg_seen_day";

    private int totalBLBSeen;
    private int totalBLBSaved;
    private int totalBLBShared;
    private float totalBLBAvgSeenDay;
    private int totalGeneratedSeen;
    private int totalGeneratedSaved;
    private int totalGeneratedShared;
    private float totalGeneratedAvgSeenDay;

    public Stats() {
    }

    public int getTotalBLBSeen() {
        return totalBLBSeen;
    }

    public void setTotalBLBSeen(int totalBLBSeen) {
        this.totalBLBSeen = totalBLBSeen;
    }

    public int getTotalBLBSaved() {
        return totalBLBSaved;
    }

    public void setTotalBLBSaved(int totalBLBSaved) {
        this.totalBLBSaved = totalBLBSaved;
    }

    public int getTotalBLBShared() {
        return totalBLBShared;
    }

    public void setTotalBLBShared(int totalBLBShared) {
        this.totalBLBShared = totalBLBShared;
    }

    public float getTotalBLBAvgSeenDay() {
        return totalBLBAvgSeenDay;
    }

    public void setTotalBLBAvgSeenDay(float totalBLBAvgSeenDay) {
        this.totalBLBAvgSeenDay = totalBLBAvgSeenDay;
    }

    public int getTotalGeneratedSeen() {
        return totalGeneratedSeen;
    }

    public void setTotalGeneratedSeen(int totalGeneratedSeen) {
        this.totalGeneratedSeen = totalGeneratedSeen;
    }

    public int getTotalGeneratedSaved() {
        return totalGeneratedSaved;
    }

    public void setTotalGeneratedSaved(int totalGeneratedSaved) {
        this.totalGeneratedSaved = totalGeneratedSaved;
    }

    public int getTotalGeneratedShared() {
        return totalGeneratedShared;
    }

    public void setTotalGeneratedShared(int totalGeneratedShared) {
        this.totalGeneratedShared = totalGeneratedShared;
    }

    public float getTotalGeneratedAvgSeenDay() {
        return totalGeneratedAvgSeenDay;
    }

    public void setTotalGeneratedAvgSeenDay(float totalGeneratedAvgSeenDay) {
        this.totalGeneratedAvgSeenDay = totalGeneratedAvgSeenDay;
    }
}
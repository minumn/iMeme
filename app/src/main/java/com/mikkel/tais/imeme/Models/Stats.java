package com.mikkel.tais.imeme.Models;

public class Stats {
    public static final String SHARED_PREFS_NAME = "imeme_stats_prefs";
    public static final String SHARED_PREFS_KEY_BOOL_FIRST_TIME = "prefs_key_first_time";
    public static final String SHARED_PREFS_KEY_STRING_FIRST_TIME = "prefs_key_first_time_date";
    public static final String SHARED_PREFS_KEY_INT_BLB_SEEN = "prefs_key_blb_seen";
    public static final String SHARED_PREFS_KEY_INT_BLB_SAVED = "prefs_key_blb_saved";
    public static final String SHARED_PREFS_KEY_INT_BLB_SHARED = "prefs_key_blb_shared";

    private int totalBLBSeen;
    private int totalBLBSaved;
    private int totalBLBShared;

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
}
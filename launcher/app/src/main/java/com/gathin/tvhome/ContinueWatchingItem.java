package com.gathin.tvhome;

import android.content.Intent;

/**
 * Data item representing a movie, show, or media title in the Continue Watching flexbar.
 */
public final class ContinueWatchingItem {
    final String id;
    final String title;
    final String subtitle;
    final String appName;
    final String packageName;
    final String posterUri;
    final int posterResId;
    final Intent launchIntent;
    final int progressPercent; // 0 to 100
    final long lastEngagementTime;

    public ContinueWatchingItem(String id, String title, String subtitle, String appName,
                                String packageName, String posterUri, int posterResId,
                                Intent launchIntent, int progressPercent, long lastEngagementTime) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.appName = appName;
        this.packageName = packageName;
        this.posterUri = posterUri;
        this.posterResId = posterResId;
        this.launchIntent = launchIntent;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.lastEngagementTime = lastEngagementTime;
    }
}

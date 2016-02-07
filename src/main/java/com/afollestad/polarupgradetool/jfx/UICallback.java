package com.afollestad.polarupgradetool.jfx;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public interface UICallback {
    void onProjectDetected(String applicationName, String applicationPackage, String applicationVersionName, String applicationVersionCode);

    void onErrorOccured(String errorMessage);

    void onArchiveDownloadFailed(String errorMessage);

    void onStatusUpdate(String statusMessage);

    void onUpdateSuccessful();
}

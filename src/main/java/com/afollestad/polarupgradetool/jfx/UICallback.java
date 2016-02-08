package com.afollestad.polarupgradetool.jfx;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public interface UICallback {

    void onProjectDetected(String applicationName, String applicationPackage, String applicationVersionName, String applicationVersionCode);

    void onErrorOccurred(String errorMessage);

    void onArchiveDownloadStarted(String sizeStr);

    void onArchiveDownloadProgress(String progressStr);

    void onArchiveDownloadFailed(String errorMessage);

    void onArchiveDownloadSuccess();

    void onStatusUpdate(String statusMessage);

    void onUpdateSuccessful();
}

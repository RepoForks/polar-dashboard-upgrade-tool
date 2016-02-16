package com.afollestad.polarupgradetool.jfx

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
interface UICallback {

    fun onProjectDetected(applicationName: String, applicationPackage: String, applicationVersionName: String, applicationVersionCode: String)

    fun onErrorOccurred(errorMessage: String)

    fun onArchiveDownloadStarted(sizeStr: String)

    fun onArchiveDownloadProgress(progressStr: String)

    fun onArchiveDownloadFailed(errorMessage: String)

    fun onArchiveDownloadSuccess()

    fun onStatusUpdate(statusMessage: String)

    fun onUpdateSuccessful()
}
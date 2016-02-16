package com.afollestad.polarupgradetool.utils

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
class UpdateUtils private constructor(private val updateCallback: UpdateUtils.UpdateCallback) {

    interface UpdateCallback {
        fun onUpdateCheckStarted()

        fun onUpdateCheckFailed(errorMsg: String)

        fun onUpdateCheckFinished(currentVersion: String, latestVersion: String)
    }

    fun execute() {
        UpdateThread().start()
    }

    private inner class UpdateThread : Thread(), Runnable {

        override fun run() {
            updateCallback.onUpdateCheckStarted()
            val pom = ManifestUtils.remoteApplicationModel
            if (pom == null) {
                updateCallback.onUpdateCheckFailed("Unable to resolve external pom model.")
                this.interrupt()
            } else if (!isInterrupted) {
                val currentVersion = ManifestUtils.getApplicationVersion(UpdateUtils::class.java)
                val externalVersion = pom.version
                updateCallback.onUpdateCheckFinished(currentVersion, externalVersion)
            }
        }
    }

    companion object {
        fun checkForUpdate(updateCallback: UpdateCallback): UpdateUtils {
            return UpdateUtils(updateCallback)
        }
    }
}
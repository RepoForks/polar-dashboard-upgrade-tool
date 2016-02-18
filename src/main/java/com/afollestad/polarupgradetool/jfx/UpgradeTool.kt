package com.afollestad.polarupgradetool.jfx

import com.afollestad.polarupgradetool.utils.UpdateUtils
import com.afollestad.polarupgradetool.utils.UrlUtils
import javafx.application.Application
import javafx.application.HostServices
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.util.Duration
import org.controlsfx.control.Notifications

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
class UpgradeTool : Application(), UpdateUtils.UpdateCallback {

    private var silent = true

    @Throws(Exception::class)
    override fun start(stage: Stage) {
        instance = this
        hostService = hostServices

        stage.title = "Polar Dashboard Upgrade Tool"
        stage.isResizable = false

        val windowScene = WindowScene()
        val scene = windowScene.scene

        stage.icons.add(Image(javaClass.getResourceAsStream("/polar_upgrade_1024.png")))
        stage.scene = scene
        stage.show()

        updateCheck()
    }

    fun updateCheck() {
        UpdateUtils.checkForUpdate(this).execute()
    }

    override fun onUpdateCheckStarted() {
    }

    override fun onUpdateCheckFailed(errorMsg: String) {
        if (Platform.isFxApplicationThread()) {
            if (!silent) {
                val alert = Alert(Alert.AlertType.ERROR)
                with(alert) {
                    title = "Polar Upgrade Tool: Error"
                    headerText = "Unable to check for update!"
                    contentText = "An error occurred while trying to resolve the latest version of Polar Upgrade Tool:\n\n$errorMsg\n\nPlease make sure you're connected to the internet and try again,\notherwise head over to the GitHub Repository."
                    dialogPane.setPrefSize(550.0, 360.0)
                    isResizable = false
                    showAndWait()
                }
            }
            silent = false
        } else
            Platform.runLater { onUpdateCheckFailed(errorMsg) }
    }

    @Suppress("NAME_SHADOWING")
    override fun onUpdateCheckFinished(currentVersion: String, latestVersion: String) {
        var currentVersion = currentVersion
        var latestVersion = latestVersion
        if (currentVersion.endsWith("-SNAPSHOT"))
            currentVersion = currentVersion.substring(0, currentVersion.indexOf("-SNAPSHOT"))
        if (latestVersion.endsWith("-SNAPSHOT"))
            latestVersion = latestVersion.substring(0, latestVersion.indexOf("-SNAPSHOT"))

        val current = parseVersion(currentVersion)
        val latest = parseVersion(latestVersion)

        if (Platform.isFxApplicationThread()) {
            if (isNewer(latest, current)) {
                val alert = Alert(Alert.AlertType.INFORMATION)
                with(alert) {
                    title = "Polar Upgrade Tool: Information"
                    headerText = "Update available:\n" + latestVersion
                    contentText = "A new version for Polar Upgrade Tool is available!\n\nCurrent Version: $currentVersion\nLatest Version: $latestVersion\n\nNote: It's always recommended to use the latest version."
                    dialogPane.setPrefSize(500.0, 260.0)
                    isResizable = false

                    val okBtn = ButtonType("Update")
                    val ignBtn = ButtonType("Skip")

                    buttonTypes.removeAll(alert.buttonTypes)
                    buttonTypes.addAll(ignBtn, okBtn)

                    val result = alert.showAndWait()
                    if (result.get() == okBtn) {
                        UrlUtils.openReleasePage()
                        Platform.exit()
                        System.exit(0)
                    }
                }
            } else if (!silent) {
                //show a notification to give visual feedback that the operation was successful
                Notifications.create().title("Everything is up to date").text("Current Version: $currentVersion\nLatest Version: $latestVersion").hideAfter(Duration.seconds(3.0)).position(Pos.TOP_RIGHT).showInformation()
            }
            silent = false
        } else {
            val fCurrentVersion = currentVersion
            val fLatestVersion = latestVersion
            Platform.runLater { onUpdateCheckFinished(fCurrentVersion, fLatestVersion) }
        }
    }

    companion object {

        val GITHUB_REPO = "https://github.com/afollestad/polar-dashboard-upgrade-tool"

        var hostService: HostServices? = null
            private set
        var instance: UpgradeTool? = null
            private set

        private fun parseVersion(version: String): IntArray {
            if (version == "???")
                return intArrayOf(1, 0, 0)
            try {
                val split = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val result = IntArray(split.size)
                for (i in split.indices)
                    result[i] = Integer.parseInt(split[i])
                return result
            } catch (t: Throwable) {
                return intArrayOf(1, 0, 0)
            }
        }

        @Suppress("NAME_SHADOWING")
        private fun isNewer(latest: IntArray, current: IntArray): Boolean {
            var latest = latest
            var current = current
            if (latest.size != current.size) {
                if (latest.size > current.size) {
                    val newCurrent = IntArray(latest.size)
                    for (i in newCurrent.indices) {
                        if (i < current.size)
                            newCurrent[i] = current[i]
                        else
                            newCurrent[i] = 0
                    }
                    current = newCurrent
                }
                if (current.size > latest.size) {
                    val newLatest = IntArray(current.size)
                    for (i in newLatest.indices) {
                        if (i < latest.size)
                            newLatest[i] = latest[i]
                        else
                            newLatest[i] = 0
                    }
                    latest = newLatest
                }
            }
            var newer = false
            for (i in latest.indices) {
                if (latest[i] > current[i]) {
                    newer = true
                    break
                }
            }
            return newer
        }

        @JvmStatic fun main(args: Array<String>) {
            Application.launch(UpgradeTool::class.java, *args)
        }
    }
}
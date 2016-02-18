package com.afollestad.polarupgradetool.jfx

import com.afollestad.polarupgradetool.Main
import com.afollestad.polarupgradetool.utils.SecurityUtil
import com.afollestad.polarupgradetool.utils.UrlUtils
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.controlsfx.control.MaskerPane
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.GlyphFontRegistry
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
class WindowScene {

    var scene: Scene
    private val windowSceneController: WindowSceneController
    private var updateBtn: Button? = null
    private var logMessages: ObservableList<String>? = null

    init {
        windowSceneController = WindowSceneController()
        scene = Scene(windowSceneController)
        windowSceneController.setRootScene(scene)
    }

    private inner class WindowSceneController internal constructor() : StackPane(), UICallback {

        private var selectedFolder: File? = null
        private var interfaceUpdateThread: InterfaceUpdateThread? = null
        private var maskerPane: MaskerPane? = null
        //main menu bar and menu items
        private var menuBar: MenuBar? = null
        private var fileMenu: Menu? = null
        private val fontAwesome = GlyphFontRegistry.font("FontAwesome")

        @FXML lateinit var logMessages: ObservableList<String>

        //ui elements
        @FXML lateinit var projectLocationTextField: TextField
//        @FXML lateinit var browseButton: Button
        @FXML lateinit var messageListView: ListView<String>
        @FXML lateinit var updateBtn: Button
        @FXML lateinit var copyrightLabel: Hyperlink
        @FXML lateinit var mainPane: StackPane
        @FXML lateinit var boxPane: VBox

        init {
            try {
                val fxmlLoader = FXMLLoader(javaClass.getResource("/applicationUI.fxml"))
                fxmlLoader.setRoot(this)
                fxmlLoader.setController(this)
                fxmlLoader.load<Any>()

                maskerPane = MaskerPane()
                maskerPane!!.isVisible = false

                mainPane.children.addAll(maskerPane)

                createMenuBar()

                projectLocationTextField.isEditable = false
                logMessages = FXCollections.observableArrayList<String>()
                messageListView.items = logMessages
                messageListView.isEditable = false
                updateBtn.isVisible = false
                updateBtn.setOnAction { event ->
                    //Main.upgrade(selectedFolder.getAbsolutePath(), WindowSceneController.this);
                    with(maskerPane!!) {
                        text = "Updating " + if (Main.USER_APPNAME.isEmpty()) "Project" else Main.USER_APPNAME
                        isVisible = true
                    }
                    updateBtn.isVisible = false
                    interfaceUpdateThread = InterfaceUpdateThread(selectedFolder!!.absolutePath, this)
                    interfaceUpdateThread!!.start()
                }

                this@WindowScene.updateBtn = updateBtn
                this@WindowScene.logMessages = logMessages
                copyrightLabel.text = "(c) ${Calendar.getInstance().get(Calendar.YEAR)} Polar Upgrade Tool"
            } catch (io: IOException) {
                io.printStackTrace()
            }

        }

        private fun createMenuBar() {
            //create the menuBar
            menuBar = MenuBar()
            //Create the menuItems
            fileMenu = Menu("Menu")
            menuBar!!.menus.addAll(fileMenu)
            val helpItem = MenuItem("Help / Usage", fontAwesome.create(FontAwesome.Glyph.QUESTION).color(Color.color(0.0, 0.0, 0.0, 0.87)))
            helpItem.setOnAction { event -> UrlUtils.openWikiPage() }

            val checkUpdateItem = MenuItem("Check for Update", fontAwesome.create(FontAwesome.Glyph.DOWNLOAD).color(Color.color(0.0, 0.0, 0.0, 0.87)))
            checkUpdateItem.setOnAction { event -> UpgradeTool.instance?.updateCheck() }

            val aboutItem = MenuItem("About", fontAwesome.create(FontAwesome.Glyph.INFO).color(Color.color(0.0, 0.0, 0.0, 0.87)))
            aboutItem.setOnAction { event ->
                val stage = Stage()
                stage.title = "About Polar Upgrade Tool"
                stage.isResizable = false
                val aboutScene = AboutScene()
                stage.scene = aboutScene.scene
                stage.show()
            }

            fileMenu!!.items.addAll(helpItem, checkUpdateItem, aboutItem)
            boxPane.children.add(0, menuBar)
        }

        fun setRootScene(scene: Scene) {
            this@WindowScene.scene = scene
        }

        @FXML
        protected fun openFolderChooserDialog() {
            val directoryChooser = DirectoryChooser()
            selectedFolder = directoryChooser.showDialog(scene.window)
            if (selectedFolder != null) {
                projectLocationTextField.text = selectedFolder!!.absolutePath
                if (SecurityUtil.checkIsPolarBased(selectedFolder!!.absolutePath)) {
                    updateBtn.isVisible = true
                } else {
                    showSecurityInfoDialog(selectedFolder!!.absolutePath)
                }
            }
        }

        @FXML
        protected fun onHyperlinkClicked() {
            UpgradeTool.hostService?.showDocument(UpgradeTool.GITHUB_REPO)
        }

        override fun onProjectDetected(applicationName: String, applicationPackage: String, applicationVersionName: String, applicationVersionCode: String) {
            if (Platform.isFxApplicationThread()) {
                logMessages.add("Found Project: $applicationName [$applicationPackage], Version Name: $applicationVersionName, Version Code: $applicationVersionCode")
                messageListView.scrollTo(logMessages.size - 1)
                maskerPane!!.text = "Updating " + Main.USER_APPNAME
            } else {
                Platform.runLater { onProjectDetected(applicationName, applicationPackage, applicationVersionName, applicationVersionCode) }
            }
        }

        override fun onErrorOccurred(errorMessage: String) {
            if (Platform.isFxApplicationThread()) {
                maskerPane!!.isVisible = false
                showErrorDialog(errorMessage)
            } else {
                Platform.runLater {
                    maskerPane!!.isVisible = false
                    showErrorDialog(errorMessage)
                }
            }
        }

        override fun onArchiveDownloadStarted(sizeStr: String) {
            if (Platform.isFxApplicationThread()) {
                onStatusUpdate("Downloading a ZIP of Polar's latest code (%s)...".format(sizeStr))
            } else {
                Platform.runLater { onArchiveDownloadStarted(sizeStr) }
            }
        }

        override fun onArchiveDownloadProgress(progressStr: String) {
            if (Platform.isFxApplicationThread()) {
                maskerPane!!.text = "Downloading latest source\n" + progressStr
            } else {
                Platform.runLater { onArchiveDownloadProgress(progressStr) }
            }
        }

        override fun onArchiveDownloadSuccess() {
            if (Platform.isFxApplicationThread()) {
                onStatusUpdate("Download complete!")
                maskerPane!!.text = "Migrating resources..."
            } else {
                Platform.runLater {
                    onArchiveDownloadSuccess()
                    maskerPane!!.text = "Migrating resources..."
                }
            }
        }

        override fun onArchiveDownloadFailed(errorMessage: String) {
            if (Platform.isFxApplicationThread()) {
                showErrorDialog(errorMessage)
                maskerPane!!.isVisible = false
            } else {
                Platform.runLater { onArchiveDownloadFailed(errorMessage) }
            }
        }

        override fun onStatusUpdate(statusMessage: String) {
            if (Platform.isFxApplicationThread()) {
                logMessages.add(statusMessage)
                messageListView.scrollTo(logMessages.size - 1)
            } else {
                Platform.runLater { onStatusUpdate(statusMessage) }
            }
        }

        override fun onUpdateSuccessful() {
            if (Platform.isFxApplicationThread()) {
                maskerPane!!.isVisible = false
                showUpdateSuccessDialog()
            } else {
                Platform.runLater {
                    maskerPane!!.isVisible = false
                    showUpdateSuccessDialog()
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Polar Upgrade Tool: Error"
        alert.headerText = "An error occurred!"
        alert.contentText = message
        alert.dialogPane.setPrefSize(550.0, 270.0)
        alert.isResizable = true
        alert.setOnHiding { event ->
            //            Platform.exit();
            //            System.exit(0);
            logMessages!!.clear()
            if (updateBtn != null)
                updateBtn!!.isVisible = true
        }
        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            //            Platform.exit();
            //            System.exit(0);
            logMessages!!.clear()
            if (updateBtn != null)
                updateBtn!!.isVisible = true
        }
    }

    private fun showUpdateSuccessDialog() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Polar Upgrade Tool: Info"
        alert.headerText = "Update successful!"
        alert.contentText = "${Main.USER_APPNAME} is now up to date! Your configuration has been restored.\n\n" +
                "Find any issues? Please report them on GitHub. You can undo changes made by this tool either " +
                "using the backup ZIP archive placed in your project directory, or by using the following Git " +
                "commands:\n\ngit add -A\ngit stash save\ngit stash drop"
        alert.dialogPane.setPrefSize(550.0, 360.0)
        alert.isResizable = true
        alert.showAndWait()
        updateBtn!!.isVisible = true
    }

    fun showSecurityInfoDialog(path: String) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.title = "Polar Upgrade Tool: Warning"
        alert.headerText = "WARNING: Unable to find Polar based Project!"
        alert.dialogPane.setPrefSize(550.0, 360.0)
        alert.contentText = "The project located at:\n\n$path\n\ndoesn't seem to be a project based on Polar.\nProceeding with the upgrade might destroy your project setup and build system.\nPlease make sure to backup your current data before continuing,\nyou proceed at your own risk!"
        alert.isResizable = false

        val confirmBtn = ButtonType("Got it, Continue anyway!", ButtonBar.ButtonData.OK_DONE)
        val cancelBtn = ButtonType("Abort Process", ButtonBar.ButtonData.CANCEL_CLOSE)

        alert.buttonTypes.removeAll(alert.buttonTypes)
        alert.buttonTypes.addAll(confirmBtn, cancelBtn)

        val result = alert.showAndWait()
        if (result.get() == confirmBtn) {
            updateBtn!!.isVisible = true
        } else {
            updateBtn!!.isVisible = false
        }

    }
}
package com.afollestad.polarupgradetool.jfx;

import com.afollestad.polarupgradetool.Main;
import com.afollestad.polarupgradetool.utils.SecurityUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Optional;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public class WindowScene {

    private final Scene scene;
    private final WindowSceneController windowSceneController;
    private Button updateBtn;
    private ObservableList<String> logMessages;

    public WindowScene() {
        windowSceneController = new WindowSceneController();
        scene = new Scene(windowSceneController);
        windowSceneController.setRootScene(scene);
    }



    public WindowSceneController getWindowSceneController() {
        return windowSceneController;
    }

    public Scene getScene() {
        return scene;
    }

    private class WindowSceneController extends StackPane implements UICallback {

        private Scene scene;
        private File selectedFolder;
        private InterfaceUpdateThread interfaceUpdateThread;
        private MaskerPane maskerPane;
        //main menu bar and menu items
        private MenuBar menuBar;
        private Menu fileMenu;
        private GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        @FXML
        private ObservableList<String> logMessages;

        //ui elements
        @FXML
        private TextField projectLocationTextField;
        @FXML
        private Button browseButton;
        @FXML
        private ListView<String> messageListView;
        @FXML
        private Button updateBtn;
        @FXML
        private Hyperlink copyrightLabel;
        @FXML
        private StackPane mainPane;
        @FXML
        private VBox boxPane;

        WindowSceneController() {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/applicationUI.fxml"));
                fxmlLoader.setRoot(this);
                fxmlLoader.setController(this);
                fxmlLoader.load();

                maskerPane = new MaskerPane();
                maskerPane.setVisible(false);

                mainPane.getChildren().addAll(maskerPane);

                createMenuBar();

                projectLocationTextField.setEditable(false);
                logMessages = FXCollections.observableArrayList();
                messageListView.setItems(logMessages);
                messageListView.setEditable(false);
                updateBtn.setVisible(false);
                updateBtn.setOnAction(event -> {
                    //Main.upgrade(selectedFolder.getAbsolutePath(), WindowSceneController.this);
                    maskerPane.setText("Updating " + (Main.USER_APPNAME == null ? "Project" : Main.USER_APPNAME));
                    maskerPane.setVisible(true);
                    updateBtn.setVisible(false);
                    interfaceUpdateThread = new InterfaceUpdateThread(selectedFolder.getAbsolutePath(), this);
                    interfaceUpdateThread.start();
                });

                WindowScene.this.updateBtn = updateBtn;
                WindowScene.this.logMessages = logMessages;
                copyrightLabel.setText(String.format("(c) %d Polar Upgrade Tool", Calendar.getInstance().get(Calendar.YEAR)));
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        private void createMenuBar() {
            //create the menuBar
            menuBar = new MenuBar();
            //Create the menuItems
            fileMenu = new Menu("Menu");
            menuBar.getMenus().addAll(fileMenu);
            MenuItem helpItem = new MenuItem("Help / Usage", fontAwesome.create(FontAwesome.Glyph.QUESTION).color(Color.color(0, 0, 0, 0.87)));
            helpItem.setOnAction(event -> {
            });

            MenuItem checkUpdateItem = new MenuItem("Check for Update", fontAwesome.create(FontAwesome.Glyph.DOWNLOAD).color(Color.color(0, 0, 0, 0.87)));
            checkUpdateItem.setOnAction(event -> {
            });

            MenuItem aboutItem = new MenuItem("About", fontAwesome.create(FontAwesome.Glyph.INFO).color(Color.color(0, 0, 0, 0.87)));
            aboutItem.setOnAction(event -> {
                Stage stage = new Stage();
                stage.setTitle("About Polar Upgrade Tool");
                stage.setResizable(false);
                AboutScene aboutScene = new AboutScene();
                stage.setScene(aboutScene.getScene());
                stage.show();
            });

            fileMenu.getItems().addAll(helpItem, checkUpdateItem, aboutItem);
            boxPane.getChildren().add(0, menuBar);
        }

        public void setRootScene(Scene scene) {
            this.scene = scene;
        }

        @FXML
        protected void openFolderChooserDialog() {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            selectedFolder = directoryChooser.showDialog(scene.getWindow());
            if (selectedFolder != null) {
                projectLocationTextField.setText(selectedFolder.getAbsolutePath());
                if(SecurityUtil.checkIsPolarBased(selectedFolder.getAbsolutePath())) {
                    updateBtn.setVisible(true);
                } else {
                    showSecurityInfoDialog(selectedFolder.getAbsolutePath());
                }
            }
        }

        @FXML
        protected void onHyperlinkClicked() {
            UpgradeTool.getHostService().showDocument(UpgradeTool.GITHUB_REPO);
        }

        @Override
        public void onProjectDetected(String applicationName, String applicationPackage, String applicationVersionName, String applicationVersionCode) {
            if (Platform.isFxApplicationThread()) {
                logMessages.add("Found Project: " + applicationName + " [" + applicationPackage + "], Version Name: " + applicationVersionName + ", Version Code: " + applicationVersionCode);
                messageListView.scrollTo(logMessages.size() - 1);
                maskerPane.setText("Updating " + Main.USER_APPNAME);
            } else {
                Platform.runLater(() -> onProjectDetected(applicationName, applicationPackage, applicationVersionName, applicationVersionCode));
            }
        }

        @Override
        public void onErrorOccurred(String errorMessage) {
            if (Platform.isFxApplicationThread()) {
                maskerPane.setVisible(false);
                showErrorDialog(errorMessage);
            } else {
                Platform.runLater(() -> {
                    maskerPane.setVisible(false);
                    showErrorDialog(errorMessage);
                });
            }
        }

        @Override
        public void onArchiveDownloadStarted(String sizeStr) {
            if (Platform.isFxApplicationThread()) {
                onStatusUpdate(String.format("Downloading a ZIP of Polar's latest code (%s)...", sizeStr));
            } else {
                Platform.runLater(() -> {
                    onArchiveDownloadStarted(sizeStr);
                });
            }
        }

        @Override
        public void onArchiveDownloadProgress(String progressStr) {
            if (Platform.isFxApplicationThread()) {
                maskerPane.setText("Downloading latest source\n" + progressStr);
            } else {
                Platform.runLater(() -> onArchiveDownloadProgress(progressStr));
            }
        }

        @Override
        public void onArchiveDownloadSuccess() {
            if (Platform.isFxApplicationThread()) {
                onStatusUpdate("Download complete!");
                maskerPane.setText("Migrating resources...");
            } else {
                Platform.runLater(() -> {
                    onArchiveDownloadSuccess();
                    maskerPane.setText("Migrating resources...");
                });
            }
        }

        @Override
        public void onArchiveDownloadFailed(String errorMessage) {
            if (Platform.isFxApplicationThread()) {
                showErrorDialog(errorMessage);
                maskerPane.setVisible(false);
            } else {
                Platform.runLater(() -> onArchiveDownloadFailed(errorMessage));
            }
        }

        @Override
        public void onStatusUpdate(String statusMessage) {
            if (Platform.isFxApplicationThread()) {
                logMessages.add(statusMessage);
                messageListView.scrollTo(logMessages.size() - 1);
            } else {
                Platform.runLater(() -> onStatusUpdate(statusMessage));
            }
        }

        @Override
        public void onUpdateSuccessful() {
            if (Platform.isFxApplicationThread()) {
                maskerPane.setVisible(false);
                showUpdateSuccessDialog();
            } else {
                Platform.runLater(() -> {
                    maskerPane.setVisible(false);
                    showUpdateSuccessDialog();
                });
            }
        }
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Polar Upgrade Tool: Error");
        alert.setHeaderText("An error occurred!");
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(550, 270);
        alert.setResizable(true);
        alert.setOnHiding(event -> {
//            Platform.exit();
//            System.exit(0);
            logMessages.clear();
            if (updateBtn != null)
                updateBtn.setVisible(true);
        });
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
//            Platform.exit();
//            System.exit(0);
            logMessages.clear();
            if (updateBtn != null)
                updateBtn.setVisible(true);
        }
    }

    private void showUpdateSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Polar Upgrade Tool: Info");
        alert.setHeaderText("Update successful!");
        alert.setContentText(Main.USER_APPNAME + " is now up to date! Your configuration has been restored.\n\n" +
                "Find any issues? Please report them on GitHub. You can undo changes made by this tool either " +
                "using the backup ZIP archive placed in your project directory, or by using the following Git " +
                "commands:\n\ngit add -A\ngit stash save\ngit stash drop");
        alert.getDialogPane().setPrefSize(550, 360);
        alert.setOnHiding(event -> {
            Platform.exit();
            System.exit(0);
        });
        alert.setResizable(true);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }

    public void showSecurityInfoDialog(String path) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Polar Upgrade Tool: Warning");
        alert.setHeaderText("WARNING: Unable to find Polar based Project!");
        alert.getDialogPane().setPrefSize(550, 360);
        alert.setContentText("The project located at:\n\n" +
                path + "\n\n" +
                "doesn't seem to be a project based on Polar.\n" +
                "Proceeding with the upgrade might destroy your project setup and build system.\n" +
                "Please make sure to backup your current data before continuing,\n" +
                "you proceed at your own risk!");
        alert.setResizable(false);

        ButtonType confirmBtn = new ButtonType("Got it, Continue anyway!", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Abort Process", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().removeAll(alert.getButtonTypes());
        alert.getButtonTypes().addAll(confirmBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if(result.get() == confirmBtn) {
            updateBtn.setVisible(true);
        } else {
            updateBtn.setVisible(false);
        }

    }

}
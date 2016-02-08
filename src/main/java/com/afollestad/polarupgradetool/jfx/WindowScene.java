package com.afollestad.polarupgradetool.jfx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

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

    private class WindowSceneController extends VBox implements UICallback {

        private Scene scene;
        private File selectedFolder;
        private InterfaceUpdateThread interfaceUpdateThread;
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

        WindowSceneController() {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/applicationUI.fxml"));
                fxmlLoader.setRoot(this);
                fxmlLoader.setController(this);
                fxmlLoader.load();
                projectLocationTextField.setEditable(false);
                logMessages = FXCollections.observableArrayList();
                messageListView.setItems(logMessages);
                messageListView.setEditable(false);
                updateBtn.setVisible(false);
                updateBtn.setOnAction(event -> {
                    //Main.upgrade(selectedFolder.getAbsolutePath(), WindowSceneController.this);
                    interfaceUpdateThread.start();
                });

                WindowScene.this.updateBtn = updateBtn;
                copyrightLabel.setText(String.format("(c) %d Polar Upgrade Tool", Calendar.getInstance().get(Calendar.YEAR)));
            } catch (IOException io) {
                io.printStackTrace();
            }
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
                interfaceUpdateThread = new InterfaceUpdateThread(selectedFolder.getAbsolutePath(), this);
                updateBtn.setVisible(true);
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
            } else {
                Platform.runLater(() -> logMessages.add("Found Project: " + applicationName + " [" + applicationPackage + "], Version Name: " + applicationVersionName + ", Version Code: " + applicationVersionCode));
            }
        }

        @Override
        public void onErrorOccurred(String errorMessage) {
            if (Platform.isFxApplicationThread()) {
                showErrorDialog(errorMessage);
            } else {
                Platform.runLater(() -> showErrorDialog(errorMessage));
            }
        }

        @Override
        public void onArchiveDownloadFailed(String errorMessage) {
            if (Platform.isFxApplicationThread()) {
                showErrorDialog(errorMessage);
            } else {
                Platform.runLater(() -> showErrorDialog(errorMessage));
            }
        }

        @Override
        public void onStatusUpdate(String statusMessage) {
            if (Platform.isFxApplicationThread()) {
                logMessages.add(statusMessage);
                messageListView.scrollTo(logMessages.size() - 1);
            } else {
                Platform.runLater(() -> logMessages.add(statusMessage));
            }
        }

        @Override
        public void onUpdateSuccessful() {
            if (Platform.isFxApplicationThread()) {
                showUpdateSuccessDialog();
            } else {
                Platform.runLater(WindowScene.this::showUpdateSuccessDialog);
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
            if (updateBtn != null)
                updateBtn.setVisible(true);
        });
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
//            Platform.exit();
//            System.exit(0);
            if (updateBtn != null)
                updateBtn.setVisible(true);
        }
    }

    private void showUpdateSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Polar Upgrade Tool: Info");
        alert.setHeaderText("Update successful!");
        alert.setContentText("Polar is up to date.\nYour config has been restored.");
        alert.getDialogPane().setPrefSize(550, 270);
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
}
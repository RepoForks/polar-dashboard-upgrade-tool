package com.afollestad.polarupgradetool.jfx;

import com.afollestad.polarupgradetool.utils.ManifestUtils;
import com.afollestad.polarupgradetool.utils.UpdateUtils;
import com.afollestad.polarupgradetool.utils.UrlUtils;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.util.Optional;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public class UpgradeTool extends Application implements UpdateUtils.UpdateCallback {

    public static final String GITHUB_REPO = "https://github.com/afollestad/polar-dashboard-upgrade-tool";

    private static HostServices hostServices;
    private static UpgradeTool upgradeTool;

    private boolean silent = true;

    @Override
    public void start(Stage stage) throws Exception {
        upgradeTool = this;
        hostServices = getHostServices();

        stage.setTitle("Polar Dashboard Upgrade Tool");
        stage.setResizable(false);

        WindowScene windowScene = new WindowScene();
        Scene scene = windowScene.getScene();

        stage.setScene(scene);
        stage.show();

        updateCheck();
    }

    public static HostServices getHostService() {
        return hostServices;
    }

    public static UpgradeTool getInstance() {
        return upgradeTool;
    }

    public void updateCheck() {
        UpdateUtils.checkForUpdate(this).execute();
    }

    @Override
    public void onUpdateCheckStarted() {
        if(Platform.isFxApplicationThread()) {
            System.out.println("Update check started");
        } else {
            Platform.runLater(this::onUpdateCheckStarted);
        }
    }

    @Override
    public void onUpdateCheckFailed(String errorMsg) {
        if(Platform.isFxApplicationThread()) {
            if(!silent) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Polar Upgrade Tool: Error");
                alert.setHeaderText("Unable to check for update!");
                alert.setContentText("An error occurred while trying to resolve the latest version " +
                        "of Polar Upgrade Tool:\n\n" + errorMsg +
                        "\n\nPlease make sure you're connected to the internet and try again,\n" +
                        "otherwise head over to the GitHub Repository.");
                alert.getDialogPane().setPrefSize(550, 360);
                alert.setResizable(false);
                alert.showAndWait();
            }
            silent = false;
        } else Platform.runLater(() -> onUpdateCheckFailed(errorMsg));
    }

    @Override
    public void onUpdateCheckFinished(boolean isSame, String currentVersion, String latestVersion) {
        if(Platform.isFxApplicationThread()) {
            if(isSame) {
                if(!silent) {
                    //show a notification to give visual feedback that the operation was successful
                    Notifications.create()
                            .title("Everything is up to date")
                            .text("Current Version: " + currentVersion + "\nLatest Version: " + latestVersion)
                            .hideAfter(Duration.seconds(3))
                            .position(Pos.TOP_RIGHT)
                            .showInformation();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Polar Upgrade Tool: Information");
                alert.setHeaderText("Update available:\n" + latestVersion);
                alert.setContentText("A new version for Polar Upgrade Tool is available!\n\nCurrent Version: " +
                        currentVersion +
                        "\nLatest Version: " +
                        latestVersion + "\n\nNote: It's always recommended to use the latest version.");
                alert.getDialogPane().setPrefSize(500, 260);
                alert.setResizable(false);
                ButtonType okBtn = new ButtonType("Update");
                ButtonType ignBtn = new ButtonType("Skip");

                alert.getButtonTypes().removeAll(alert.getButtonTypes());
                alert.getButtonTypes().addAll(ignBtn, okBtn);

                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == okBtn) {
                    UrlUtils.openReleasePage();
                    Platform.exit();
                    System.exit(0);
                }

            }
            silent = false;
        } else Platform.runLater(() -> onUpdateCheckFinished(isSame, currentVersion, latestVersion));
    }

}
package com.afollestad.polarupgradetool.jfx;

import com.afollestad.polarupgradetool.utils.ManifestUtils;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public class UpgradeTool extends Application {

    public static final String GITHUB_REPO = "https://github.com/afollestad/polar-dashboard-upgrade-tool";

    private static HostServices hostServices;

    @Override
    public void start(Stage stage) throws Exception {
        hostServices = getHostServices();

        stage.setTitle("Polar Dashboard Upgrade Tool");
        stage.setResizable(false);

        WindowScene windowScene = new WindowScene();
        Scene scene = windowScene.getScene();

        stage.setScene(scene);
        stage.show();

        ManifestUtils.getGithubApplicationVersion();
    }

    public static HostServices getHostService() {
        return hostServices;
    }
}
package com.afollestad.polarupgradetool.jfx;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public class UpgradeTool extends Application {

    public static final String GITHUB_REPO = "https://github.com/afollestad/polar-dashboard-upgrade-tool";

    private static HostServices hostServices;
    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        hostServices = getHostServices();
        this.stage = stage;
        stage.setTitle("Polar Dashboard Upgrade Tool");
        stage.setResizable(false);
        WindowScene windowScene = new WindowScene();
        stage.setScene(windowScene.getScene());
        stage.show();
    }

    public static HostServices getHostService() {
        return hostServices;
    }

}

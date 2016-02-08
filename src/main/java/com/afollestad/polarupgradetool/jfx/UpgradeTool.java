package com.afollestad.polarupgradetool.jfx;

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

    //main menu bar and menu items
    private MenuBar menuBar;
    private Menu fileMenu;


    @Override
    public void start(Stage stage) throws Exception {
        hostServices = getHostServices();

        stage.setTitle("Polar Dashboard Upgrade Tool");
        stage.setResizable(false);

        WindowScene windowScene = new WindowScene();
        Scene scene = windowScene.getScene();

        createMenuBar();
        ((VBox) scene.getRoot()).getChildren().add(0,menuBar);

        stage.setScene(scene);
        stage.show();
    }

    private void createMenuBar() {
        //create the menuBar
        menuBar = new MenuBar();
        //Create the menuItems
        fileMenu = new Menu("Menu");
        menuBar.getMenus().addAll(fileMenu);
        MenuItem helpItem = new MenuItem("Help / Usage");
        helpItem.setOnAction(event -> {
        });

        MenuItem checkUpdateItem = new MenuItem("Check for update");
        checkUpdateItem.setOnAction(event -> {
        });

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> {
            Stage stage = new Stage();
            stage.setTitle("About Polar Update Tool");
            stage.setResizable(false);
            AboutScene aboutScene = new AboutScene();
            stage.setScene(aboutScene.getScene());
            stage.show();
        });

        fileMenu.getItems().addAll(helpItem, checkUpdateItem, aboutItem);
    }

    public static HostServices getHostService() {
        return hostServices;
    }
}
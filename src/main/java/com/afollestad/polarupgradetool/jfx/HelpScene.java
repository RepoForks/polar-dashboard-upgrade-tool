package com.afollestad.polarupgradetool.jfx;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class HelpScene {

    private final Scene scene;
    private final HelpSceneController helpSceneController;

    public HelpScene() {
        helpSceneController = new HelpSceneController();
        scene = new Scene(helpSceneController, 750, 500);
        helpSceneController.setRootScene(scene);
    }

    public Scene getScene() {
        return scene;
    }

    private class HelpSceneController extends VBox {

        private Scene scene;

        private final WebView webView;
        private final WebEngine webEngine;

        HelpSceneController() {
            webView = new WebView();
            webEngine = webView.getEngine();
            getChildren().addAll(webView);
            webEngine.load("https://github.com/PDDStudio/polar-dashboard-upgrade-tool/wiki");
        }

        void setRootScene(Scene scene) {
            this.scene = scene;
        }

        @Override protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(webView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
        }

        @Override protected double computePrefWidth(double height) {
            return 750;
        }

        @Override protected double computePrefHeight(double width) {
            return 500;
        }

    }

}

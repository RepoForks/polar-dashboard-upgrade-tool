package com.afollestad.polarupgradetool.jfx;

import com.afollestad.polarupgradetool.utils.ManifestUtils;
import com.afollestad.polarupgradetool.utils.UrlUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class AboutScene {

    private final Scene scene;
    private final AboutSceneController aboutSceneController;

    public AboutScene() {
        aboutSceneController = new AboutSceneController();
        scene = new Scene(aboutSceneController);
        aboutSceneController.setRootScene(scene);
    }

    public Scene getScene() {
        return scene;
    }

    public AboutSceneController getAboutSceneController() {
        return aboutSceneController;
    }

    private class AboutSceneController extends VBox {

        private Scene scene;

        @FXML private Label projectVersionLabel;

        AboutSceneController() {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/aboutUI.fxml"));
                fxmlLoader.setRoot(this);
                fxmlLoader.setController(this);
                fxmlLoader.load();
                projectVersionLabel.setText("Version " + ManifestUtils.getApplicationVersion(getClass()));
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        public void setRootScene(Scene scene) {
            this.scene = scene;
        }

        @FXML
        protected void openGitProfileAidan()  {
            UrlUtils.openAfollestadGithubPage();
        }

        @FXML
        protected void openGitProfilePatrick() {
            UrlUtils.openPddstudioGithubPage();
        }

        @FXML
        protected void openGitPUT() {
            UrlUtils.openPolarUpgradeToolPage();
        }

        @FXML
        protected void openGitPolar()  {
            UrlUtils.openPolarPage();
        }

    }

}

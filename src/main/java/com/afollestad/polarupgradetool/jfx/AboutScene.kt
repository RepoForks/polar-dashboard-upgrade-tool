package com.afollestad.polarupgradetool.jfx

import com.afollestad.polarupgradetool.utils.ManifestUtils
import com.afollestad.polarupgradetool.utils.UrlUtils
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox

import java.io.IOException

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
class AboutScene {

    var scene: Scene
    private val aboutSceneController: AboutSceneController

    init {
        aboutSceneController = AboutSceneController()
        scene = Scene(aboutSceneController)
        aboutSceneController.setRootScene(scene)
    }

    private inner class AboutSceneController internal constructor() : VBox() {

        @FXML private val projectVersionLabel: Label? = null

        init {
            try {
                val fxmlLoader = FXMLLoader(javaClass.getResource("/aboutUI.fxml"))
                fxmlLoader.setRoot(this)
                fxmlLoader.setController(this)
                fxmlLoader.load<Any>()
                projectVersionLabel!!.text = "Version " + ManifestUtils.getApplicationVersion(javaClass)
            } catch (io: IOException) {
                io.printStackTrace()
            }

        }

        fun setRootScene(scene: Scene) {
            this@AboutScene.scene = scene
        }

        @FXML
        protected fun openGitProfileAidan() {
            UrlUtils.openAfollestadGithubPage()
        }

        @FXML
        protected fun openGitProfilePatrick() {
            UrlUtils.openPddstudioGithubPage()
        }

        @FXML
        protected fun openGitPUT() {
            UrlUtils.openPolarUpgradeToolPage()
        }

        @FXML
        protected fun openGitPolar() {
            UrlUtils.openPolarPage()
        }
    }
}
package com.afollestad.polarupgradetool.utils;

import com.afollestad.polarupgradetool.jfx.UpgradeTool;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class UrlUtils {

    private static final String URL_GITHUB_PUT = "https://github.com/afollestad/polar-dashboard-upgrade-tool";
    private static final String URL_GITHUB_POLAR = "https://github.com/afollestad/polar-dashboard";
    private static final String URL_GITHUB_AFOLLESTAD = "https://github.com/afollestad";
    private static final String URL_GITHUB_PDDSTUDIO = "https://github.com/PDDStudio";

    public static void openPolarUpgradeToolPage() {
        UpgradeTool.getHostService().showDocument(URL_GITHUB_PUT);
    }

    public static void openPolarPage() {
        UpgradeTool.getHostService().showDocument(URL_GITHUB_POLAR);
    }

    public static void openAfollestadGithubPage() {
        UpgradeTool.getHostService().showDocument(URL_GITHUB_AFOLLESTAD);
    }

    public static void openPddstudioGithubPage() {
        UpgradeTool.getHostService().showDocument(URL_GITHUB_PDDSTUDIO);
    }

}

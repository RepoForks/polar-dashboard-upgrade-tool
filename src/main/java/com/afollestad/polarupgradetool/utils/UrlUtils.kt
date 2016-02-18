package com.afollestad.polarupgradetool.utils

import com.afollestad.polarupgradetool.jfx.UpgradeTool

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
object UrlUtils {

    private val URL_GITHUB_PUT = "https://github.com/afollestad/polar-dashboard-upgrade-tool"
    private val URL_GITHUB_POLAR = "https://github.com/afollestad/polar-dashboard"
    private val URL_GITHUB_AFOLLESTAD = "https://github.com/afollestad"
    private val URL_GITHUB_PDDSTUDIO = "https://github.com/PDDStudio"
    private val URL_GITHUB_WIKI = "https://github.com/PDDStudio/polar-dashboard-upgrade-tool/wiki/Polar-Dashboard-Upgrade-Tool---Wiki"
    private val URL_GITHUB_RELEASES = "https://github.com/afollestad/polar-dashboard-upgrade-tool/releases"

    fun openPolarUpgradeToolPage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_PUT)
    }

    fun openPolarPage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_POLAR)
    }

    fun openAfollestadGithubPage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_AFOLLESTAD)
    }

    fun openPddstudioGithubPage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_PDDSTUDIO)
    }

    fun openWikiPage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_WIKI)
    }

    fun openReleasePage() {
        UpgradeTool.hostService?.showDocument(URL_GITHUB_RELEASES)
    }
}
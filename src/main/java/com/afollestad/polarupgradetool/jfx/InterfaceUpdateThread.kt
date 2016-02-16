package com.afollestad.polarupgradetool.jfx

import com.afollestad.polarupgradetool.Main

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
class InterfaceUpdateThread(internal val location: String, internal val uiCallback: UICallback) : Thread(), Runnable {

    override fun run() {
        Main.upgrade(location, uiCallback)
    }
}
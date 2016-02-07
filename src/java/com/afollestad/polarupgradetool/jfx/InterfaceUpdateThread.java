package com.afollestad.polarupgradetool.jfx;

import com.afollestad.polarupgradetool.Main;

/**
 * Project : polar-dashboard-upgrade-tool
 * Author : pddstudio
 * Year : 2016
 */
public class InterfaceUpdateThread extends Thread implements Runnable {

    final String location;
    final UICallback uiCallback;

    public InterfaceUpdateThread(String location, UICallback uiCallback) {
        this.location = location;
        this.uiCallback = uiCallback;
    }

    @Override
    public void run() {
        Main.upgrade(location, uiCallback);
    }

}

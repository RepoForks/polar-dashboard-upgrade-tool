package com.afollestad.polarupgradetool.utils;

import com.afollestad.polarupgradetool.Main;

import java.io.File;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class SecurityUtil {

    private static final String DEV_CUSTOM = "dev_customization.xml";
    private static final String DEV_THEMING = "dev_theming.xml";

    public static boolean checkIsPolarBased(String basePath) {
        File resources = new File(basePath + Main.getResourcesDir());
        if(!resources.exists() || !resources.isDirectory() || resources.listFiles() == null || resources.listFiles().length <= 0) return false;
        for(File file : resources.listFiles()) {
            if(file.getName().equals(DEV_CUSTOM) || file.getName().equals(DEV_THEMING)) return true;
        }
        return false;
    }

}

package com.afollestad.polarupgradetool.utils

import com.afollestad.polarupgradetool.Main

import java.io.File

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
object SecurityUtil {

    private val DEV_CUSTOM = "dev_customization.xml"
    private val DEV_THEMING = "dev_theming.xml"

    fun checkIsPolarBased(basePath: String): Boolean {
        val resources = File(basePath + Main.VALUES_FOLDER_PATH)
        if (!resources.exists() || !resources.isDirectory || resources.listFiles() == null || resources.listFiles()!!.size <= 0) return false
        for (file in resources.listFiles()!!) {
            if (file.name == DEV_CUSTOM || file.name == DEV_THEMING) return true
        }
        return false
    }
}
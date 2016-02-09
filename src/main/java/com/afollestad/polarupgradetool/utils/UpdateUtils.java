package com.afollestad.polarupgradetool.utils;

import org.apache.maven.model.Model;

/**
 * Project : polarupgradetool
 * Author : pddstudio
 * Year : 2016
 */
public class UpdateUtils {

    public interface UpdateCallback {
        void onUpdateCheckStarted();
        void onUpdateCheckFailed(String errorMsg);
        void onUpdateCheckFinished(boolean isSame, String currentVersion, String latestVersion);
    }

    private final UpdateCallback updateCallback;

    private UpdateUtils(UpdateCallback updateCallback) {
        this.updateCallback = updateCallback;

    }

    public static UpdateUtils checkForUpdate(UpdateCallback updateCallback) {
        return new UpdateUtils(updateCallback);
    }

    public void execute() {
        new UpdateThread().start();
    }

    private class UpdateThread extends Thread implements Runnable {

        @Override
        public void run() {
            updateCallback.onUpdateCheckStarted();

            Model pom = ManifestUtils.getRemoteApplicationModel();
            if(pom == null) {
                updateCallback.onUpdateCheckFailed("Unable to resolve external pom model.");
                this.interrupt();
            } else {

                if(!isInterrupted()) {
                    String currentVersion = ManifestUtils.getApplicationVersion(UpdateUtils.class);
                    String externalVersion = pom.getVersion();
                    updateCallback.onUpdateCheckFinished(currentVersion.toUpperCase().equals(externalVersion.toUpperCase()), currentVersion, externalVersion);
                }

            }

        }

    }

}

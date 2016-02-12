# Polar Upgrade Tool

<img src="https://raw.githubusercontent.com/afollestad/polar-dashboard-upgrade-tool/master/images/showcase.JPG"
    style="width: 600px; height: auto;" width="600px"/>

PUT allows you to automatically migrate your icon packs to the latest versions of Polar, without any manual copying. **This project does not migrate your project to Polar from other dashboard templates, it upgrades existing Polar-using icon packs.**

See [Polar's main repository](https://github.com/afollestad/polar-dashboard) for information about Polar.

PUT was designed and developed by [Aidan Follestad](https://github.com/afollestad) and
[Patrick J](https://github.com/PDDStudio). The icon was designed by [Anthony Nguyen](https://plus.google.com/+AHNguyen).

# Download & Wiki
You can find the latest version of PUT's packaged binary in the [Release Page of this Repository](https://github.com/afollestad/polar-dashboard-upgrade-tool/releases).

In case you need more information about how to use this Tool head over to the [Wiki Page](https://github.com/PDDStudio/polar-dashboard-upgrade-tool/wiki/Polar-Dashboard-Upgrade-Tool---Wiki).

There is also some information about the upgrade tool on [Polar's Web Guide](http://afollestad.github.io/polar-dashboard/upgrades.html).

The Wiki should guide people which are not common with technical/development stuff through the whole process to get their Polar-using icon pack updated.

# Building and Running

### Windows

From the project root:

```Gradle
build-and-deploy.bat
```

This will create a JAR in the `target` folder that you can execute.

### Unix (OSX) and Linux

From the project root:

```Gradle
./build-and-deploy.sh
```

This will create a JAR in the `target` folder that you can execute.

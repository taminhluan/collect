# collect

The KoBo fork of this project is not actively maintained. For the latest ODK Collect source, please visit [`opendatakit/collect` on Github](https://github.com/opendatakit/collect).

It is the ODK 1.0 Android application.

The developer [wiki](https://github.com/opendatakit/opendatakit/wiki) (including release notes) and
[issues tracker](https://github.com/opendatakit/opendatakit/issues) are located under
the [**opendatakit**](https://github.com/opendatakit/opendatakit) project.

The Google group for software engineering questions is: [opendatakit-developers@](https://groups.google.com/forum/#!forum/opendatakit-developers)

## Setting up your environment

This project depends upon the gradle-config and google-play-services projects


        |-- odk
            |-- gradle-config
            |-- google-play-services
            |-- collect

The `gradle-config` project should be checked out at the tag number declared at the 
top of the `collect/settings.gradle` file.

The `google-play-services` project should be pulled and at the tip.

Then, import the `collect/build.gradle` file into Android Studio.

# KoBo specific build suggestions

1. [Install IntelliJ IDEA.](https://www.jetbrains.com/idea/features/android.html)
1. [Configure IDEA with the Android SDK.](https://www.jetbrains.com/idea/help/getting-started-with-android-development.html)
1. Import the `collect` project into IDEA either by root directory or by repository URL.
1. From the menu, click `Build` -> `Generate Signed APK...`
1. Point IDEA to the `kobo-android.keystore` file, enter `kobotoolbox` as the `Key alias`, and provide both the `Key store password` and the `Key password`.
1. Select the `kobo` build flavor.
1. Build.
1. Note the likely missuse of an apostrophe in the response message `Signed APK's generated successfully.`
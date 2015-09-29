# Build instructions
1. [Install IntelliJ IDEA.](https://www.jetbrains.com/idea/features/android.html)
2. [Configure IDEA with the Android SDK.](https://www.jetbrains.com/idea/help/getting-started-with-android-development.html)
3. Import the `collect` project into IDEA either by root directory or by repository URL.
4. From the menu, click `Build` -> `Generate Signed APK...`
5. Point IDEA to the `kobo-android.keystore` file, enter `kobotoolbox` as the `Key alias`, and provide both the `Key store password` and the `Key password`.
6. Select the `kobo` build flavor.
7. Build.
8. Note the likely missuse of an apostrophe in the response message `Signed APK's generated successfully.`
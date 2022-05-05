# MI2-Utilities Java

- [English](README.md) | [中文](README_zh.md)

Powerful utilities supporting vanilla. For destined players.

Early JS version: [BlackDeluxeCat/MI2-Utilities](https://github.com/BlackDeluxeCat/MI2-Utilities)

Install:

- Method 1(recommended): Use "Import from github" in game, with "BlackDeluxeCat/MI2-Utilities-Java"
- Method 2: Get the latest release, download "MI2-Utilities Java.jar". Then use "Import from  a file" in game, with the downloaded artifact.
- Method 3: Reinstall if you have installed before.

Group(zh_cn) number: 875035496

Unable to localize for other languages, but accepts pr.

## Features(0.8.3)

### ui

#### MI2UI
- Functions: one-click planning rebuild, distribution building reveal.
- AI-assist: auto mining, auto construct plans, auto self-repair, auto aiming.
- The options popup of this window contains most of MI2U's options.

#### Core Info
- Vanilla core info ++, resource line chart.
- Unit counter.
- Power net bars & net building analyse.
- Switch to view other teams.

#### MindowMap
- Renderer based on vanilla.
- Sensible zoomed to full map.
- Player labels.
- Clicking pans camera (desktop only)

#### Map Info
- Most of the map rules.
- Wave preview, force skipwave.
- Wave health bars.

#### Logic Helper
- Sort and fast typing var names, using delimiter. 
- Search & Replace
- Cut & Copy, and Paste

#### Emoji
- Emoji book.

### Mindow2
A draggable window. On the title bar, you can open options dialog, help page. For details, see the help pages of each Mindow2.

Position, snap, etc. can be saved through options.

### Other
- Smooth player cursors.
- Unit hp bar, logic lines, pathfind line.
- Disable wrecks, units, bullets rendering.
- Modify block stat bars.
- Add a set of map filters, featuring composable coordinate transformations.
- 99.5% of changes can be turned off with options.
- The MI2U option is stored in the directory $Mindustry/mods/MI2U_Settings/.

# Gen From Mindustry Java Mod Template
A Java Mindustry mod template that works on Android and PC. The Kotlin version of this mod can be seen [here](https://github.com/Anuken/MindustryKotlinModTemplate).

## Building for Desktop Testing

1. Install JDK **17**.
2. Run `gradlew jar` [1].
3. Your mod jar will be in the `build/libs` directory. **Only use this version for testing on desktop. It will not work with Android.**
To build an Android-compatible version, you need the Android SDK. You can either let Github Actions handle this, or set it up yourself. See steps below.

## Building through Github Actions

This repository is set up with Github Actions CI to automatically build the mod for you every commit. This requires a Github repository, for obvious reasons.
To get a jar file that works for every platform, do the following:
1. Make a Github repository with your mod name, and upload the contents of this repo to it. Perform any modifications necessary, then commit and push. 
2. Check the "Actions" tab on your repository page. Select the most recent commit in the list. If it completed successfully, there should be a download link under the "Artifacts" section. 
3. Click the download link (should be the name of your repo). This will download a **zipped jar** - **not** the jar file itself [2]! Unzip this file and import the jar contained within in Mindustry. This version should work both on Android and Desktop.

## Building Locally

Building locally takes more time to set up, but shouldn't be a problem if you've done Android development before.
1. Download the Android SDK, unzip it and set the `ANDROID_HOME` environment variable to its location.
2. Make sure you have API level 30 installed, as well as any recent version of build tools (e.g. 30.0.1)
3. Add a build-tools folder to your PATH. For example, if you have `30.0.1` installed, that would be `$ANDROID_HOME/build-tools/30.0.1`.
4. Run `gradlew deploy`. If you did everything correctlly, this will create a jar file in the `build/libs` directory that can be run on both Android and desktop. 

## Adding Dependencies

Please note that all dependencies on Mindustry, Arc or its submodules **must be declared as compileOnly in Gradle**. Never use `implementation` for core Mindustry or Arc dependencies. 

- `implementation` **places the entire dependency in the jar**, which is, in most mod dependencies, very undesirable. You do not want the entirety of the Mindustry API included with your mod.
- `compileOnly` means that the dependency is only around at compile time, and not included in the jar.

Only use `implementation` if you want to package another Java library *with your mod*, and that library is not present in Mindustry already.

--- 

*[1]* *On Linux/Mac it's `./gradlew`, but if you're using Linux I assume you know how to run executables properly anyway.*  
*[2]: Yes, I know this is stupid. It's a Github UI limitation - while the jar itself is uploaded unzipped, there is currently no way to download it as a single file.*

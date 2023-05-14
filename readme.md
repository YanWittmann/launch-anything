![Example search results](doc/img/barExample.gif)

### _A search bar for anything you need_

Use this application to launch a variety of processes on your system.  
Currently only supports Windows, but might be expanded to support other platforms. This is because the global key
detector is based on the Windows API.

Note: The documents seen on this branch always relates to the current beta version state of the application. To view the
documentation for the latest stable version, select it's tagged version above.

## Features

- A search bar available from anywhere with only two keystrokes
- Open files and directories
- Open URLs in the browser
- Copy text to the clipboard
- Tile Generators: generate tiles for files in a given directory
- Runtime Tiles (see **[this document](doc/how-to.md)** for more information)
    - Integrated [Menter Interpreter](https://yanwittmann.github.io/menter-lang-docs/)! Run Menter code directly from the
      search bar
    - Display charts for mathematical functions/expressions
    - Copy system information like the local IP address
    - Convert between a variety of units
    - Calculate aspect ratios
    - Convert timestamps between different time zones
    - Use the WolframAlpha API to evaluate expressions
    - Convert between number systems
    - Use the `I'm feeling lucky` functionality to open any webpage
    - Directly go to a wikipedia article
- Plugins: add new functionality to the search bar!
  - **[Runtime Tile Plugins](doc/runtime-tile-plugins.md)**
  - **[Tile Action Plugins](doc/tile-action-plugins.md)**
- Disable (timeout) the bar for a given amount of time
- Cloud Tiles: Store your tiles in the cloud to access them from any device. **[More information](doc/cloud-tiles.md)**.

## Quick Setup

- Download the **[latest version](https://github.com/YanWittmann/launch-anything/releases)** and place the jar file in
  any directory on your system that you want the base directory to be
- Start the jar file with at least Java 8
- Double-Tap `command` or `ctrl` to open the search bar
- Type in some search terms and navigate the results using the arrow keys
- Press `enter` to execute the topmost result tile

For a more complete how-to, see this **[document](doc/how-to.md)**.

## Bar Settings

After having started the application, open up the search bar by double-tapping `command` or `ctrl`, type in `settings`
and hit enter. This will open up a webserver and the corresponding web page in your browser. A more detailed description
of this settings page can be found **[here](doc/how-to.md)**.

## Trouble Shooting

If the activation key (double `ctrl` or `command`) is not working, try changing the activation key in the settings page
by right-clicking the tray icon in your dock and selecting `Settings`. There, in the `Settings & Help` tab, you can
change the activation key by clicking on the `activationKey` button.

Important: These documents represent the state of the latest snapshot of the LaunchAnything bar.

## Build it yourself

You will need to download maven and git to build this application. Then, run these commands:

```shell
git clone https://github.com/steos/jnafilechooser.git
cd jnafilechooser
mvn clean install
cd ..
git clone https://github.com/YanWittmann/launch-anything.git
cd launch-anything
mvn clean install
cd launch-anything-application/target
java -jar launch-anything-application-2.9-SNAPSHOT-jar-with-dependencies.jar
```

## Have fun using the LaunchAnything bar!

![LaunchAnything](doc/img/LaunchAnythingLogoDefSmall.png)

If you like this application and want to support my work, you can
**[buy me a coffee](https://www.paypal.com/paypalme/yanwittmann)**!

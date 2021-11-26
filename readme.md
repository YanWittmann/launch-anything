
![Example search results](doc/img/barExample.png)

### _A search bar for anything you need_
Use this application to launch a variety of processes on your system.

## Features

- A search bar available from anywhere with only two keystrokes
- Open files and directories
- Open URLs in the browser
- Copy text to the clipboard
- Tile Generators: generate tiles for files in a given directory

## Quick Setup

- Download the **[latest version](https://github.com/Skyball2000/launch-anything/releases)** and place the jar file in any
  directory on your system that you want the base directory to be
- Start the jar file with at least Java 8
- Double-Tap 'command' or 'ctrl' to open the search bar

## Bar Settings

After having started the application, open up the search bar by double-tapping 'command' or 'ctrl', type in 'settings'
and hit enter. This will open up a webserver and the corresponding web page in your browser. A more detailed
description of this settings page can be found **[here](doc/how-to.md)**.

## Build it yourself

To build the launch bar yourself, perform these actions:

1. Clone and install the entirety of **[steos jnafilechooser](https://github.com/steos/jnafilechooser.git)** using Maven
   (`mvn install`)
2. Build this project using Maven (`mvn package`)
3. The jar will be located in the `target` directory

### Have fun using the LaunchAnything bar!

![LaunchAnything](doc/img/LaunchAnythingLogoDefSmall.png)

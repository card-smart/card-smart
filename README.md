# Card-smart
This project contains a Javacard applet for storing secrets 
and a Java application that can communicate with the applet over secure channel.

## Overview
Building and running this project requires Java 1.8, Gradle 6.6 (probably downloaded automatically)
and Javacard that supports JC304 SDK and EC operations.

This project is based on [javacard-gradle-template](https://github.com/ph4r05/javacard-gradle-template) 
and was tested on Windows 10 and Fedora 36.

### Card-smart applet
The Card-smart applet can securely store 32-bit long secrets in form of name-value pairs.
It can list all available secret names and query them individually.

The applet provides two types of communication: unsecure and secure.
The secure communication consists of encrypted APDUs, realizing sort of secure channel over ECDH
(do not mistake it with ISO7816-4 Secure Messaging or GlobalPlatform SecureChannel interface).
The unsecure communication can use the same functionality of the applet, but the transmitted APDUs are not encrypted.
Both secure and unsecure communication need to contain PIN to authenticate the user.
The unsecure communication can be held only until the applet is initialized (and the pairing secret is created) -
after then, only the secure communication can be held.

### Card-smart tool
The Card-smart tool used for communication with the Card-smart applet in two ways:
- interactively: the Smartie mode waits for user's CLI input
- non-interactively: executing of the tool with given arguments and terminating after the completion

The tool will create all needed cryptographic keys and secrets by itself.

## Setup
After cloning the repository (`git clone --recursive git@github.com:card-smart/card-smart.git`), 
the `./gradlew` (or `./gradlew.bat`) command executed in the root directory 
will create the `gradle` daemon and build the project.

Use `./gradlew buildJavaCard  --info --rerun-tasks` to build a CAP file from the applet at 
`applet/src/main/java/applet/CardSmartApplet.java`. The CAP file is stored in `applet/build/javacard/applet.cap`.
Configuration of the applet build can be found at `applet/build.gradle`.

To build the CAP file and load it into the connected card, use `./gradlew installJavacard`.

To run the tool, use `./gradlew run`. The tool can be also compiled into so-called flatJar, runnable .jar file.
This can be done via `./gradlew clean` for cleaning the `build` directory 
and `./gradlew jar` that creates `applet/build/libs/card-smart-tool`.
When compiled this way, the tool can be executed as `java -jar applet/build/libs/card-smart-tool`.

Use `./gradlew test --info --rerun-tasks` to run the JUnit tests.

## Usage
The tool can be used in interactive and non-interactive way. Both ways require same format of arguments.
Interactive mode can be started via tool execution with no argument (eg. `java -jar card-smart-tool`), 
non-interactive mode requires arguments (eg. `java -jar card-smart-tool -c 1111 -p 0000`).

### Basic workflow
`java -jar card-smart-tool --init -f pairing_secret -p 1111` initializes applet, 
gets/stores pairing secret into `pairing_secret` file and sets card PIN from default `0000` to `1111`.

`java -jar card-smart-tool -s secret1 -i secret1.txt -f pairing_secret -p 1111`
stores secret from `secret1.txt` as `secret1` (pairing secret file required).

`java -jar card-smart-tool -v secret1 -f pairing_secret -p 1111` prints stored secret with name `secret1` in hex format.

`java -jar card-smart-tool -c 1234 -f pairing_secret -p 1111` changes PIN to `1234`.

`java -jar card-smart-tool -l -f pairing_secret -p 1234` lists all names of stored secrets.

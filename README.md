# Card-smart
This project contains a Javacard applet for storing secrets 
and a Java application that can communicate with the applet over secure channel.

## Overview
This project is based on JavacardGradleTemplate TODO odkaz. The project was tested on Windows 10 and Fedora 36.

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
After cloning the repository (`git clone --recursive https://github.com/ph4r05/javacard-gradle-template.git`), 
the `./gradlew` (or `./gradlew.bat`) command executed in the root directory 
will create the `gradle` daemon and build the project.

Use `./gradlew buildJavaCard  --info --rerun-tasks` to build a CAP file from the applet at 
`applet/src/main/java/applet/CardSmartApplet.java`. The CAP file is stored in `applet/build/javacard/applet.cap`.
Configuration of applet build can be found at `applet/build.gradle`.

To build the CAP file and load it into the connected card, use `./gradlew installJavacard`.

To run the tool, use `./gradlew run` or TODO.

Use `./gradlew test --info --rerun-tasks` to run the JUnit tests.

## Usage

# card-smart project

## card-smart-tool
A tool used to communicate with the card-smart-applet.

### Options
TODO

## card-smart-applet
The applet is used to store secrets as name-value pairs. Secrets can be accessed using a PIN.

### APDU and card responses
TODO

## Build
Opening this project in IntelliJ results in download and build of gradle stuff.
If not, use `./gradlew` or `./gradlew.bat`.

### Building CAP file
Use `./gradlew buildJavaCard  --info --rerun-tasks` to build CAP file from `applet/src/main/java/applet/MainApplet.java`.

CAP file is stored in `applet/build/javacard/applet.cap`.
Building of the applet (which applet, with what AID, ...) can be configured in `applet/build.gradle`

### Running on simulator
Use `./gradlew run` to run program from `applet/src/main/java/main/Run.java` that can communicate with applet in simulator.

### Running tests
Use `./gradlew test --info --rerun-tasks` to run tests.


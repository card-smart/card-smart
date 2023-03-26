# [PV204] Project report, phase II.
Veronika Hanulíková (492760), Kristína Hanicová (492779), Lubomír Hrbáček (493077)
## Project design
### JavaCard Applet
The applet serves as a repository for secret values accessed by their name.
We use a PIN to authenticate the user.

After the applet is loaded onto the card, the PIN is set to the default value.
The maximum length of a PIN is 10 bytes; the current implementation accepts PINs of this length
(and the task of the applet tool is to align the shorter PIN in this way). The PIN can then be changed.

#### Operations
We divide basic card operations into those that can be performed without user authentication
and those for which authentication is required. Getting all the secret names from the applet
and getting the remaining number of attempts to enter the PIN are among the operations without authentication.
Creating a new secret, retrieving the contents of an existing secret, deleting a secret, or changing a PIN requires getting 
the card into a so-called authenticated state. This is achieved by sending a PIN verification instruction to the card and the PIN in question.

A detailed description of how the applet works can be found in the
applet documentation (https://github.com/card-smart/card-smart/blob/main/doc/Applet-Specification.md).

#### Secure channel
For a secure implementation of the secure channel, we decided to use two more states on the card - initialized and uninitialized.
When the applet is loaded onto the card, the applet is uninitialized, the PIN is initially set to default,
and the applet allows all basic functionality to function.

To use the secure channel, the applet needs to be initialized. During the initialization of the applet,
we set a new asymmetric key pair and the so-called pairing secret, which is further used when opening the secure channel and deriving the secret key for encryption.
The pairing secret is generated outside of the applet and supplied via encrypted APDU.
The initialization operation must be performed in a secure environment.
At this stage, we cannot yet ensure that the tool communicates with the real card and not with the attacker.
After the initialization, the pairing secret is used for deriving the secret key and thus authentication 
of both parties (without having the pairing secret, the party cannot create right secret key and then MAC validation and encryption
fails).

After initializing the applet, a new secure channel must be created before each series of operations.
The main goal is to create a shared symmetric key. To achieve this, public asymmetric keys are exchanged between
the card and the communicating tool; each party then derives the secret using the ECDH algorithm and uses hashing
to create a symmetric key from the pairing secret, salt and the derived secret. One of these keys is used to encrypt
the communication; the other is used to compute the MAC tag to ensure integrity.

The applet itself then handles the decryption of the incoming APDU and MAC tag verification and encryption
of the outgoing response. We use the incoming MAC tag values from the other side as the IV for encryption.
If the MAC tag authentication fails while processing the incoming APDU, the secure channel is closed, and the current symmetric keys are erased.

A detailed description of secure channel can be found in the
secure channel documentation (https://github.com/card-smart/card-smart/blob/main/doc/Secure-Channel.md).

#### Encountered problems
The first problem we encountered was putting the applet safely into the initialized state.
Although we were inspired by the creation of a secure channel in other open-source applets, unfortunately,
we could not find a way to provide mutual authentication during initialization when the applet and the tool do not share any secrets for now.

Since the card offers only a limited amount of memory, we decided to limit the maximum size of stored secrets.
To ensure security, we first used the `HMACKey` object for storage, which allows up to 64 bytes to be stored.
However, this object was not supported by the card, so we had to replace it with an `AESKey` object which stores up to 32 bytes.

For now, the pairing secret cannot be changed in any way after the applet is initialized.
We would like to solve this problem by either using the PIN to reinitialize the applet and clear the
storage and set a new pairing secret or by the option to use the PIN to set a new pairing secret.

### Tool
We decided to use library apache.commons.cli for parsing the program arguments
and commands. The class CommandParser implements the CommandLineParser from
this library while using the Options() class.

The arguments can be passed to the tool in two ways:
On the same line as the tool is called, e.g.,

    ./card-smart --list

Or in the shell after the tool is run, which creates a `smartie` prompt that
will take arguments until the user types in `quit`, e.g.,

    ./card-smart

    smartie$ --list

    smartie$ quit

This was created so that users don't have to call the tool with every
instruction they want to pass to the smart card and it also saves resources as
the needed initialization of the tool is done just once.

After parsing, we perform validation of options and arguments with all the
necessary type conversion or padding to byte arrays. All parameters are saved
in one class Arguments which is passed to APDUs building functions.

The APDU functions take parameter that are only necessary for specific
operations and it does not matter if the rest of the class fields are null.
This was created so that future conversion to function callbacks would be
smooth.  Each function is responsible for building the APDU, sending it for
processing to the card and handling the card response.

In the next phase, we would like to perfect the output of the card responses and
overall usability of the tool - better exception handling and more descriptive
error prints.

## Technologies

### JavaCard API
We use the JavaCard library to implement cryptographic operations and store objects securely in the applet.

PIN storage, subsequent verification and changes of the PIN in applet are implemented via `OwnerPIN` object.
As the storage of the secret value itself we do not use only an array of bytes stored in persistent memory,
but an `AESKey` object, whose implementation should be more secure.

Implementing a secure channel requires multiple cryptographic operations, including ECDH, hashing,
MAC tag calculation, and encryption along with decryption. We use the `javacard.security` and `javacard.crypto`
libraries for these purposes.

### Gradle
We use [Javacard Gradle Template](https://github.com/ph4r05/javacard-gradle-template) as the skeleton for our project.
This template based on Gradle tasks provides, among other things:

- building the entire project
- building the applet into CAP file via Ant with option to choose various JC-SDKS
- installation and deletion of the built CAP file into the Javacard smartcard via GlobalPlatformPro
- together with the IntelliJ IDE running tests in united format and displaying their results in web browser

### CardTools package
We use this package (from Petr Svenda and others), mainly CardManager class, to connect the tool to the Javacard smartcard.
We got it from [JC Gradle Template Edu](https://github.com/crocs-muni/javacard-gradle-template-edu/tree/master/applet/src/test/java/cardTools).
It provides clean functions that communicates with Java API for communication with connected cards and readers.

## Current progress
* Applet is able to work with PIN, store, retrieve and delete secrets
  * After five attempts to use the wrong PIN, the storage is reset
  * The PIN is sent to the card as 10 bytes (will probably be changed to a range of 4 - 10 bytes)
  * Basic tests for functionality without secure channel
* Applet with basic functionality can be uploaded to the card
* Tool processes arguments for basic applet functionality (without initialization)
* Tool works for basic functionality of the applet - working with PIN, saving, retrieving and deleting secrets

# CardSmartApplet Specification

## Capabilites

### 1. Store name-value pairs
* `FileSystem` class
* Named records as buffers for secret data
* PIN protected access

### 2. Reading all names 
* names stored in filesystem
* no PIN protection needed
* data sent as bytes
* data in LV (length-value) structure
  * `length` of name = 1 byte
  * `value` = name

### 3. Login
* applet works with 10 byte PIN
* PIN should be sent padded to the applet
* login operation setup logged-in-status in applet filesystem
* logged-in-status is reset by removing card
* when no remaining tries left
  * secret data are erased from card
  * PIN set to default value
  
### 4. Change PIN
* checks PIN length
* needs applet to be in logged-in-state

### 5. Get secret value
* authenticate with PIN
* ask card for data related to the given name in filesystem

### 6. Store secret value
* applet receives names and secret value
* check for correct PIN and secret length

### 7. Delete secret value
* send secret name to card, which should be deleted
* get information, that such data do not exist/cannot be deleted

### 8. Secure Channel
* encryption
* MAC tags
* details in 

## Files
### CardSmartApplet.java
#### Static information
* definition of APDU instructions
* PIN related contants
  * inital PIN value = 0000000000 (in ASCII) - 10 bytes
  * max tries = 5
* storage related information
  * max record size = 32 (`AESKey`)
  * max record number = 16
  * min length of secret = 4
  * max length of secret = 32
* name policy
  * alphanumeric characters
  * min length of name = 4
  * max length of name = 10

#### Initialization
* create default OwnerPIN object
* generate new asymmetric keys
* secure channel instance
* array to store secrets

### Record.java
* name
* `AESKey` object for secret data
* checksum to detect faults (`Checksum`)

### SecureChannel.java
* implementation of secure channel capabilities

---

## APDU Specification
* CLA = `0xB0`
* **APDU STRUCTURE**
  1. _Unsecure_ = not encrypted nor MACed
      * data are sent directly over the insecure channel
      * APDU processed directly
  2. _Secure_ = encrypt-than-MAC
      * sending with special APDU, which means _we are sending encrypted data, which are along with the command_
      * encrypted data must be padded to 16B-blocks
  ```
  Unsecure APDU:
  CLA | INS | P1 | P2 | lc | data [max 255 B]

  Secure APDU:
  CLA | INS | P1 | P2 | lc | encryted data [max 234 B] | MAC tag [16 B]

  Unsecure RES:
  SW1 | SW2 | data [max 256 B]

  Secure RES:
  0x90 | 0x00 | encrypted(data [max 238] | SW1 | SW2) [max 240 B] | MAC tag [16 B]
  ```
  * when error happens during encryption, decryption, MAC creation or MAC verify, corresponding error codes are returned directly **unencrypted**

## Applet states
### Uninitialized state
* no pairing secret set
* communication not via secure channel
* trying to use initialized functionality should fail
* when applet is initialized, all previously stored data are erased

### Initialized state
* pairing secret is set and thus also secure channel can be opened
* it is not possible to perform operations without secure channel (instructions are _disabled_)

## APDU
### APDU for initialization of applet
* not via secure channel

#### Get Card EC Public Key
* works by initialized and uninitialized state

| APDU | Values  |
|------|---------|
| CLA  | `0xB0`  |
| INS  | `0x40`  |
| P1   | `0x00`  |
| P2   | `0x00`  |
| lc   | ignored |
| DATA | ignored |
| le   | ignored |

| RES        | Data field      | Info                            |
|------------|-----------------|---------------------------------|
| `0x9000`   | EC public key   | 65 B, uncompressed point format |
| `0x6B00`   | none            | undefined error                 |

#### Card Init
* setting initial PIN and pairingSecret for subsequent secure channel creation
* when init is performed, if any data are set in applet, they are reset to default state
* in this step, tool has to already have public key of the card
* if applet is **not initialized, it can be used only in unsecure communication**
  * **subsequent init erase all stored data**
* after this command, it is not possible to get applet back into uninitialized state

| APDU | Values                                              |
|------|-----------------------------------------------------|
| CLA  | `0xB0`                                              |
| INS  | `0x41`                                              |
| P0   | `0x00`                                              |
| P1   | `0x00`                                              |
| lc   | `0x81` (65 + 16 + encrypted(10 + 32))               |
| DATA | EC public key (LV encoded) + IV + encrypted payload |
| le   | ignored                                             |

| RES      | Data field | Info                                                                            |
|----------|------------|---------------------------------------------------------------------------------|
| `0x9000` | none       |                                                                                 |
| `0x6B00` | none       | error                                                                           |
| `0x6B01` | none       | when trying to initialize already initialized applet without PIN authentication |
| `0x6B03` | none       | PIN policy not satisfied                                                        |
| `0x6B04` | none       | Reset of storage failed (could not delete data)                                 |
| `0x6B07` | none       | Data length does not match (PIN length, pairing secret length, point length)    |
| `0x6A03` | none       | ECDH failed                                                                     |
| `0x6A01` | none       | Decryption failed                                                               |

#### Open Secure Channel
* works only in initialized mode
| APDU | Values                             |
| ---- | ---------------------------------- |
| CLA  | `0xB0`                             |
| INS  | `0x42`                             |
| P0   | `0x00`                             |
| P1   | `0x00`                             |
| lc   | `0x41`                             |
| DATA | public key in uncompressed form    |

| RES      | Data field  | Info                        |
|----------|-------------|-----------------------------|
| `0x9000` | `salt \ IV` | success                     |
| `0x6A00` |             | Shared secret not generated |
| `0x6A03` |             | ECDH failed                 |
| `0x6A04` |             | applet not initialized      |
| `0x6B00` |             | general error               |

#### Close Secure Channel
| APDU  | Values  |
|-------|---------|
| CLA   | `0xB0`  |
| INS   | `0x43`  |
| P0    | `0x00`  |
| P1    | `0x00`  |
| lc    | ignored |
| DATA  | ignored |

| RES      | Data field | Info                   |
|----------|------------|------------------------|
| `0x9000` |            | success                |
| `0x6A04` |            | applet not initialized |
| `0x6B00` |            | general error          |

### APDU for applet functionality
* `INS` denotes instruction code for unsecure channel
  * using `S_INS` when applet is uninitialized returns response `TODO`
* `S_INS` denotes instruction code fo secure channel
  * using `INS` when applet is initialized returns response `TODO`

#### Get Names
| APDU  | Values  |
|-------|---------|
| INS   | `0x20`  |
| S_INS | `0x30`  |
| P1    | `0x00`  |
| P2    | `0x00`  |
| lc    | `0x00`  |
| DATA  | ignored |

| RES      | Data field | Info                        |
|----------|------------|-----------------------------|
| `0x9000` |            | success                     |
| `0x6B00` |            | general error               |
| `0x6A01` |            | decryption error            |
| `0x6A02` |            | MAC error                   |
| `0x6A04` |            | applet not initialized      |
| `0x6A05` |            | applet already initialized  |
| `0x6A06` |            | encryption error            |
| `0x6A07` |            | encrypted APDU length wrong |
| `0x6B04` |            | storage error               |
| `0x6B06` |            | secret policy not satisfied |

### Get PIN Remaining Tries
| APDU  | Values  |
|-------|---------|
| INS   | `0x21`  |
| S_INS | `0x31`  |
| P1    | `0x00`  |
| P2    | `0x00`  |
| lc    | ignored |
| DATA  | ignored |

| RES      | Data field            | Info                       |
|----------|-----------------------|----------------------------|
| `0x9000` | remaining tries [1 B] |                            |
| `0x6A04` |                       | applet not initialized     |
| `0x6A05` |                       | applet already initialized |
| `0x6B00` |                       | general error              |

### PIN Verify
| APDU  | Values    |
|-------|-----------|
| INS   | `0x22`    |
| S_INS | `0x32`    |
| P1    | `0x00`    |
| P2    | `0x00`    |
| lc    | `0x0A`    |
| DATA  | PIN value |

| RES      | Data field | Info                                                        |
|----------|------------|-------------------------------------------------------------|
| `0x9000` |            | success                                                     |
| `0x6A01` |            | decryption error                                            |
| `0x6A02` |            | MAC error                                                   |
| `0x6A04` |            | applet not initialized                                      |
| `0x6A05` |            | applet already initialized                                  |
| `0x6A06` |            | encryption error                                            |
| `0x6B00` |            | general error                                               |
| `0x6B01` |            | not logged in                                               |
| `0x6B02` |            | out of tries, secret data deleted, PIN set to default       |
| `0x6B03` |            | PIN policy not satisfied, does not decrease remaining tries |
| `0x6B04` |            | problem with resetting storage                              |

### Change PIN
| APDU  | Values  |
|-------|---------|
| INS   | `0x23`  |
| S_INS | `0x33`  |
| P1    | `0x00`  |
| P2    | `0x00`  |
| lc    | `0x0A`  |
| DATA  | new pin |

| RES      | Data field | Info                       |
|----------|------------|----------------------------|
| `0x9000` |            | success                    |
| `0x6A01` |            | decryption error           |
| `0x6A02` |            | MAC error                  |
| `0x6A04` |            | applet not initialized     |
| `0x6A05` |            | applet already initialized |
| `0x6A06` |            | encryption error           |
| `0x6B00` |            | general error              |
| `0x6B01` |            | not logged in              |
| `0x6B03` |            | PIN policy not satisfied   |


### Get Value of Secret
| APDU  | Values                           |
|-------|----------------------------------|
| INS   | `0x24`                           |
| S_INS | `0x34`                           |
| P1    | `0x00`                           |
| P2    | `0x00`                           |
| lc    | `0xBB` length of the wanted name |
| DATA  | name                             |

| RES      | Data field  | Info                              |
|----------|-------------|-----------------------------------|
| `0x9000` | secret data | success                           |
| `0x6A01` |             | decryption error                  |
| `0x6A02` |             | MAC error                         |
| `0x6A04` |             | applet not initialized            |
| `0x6A05` |             | applet already initialized        |
| `0x6A06` |             | encryption error                  |
| `0x6B00` |             | general error                     |
| `0x6B04` |             | storage error, probably not found |
| `0x6B05` |             | name policy error                 |

### Store Value of Secret
| APDU  | Values                                                                        |
|-------|-------------------------------------------------------------------------------|
| INS   | `0x25`                                                                        |
| S_INS | `0x35`                                                                        |
| OP    | `0x00`                                                                        |
| lc    | `0xBB` length of name + secret data                                           |
| DATA  | name length [1 B] \ name [max 10 B] \ secret length [1 B] \ secret [max 64 B] |

| RES      | Data field | Info                                |
|----------|------------|-------------------------------------|
| `0x9000` |            | success                             |
| `0x6A01` |            | decryption error                    |
| `0x6A02` |            | MAC error                           |
| `0x6A04` |            | applet not initialized              |
| `0x6A05` |            | applet already initialized          |
| `0x6A06` |            | encryption error                    |
| `0x6B00` |            | general error                       |
| `0x6B01` |            | not logged in                       |
| `0x6B04` |            | storage                             |
| `0x6B05` |            | name policy not satisfied           |
| `0x6B06` |            | secret or name policy not satisfied |


### Secure Delete Secret
| APDU  | Values                |
|-------|-----------------------|
| INS   | `0x26`                |
| S_INS | `0x36`                |
| P1    | `0x00`                |
| P2    | `0x00`                |
| lc    | `0xBB` length of name |
| DATA  | name                  |

| RES      | Data field | Info                            |
|----------|------------|---------------------------------|
| `0x9000` |            | success                         |
| `0x6A01` |            | decryption error                |
| `0x6A02` |            | MAC error                       |
| `0x6A04` |            | applet not initialized          |
| `0x6A05` |            | applet already initialized      |
| `0x6A06` |            | encryption error                |
| `0x6B00` |            | general error                   |
| `0x6B01` |            | not logged in                   |
| `0x6B04` |            | storage error, secret not found |
| `0x6B06` |            | name policy not satisfied       |
 
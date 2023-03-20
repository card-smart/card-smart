# CardSmartApplet Specification

## Capabilites

### 1. Store name-value pairs
* Filesystem class
* Named files as buffers for secret data
* PIN protected

### 2. Reading all names 
* names stored in filesystem
* no PIN protection needed
* data sent as bytes, '\0' divider
* data in LV (length-value) structure
  * length = 1 byte
  * data = name

### 3. Login
* PIN policy setup in the applet
* PIN is read from cmd option
* login operation setup status in applet filesystem
* checking remaining tries log out from current status
* when no remaining tries left
  * secret data are erased from card
  * PIN set to default value
* when sending PIN from the app
  * pad PIN with 0's to get full length always (easier to check in the card)
  
### 4. Change PIN
* checks PIN policy
* needs PIN to be verificated

### 5. Get secret value
* authenticate with PIN
* ask card, whether value with associated name exists and gets length of data
* ask card for data

### 6. Store secret value
* send data length to card (also with name?) to get verification, that such length can be stored
* send data with the number of chunk

### 7. Delete secret value
* send secret name to card, which should be deleted
* get information, that such data do not exist/cannot be deleted

## Files
### CardSmartApplet.java
#### Static information
* definition of APDU instructions
* PIN related contants
  * inital PIN value = 0000 (in ASCII), padded to 10 bytes (pin max length)
  * max tries = 5
  * min length = 4
  * max length = 10
* storage related information
  * max record size = 64 (`HMACKey`)
  * max record number = 16
* name policy
  * alphanumeric characters
  * min length of name = 4
  * max length of name = 10

#### Initialization
* create default OwnerPIN object
* generate new asymmetric keys
* secure channel instance
* array to store secrets:
```
private short maxNumberOfRecords;
MyObject[] myObjects = new MyObject[10];

for (short i = 0; i < myObjects.length; i++) {
    myObjects[i] = new MyObject();
}
```

### Record.java
* name
* HMACKey object for secret data
* getter
* checksum to detect faults (`Checksum`)

### SecureChannel.java
* generate random new key pair for secure communication


## APDU Specification
* CLA = `0xC0`
* **APDU STRUCTURE**
  1. Unsecure = not encrypted nor MACed
      * data are sent directly over the insecure channel
      * APDU processed directly
  2. Secure = encrypt-than-MAC
      * sending with special APDU, which means _we are sending encrypted data, which are along with the command_
  ```
  Unsecure APDU:
  CLA | INS | P1 | P2 | lc | data [max 255 B]

  Secure APDU:
  CLA | INS | P1 | P2 | lc | encryted data [max 239 B] | MAC tag [16 B]

  Inner encrypted data [max 239 B]:
  INS | OP | lc | data [max 236 B]

  Unsecure RES:
  SW1 | SW2 | data [max 256 B]

  Secure RES:
  SW1 | SW2 | encrypted data [max 240 B] | MAC tag [16 B]
  ```
* **LV structure** (length-value)
  ```
  LEN | VALUE
  ```
  * by default, the `OP` in the inner APDU is only byte, it can be also used as `P1` while `P2 = 0x00`

### Unsecure Get Card EC Public Key
| APDU | Values  |
| ---- | ------- |
| CLA  | `0xC0`  |
| INS  | `0x40`  |
| P0   | `0x00`  |
| P1   | `0x00`  |
| lc   | `0x00`  |
| DATA | ignored |
| le   | ignored |

| RES      | Data field    | Info  |
| -------- | ------------- | ----- |
| `0x9000` | EC public key |       |
| `0x6B00` | none          | error |

### Unsecure Open Secure Channel
| APDU | Values                             |
| ---- | ---------------------------------- |
| CLA  | `0xC0`                             |
| INS  | `0x41`                             |
| P0   | `0x00`                             |
| P1   | `0x00`                             |
| lc   | length of app public key and nonce |
| DATA | app public key and nonce           |

| RES      | Data field | Info    |
| -------- | ---------- | ------- |
| `0x9000` |            | success |

### Unsecure Send Message
| APDU | Values                                 |
| ---- | -------------------------------------- |
| CLA  | `0xC0`                                 |
| INS  | `0x42`                                 |
| P0   | `0x00`                                 |
| P1   | `0x00`                                 |
| lc   | length of the encrypted data + MAC tag |
| DATA | encrypted data + MAC tag               |

### Unsecure Close Secure Channel
| APDU | Values  |
| ---- | ------- |
| CLA  | `0xC0`  |
| INS  | `0x43`  |
| P0   | `0x00`  |
| P1   | `0x00`  |
| lc   | `0x00`  |
| DATA | ignored |

### Secure channel error codes
| RES      | Data field | Info                                                  |
|----------| ---------- |-------------------------------------------------------|
| `0x9000` |            | success                                               |
| `0x6A00` |            | decryption error                                      |
| `0x6A01` |            | MAC error                                             |
| `0x6B00` |            | error                                                 |
| `0x6B01` |            | not logged in                                         |
| `0x6B02` |            | out of tries, secret data deleted, PIN set to default |
| `0x6B03` |            | PIN policy not satisfied                              |
| `0x6B04` |            | storage full                                          |
| `0x6B05` |            | name policy not satisfied                             |
| `0x6B06` |            | secret policy not satisfied                           |
| `0x6B07` |            | no such data                                          |
| `0x6C00` |            | unsupported CLA                                       |
| `0x6C01` |            | unsupported INS                                       |

### Secure Get Names Length
| APDU | Values  |
| ---- | ------- |
| INS  | `0x50`  |
| OP   | `0x00`  |
| lc   | `0x00`  |
| DATA | ignored |

| RES      | Data field | Info  |
| -------- | ---------- | ----- |
| `0x9000` |            |       |
| `0x6B00` |            | error |

#### Secure Get Names
| APDU | Values                                                                                        |
| ---- | --------------------------------------------------------------------------------------------- |
| INS  | `0x51`                                                                                        |
| OP   | `0x0Y`, where `Y` is the number of expected chunk order of the wanted chunk (starting from 0) |
| lc   | `0x00`                                                                                        |
| DATA | ignored                                                                                       |

| RES      | Data field | Info  |
| -------- | ---------- | ----- |
| `0x9000` |            |       |
| `0x6B00` |            | error |

### Secure Get PIN Remaining Tries
| APDU | Values  |
| ---- | ------- |
| INS  | `0x60`  |
| OP   | `0x00`  |
| lc   | `0x00`  |
| DATA | ignored |

| RES      | Data field            | Info  |
| -------- | --------------------- | ----- |
| `0x9000` | remaining tries [2 B] |       |
| `0x6B00` |                       | error |

### Secure PIN Verify
| APDU | Values               |
| ---- | -------------------- |
| INS  | `0x61`               |
| OP   | `0x00`               |
| lc   | `0x10`               |
| DATA | PIN [padded to 16 B] |

| RES      | Data field | Info                                                  |
|----------| ---------- |-------------------------------------------------------|
| `0x9000` |            | success                                               |
| `0x6B00` |            | error                                                 |
| `0x6B01` |            | not logged in                                         |
| `0x6B02` |            | out of tries, secret data deleted, PIN set to default |

### Secure Change PIN
| APDU | Values                   |
| ---- | ------------------------ |
| INS  | `0x62`                   |
| OP   | `0x00`                   |
| lc   | `0x10`                   |
| DATA | new pin [padded to 16 B] |

| RES      | Data field | Info                     |
|----------| ---------- | ------------------------ |
| `0x9000` |            | success                  |
| `0x6B00` |            | error                    |
| `0x6B03` |            | PIN policy not satisfied |

### Secure Get Length of Secret
| APDU | Values                           |
| ---- |----------------------------------|
| INS  | `0x70`                           |
| OP   | `0x00`                           |
| lc   | `0xYY` length of the wanted name |
| DATA | name                             |

| RES      | Data field                 | Info    |
| -------- | -------------------------- | ------- |
| `0x9000` | length of the secret [2 B] | success |
| `0x6B00` |                            | error   |

### Secure Get Value of Secret
| APDU | Values                                             |
| ---- | -------------------------------------------------- |
| INS  | `0x71`                                             |
| OP   | `0x0A` order of the wanted chunk (starting from 0) |
| lc   | `0xBB` length of the wanted name                   |
| DATA | name                                               |

| RES      | Data field  | Info    |
| -------- | ----------- | ------- |
| `0x9000` | secret data | success |
| `0x6B00` |             | error   |

### Secure Store Value of Secret
| APDU | Values                                                                        |
|------|-------------------------------------------------------------------------------|
| INS  | `0x80`                                                                        |
| OP   | `0x00`                                                                        |
| lc   | `0xBB` length of name + secret data                                           |
| DATA | name length [1 B] \ name [max 10 B] \ secret length [1 B] \ secret [max 64 B] |

| RES      | Data field | Info                        |
|----------| ---------- | --------------------------- |
| `0x9000` |            | success                     |
| `0x6B00` |            | error                       |
| `0x6B04` |            | storage full                |
| `0x6B05` |            | name policy not satisfied   |
| `0x6B06` |            | secret policy not satisfied |


### Secure Delete Secret
| APDU | Values                |
| ---- | --------------------- |
| INS  | `0x81`                |
| OP   | `0x00`                |
| lc   | `0xBB` length of name |
| DATA | name                  |

| RES      | Data field | Info         |
|----------|------------| ------------ |
| `0x9000` |            | success      |
| `0x6B00` |            | error        |
| `0x6B07` |            | no such data |

## Workflows

### Secure channel
#### Opening secure channel
```
--> C0 40 00 00 00
--< xx xx .. 90 00 (xx = public key)
--> C0 41 00 00 xx data 
--< xx xx .. 90 00 (xx = authentication data)
```

#### Sending encrypted message
```
--> C0 42 00 00 xx data (xx = length of encrypted data)
--< xx xx .. 90 00 (xx = encrypted response)
```

#### Closing secure channel
```
--> C0 43 00 00 00
--< 90 00
```

#### Changing PIN
```
open channel
--> C0 40 00 00 00
--< xx xx .. 90 00 (xx = public key)
--> C0 41 00 00 xx data 
--< xx xx .. 90 00 (xx = authentication data)
following data fields will be encrypted
1. get PIN remaining tries
--> C0 42 00 00 xx 60 00 00 MAC (xx = length of encrypted data)
--< xx xx MAC 90 00 (xx = remaining tries)
2. verify PIN 1234
--> C0 42 00 00 xx 61 00 10 31 32 33 34 00 .. 00 MAC (xx = length of encrypted data)
--< 90 00
3. change PIN to 5678
--> C0 42 00 00 xx 62 00 10 35 36 37 38 00 .. 00 MAC (xx = length of encrypted data)
--< 90 00
close channel
--> C0 30 00 00 00
--< 90 00
```

### Getting value of some secret by name
```
open channel
--> C0 40 00 00 00
--< xx xx .. 90 00 (xx = public key)
--> C0 41 00 00 xx data 
--< xx xx .. 90 00 (xx = authentication data)
following data fields will be encrypted
1. get PIN remaining tries
--> C0 42 00 00 xx 60 00 00 MAC (xx = length of encrypted data)
--< xx xx MAC 90 00 (xx = remaining tries)
2. verify PIN 1234
--> C0 42 00 00 xx 61 00 10 31 32 33 34 00 .. 00 MAC (xx = length of encrypted data)
--< 90 00
3. get value of secret abcd
--> C0 42 00 00 xx 80 00 04 61 62 63 64 MAC (xx = length of encrypted data)
--< xx xx .. MAC 90 00 (xx = value of secret)
close channel
--> C0 30 00 00 00
--< 90 00
```
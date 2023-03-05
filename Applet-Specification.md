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

### 3. Login
* PIN policy setup in the applet
* PIN is read from cmd option
* login operation setup status in applet filesystem
* chekcing remaining tries log out from current status

## APDU Specification
* CLA = C0

### GET names
* data length
  * INS = 0x40
  * P1 = 0x01
  * P2 = 0x00
  * lc = 0x00
  * responses:
    * 90 00 - success
    * 6A 88 - data not found
    * 6B 00 - wrong parameter(s) P1-P2
* data chunks
  * INS = 0x40
  * P1 = 0x02
  * P2 = 0xXX (XX is the order of the chunk)
  * lc = 0x00
  * responses:
    * 90 00 - success
    * 6A 88 - data not found
    * 6B 00 - wrong parameter(s) P1-P2
    * 6B 01 - wrong parameter P2 - chunk not found

### PIN login
* get remaining tries
  * INS = 0x20
  * P1 = 0x00
  * P2 = 0x01 (PIN refernce, for PUK it would be different)
  * lc = 00
  * reponses
    * 63 C3 - 3 tries left, not logged in
    * 63 C2 - 2 tries left, not logged in
    * 63 C1 - 1 tries left, not logged in
    * 63 C0 - 0 tries left, not logged in, card locked
    * 6B 00 - wrong parameter(s) P1-P2
* verify PIN = log-in
  * INS = 0x20
  * P1 = 0x00
  * P2 = 0x01 (PIN reference, for PUK it would be different)
  * lc = XX (length of PIN, max 0x0A)
  * data
  * reponses
    * 90 00 successfully logged into card
    * 63 C2 - 2 tries left, not logged in
    * 63 C1 - 1 tries left, not logged in
    * 63 C0 - 0 tries left, not logged in, card locked
    * 6B 00 - wrong parameter(s) P1-P2

### Change PIN
* change pin
  * INS = 0x21
  * P1 = 0x00
  * P2 = 0x01 (PIN reference, for PUK it would be different)
  * lc = XX (length of PIN, max 0x0A)
  * data = PIN
  * responses
    * 90 00 success
    * 6B 00 wrong parameter(s) P1-P2
    * 6B 02 PIN policy not satisfied

### GET value of secret
* data length
  * INS = 0x41
  * P1 = 0x01
  * P2 = 0x00
  * lc = XX (length of name)
  * data = name
  * responses:
    * 90 00 - success
    * 6A 88 - data not found
    * 6B 00 - wrong parameter(s) P1-P2
* data chunks
  * INS = 0x41
  * P1 = 0x02
  * P2 = 0xXX (XX is the order of the chunk)
  * lc = 0x00
  * responses:
    * 90 00 - success
    * 6A 88 - data not found
    * 6B 00 - wrong parameter(s) P1-P2
    * 6B 01 - wrong parameter P2 - chunk not found

## Workflows
### 1. Reading of all names
```
--> C0 40 01 00 00 (send me resulting lengthof data)
--< xx xx .. 90 00 (xx length of data)
--> C0 40 02 XX 00 (XXth chunk expected)
--< ........ 90 00
```

### 2. Log into card with PIN
```
--> C0 20 00 01 00
--< 63 C3 (3 tries left)
--> C0 20 00 01 06 01 02 03 04 05 06
--< 90 00
```

### 3. Change PIN to NEWPIN
```
--> C0 20 00 01 00
--< 63 C3 (3 tries left)
--> C0 20 00 01 06 01 02 03 04 05 06
--< 90 00
--> C0 21 00 01 07 01 02 03 04 05 06 07
--> 90 00
```

## Files
### CardSmartApplet.java
#### Static information
* definition of APDU instructions
* PIN related contants
  * inital PIN value = 0000
  * max tries = 5
  * min length = 4
  * max length = 10
* storage related information
  * max record size = 4096
  * max record number = ? 
* name policy
  * alfanumeric characters
  * min length of name = 4
  * max length of name = 10
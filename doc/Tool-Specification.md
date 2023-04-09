# CardSmart Tool

## Workflows in uninitialized mode
// TODO

## Workflow to initialize applet
1. <span style="color:blue">user provides `PIN` (which will be set to the card) and `path` for storing `pairingSecret`</span>
2. <span style="color:brown">tool generates _ephemeral_ EC keypair</span>
    ``` java
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
    keyGen.initialize(ecSpec, new SecureRandom());
    KeyPair keyPair = keyGen.generateKeyPair();
    PublicKey publicKey = keyPair.getPublic();
    PrivateKey privateKey = keyPair.getPrivate();
    ```
3. <span style="color:brown">tools sends APDU with command `Get Card EC Public Key`</span>
   * APDU: `0xB0 | 0x40 | 0x00 | 0x00`

4. <span style="color:green">card responds</span>
   * RES: `0x90 0x00` (success), DATA: `0x04 | point [64 B]`
     * point represents public key
     * BER/DER encoded uncompressed point
       * `0x04` is the header indicator
       * 32 bytes for X coordinate
       * 32 bytes for Y coordinate
           * can be converted into two `BigInteger` coordinates object and then form an `ECPoint`
           * https://stackoverflow.com/questions/55747517/hsm-returns-a-67-byte-ecdsa-secp256k1-public-key-what-does-this-mean
5. <span style="color:brown">tool calculates</span>
   * generate `pairingSecret` [32 B]
     * by `SecureRandom` class
   * generate `IV` [16 B]
   * derive `initEncryptionKey` [32 B] from `privateKey` and `cardPublicKey`
     * `KeyAgreement.getInstance("ECDH");`
   * AES encrypt `PIN | pairingSecret` to `payload` [48 B, after padding]
     * key is `initEncryptionKey`
     * IV is `IV`
6. <span style="color:brown">tool sends APDU with command `Card Init`</span>
   * APDU: `0xB0 | 0x41 | 0x00 | 0x00 | 0x81 | data [129 B]`
     * DATA: `publicKey [65 B] | IV [16 B] | payload [48 B]`
7. <span style="color:green">card responds</span>
   * RES: `0x90 0x00` (success, `pairingSecret` and `PIN` set)
   * RES: any other mean some error
8. <span style="color:brown">tool creates file with `path` and stores `pairingSecret`</span>

## Workflow in initialized mode
* <span style="color:blue">user provides `PIN` if needed</span>
* <span style="color:blue">user provides `path` for file</span>

### Create secure channel
1. <span style="color:brown">tool checks that `path` is valid and extracts `pairingSecret` [32 B]</span>
2. <span style="color:brown">tools sends APDU with command `Get Card EC Public Key`</span>
   * APDU: `0xB0 | 0x40 | 0x00 | 0x00`

3. <span style="color:green">card responds</span>
   * RES: `0x90 0x00` (success), DATA: `0x04 | point [64 B]`
     * set as `cardPublicKey`
4. <span style="color:brown">tool sends APDU with command `Open Secure Channel`</span>
   * APDU: `0xB0 | 0x42 | 0x00 | 0x00 | 0x41 | uncompressed point [65 B]`

   * public key (uncompressed point) has the same format as public key sent by card (bytes from `getW()` method)
5. <span style="color:green">card responds</span>
   * RES: `0x90 0x00` (success), DATA: `salt [32 B] | IV [16 B]` in plaintext
     * `salt` is used for generating symmetric encryption key and MAC key
     * `IV` is used as `toolIV` initialization value for the first encryption
   * RES: `0x6A 0x03` (applet not initialized, no pairing secret set)
6. <span style="color:brown">tool computes symmetric keys</span>
   * `derivedSecret` [32 B] from `ECDH` (`privateKey`, `cardPublicKey`)
   * computing SHA512 (by updating) into `hash` [64 B]
     * first `derivedSecret`
     * then `pairingSecret`
     * last `salt`
   * `hash` is split into 2 keys
     * `encryptionKey = hash[0:32]` (first 32 B)
     * `macKey = hash[32:64]` (last 32 B)


### First APDU after opening the secure channel (i. e. PIN Verify)
1. <span style="color:brown">tool formats `PIN`</span>
   * `PIN` is padded to 10 B with zeroes -> `data`
2. <span style="color:brown">tool encrypts data with AES, padding M2</span>
   * **first IV after opening secure channel** was received from card is `toolIV`
   * key is `encryptionKey`
   * result is encrypted `payload` [16 B] (generally divisible by 16 B)
3. <span style="color:brown">tool computes MAC over `CLA`, `INS`, `P1`, `P2` and `payload`</span>
   * **MAC is computed in one shot** from buffer `0xB0 | 0x32 | 0x00 | 0x00 | 0x20 | payload [16B]`
     * with `macKey`
     * `Lc` (length of APDU data part) contains the whole length of expected APDU - it contains also MAC length
       * here `16 + 16 ~ 0x20` for encrypted PIN and appended MAC
   * **MAC must be stored as it is used as IV by card in encryption of response** as `cardIV`
4. <span style="color:brown">tool appends MAC after `payload` part in APDU</span>
5. <span style="color:brown">tool sends APDU to card</span>
   * APDU: `0xB0 | 0x32 | 0x00 | 0x00 | 0x20 | payload [16B] | MAC [16 B]`
6. <span style="color:green">card responds</span>
   * RES: `0x90 0x00` (success), DATA: `payload [16 B] | MAC [16 B]` encrypted
     * **MAC in response is not computed over unencrypted response code**
7. <span style="color:brown">tool verifies MAC tag</span>
   * in one shot, verify `payload`
   * with `macKey`
   * MAC tag is stored as next `toolIV` for next encryption
8. <span style="color:brown">tool decrypts payload</span>
   * key is `encryptionKey`
   * IV is `cardIV`
   * `payload` is decrypted into 2 B real response, whether the PIN verification was successful

### Any other (not first) APDU after secure channel opening (i. e. Change PIN)
1. <span style="color:brown">tool formats `newPIN`</span>
    * `newPIN` is padded to 10 B with zeroes -> `data`
2. <span style="color:brown">tool encrypts data with AES, padding M2</span>
    * IV is `toolIV` (last MAC extracted from card response)
    * key is `encryptionKey`
    * result is encrypted `payload` [16 B] (generally divisible by 16 B)
3. <span style="color:brown">tool computes MAC over `CLA`, `INS`, `P1`, `P2` and `payload`</span>
    * **MAC is computed in one shot** from buffer `0xB0 | 0x33 | 0x00 | 0x00 | 0x20 | payload [16B]`
        * with `macKey`
    * **MAC must be stored as it is used as IV by card in encryption of response** as `cardIV`
4. <span style="color:brown">tool appends MAC after `payload` part in APDU</span>
5. <span style="color:brown">tool sends APDU to card</span>
    * APDU: `0xB0 | 0x33 | 0x00 | 0x00 | 0x20 | payload [16B] | MAC [16 B]`
6. <span style="color:green">card responds</span>
    * RES: `0x90 0x00` (success), DATA: `payload [16 B] | MAC [16 B]` encrypted
        * **MAC in response is not computed over unencrypted response code**
7. <span style="color:brown">tool verifies MAC tag</span>
    * in one shot, verify `payload`
    * with `macKey`
    * MAC tag is stored as next `toolIV` for next encryption
8. <span style="color:brown">tool decrypts payload</span>
    * key is `encryptionKey`
    * IV is `cardIV`
    * `payload` is decrypted into 2 B real response, whether the PIN change was successful

### Closing secure channel
1. <span style="color:brown">tool formats APDU for command `Close Secure Channel`</span>
    * APDU: `0xB0 | 0x43 | 0x00 | 0x00 | 0x10`
      * `Lc` is already set to 16 B as the MAC tag will be prepended
2. <span style="color:brown">tool computes MAC tag over `0xB0 | 0x43 | 0x00 | 0x00 | 0x10`</span>
   * with `macKey`
3. <span style="color:brown">tool appends MAC tag and sends APDU to card</span>
   * APDU: `0xB0 | 0x43 | 0x00 | 0x00 | 0x10 | MAC [16 B]`
4. <span style="color:green">card responds</span>
   * RES: `0x90 0x00`

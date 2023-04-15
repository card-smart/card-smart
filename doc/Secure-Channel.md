# Secure Channel in CardSmart Applet and Tool

## Used Algorithms
### Encryption of communication
* _data confidentiality_
* AES256 cipher in CBC mode with [ISO9797 M2 padding](https://en.wikipedia.org/wiki/ISO/IEC_9797-1)
  * derived shared symmetric key used both by tool and applet
  * maximum length after encryption is 240 B
    * maximum of 223 B of data can be encrypted
      * 16 B need to be padded in case of data length divisible by 16 B
      * otherwise the smallest padding size is 1 B
### MAC tags
* _data integrity, authentication of origin_
  * non-repudiation is not needed property as MAC key is shared only among 2 parties communicating in special manner
* based on AES128 CBC MAC with not padding
  * response data have already desired length as they are produced by AES encryption
  * tag in APDU is computed from 5 instruction bytes (CLA, INS, P1, P2, Lc) and encrypted data
    * missing 11 bytes taken from current iv value
* _encrypt-then-MAC_ - mechanism
  * ensuring integrity of both plaintext and ciphertext
  * MAC is computed after encryption
  * MAC is verified before decryption

## Secure Channel Establishment
### **Tool** Secure Channel Initialization
* pairingSecret not set yet
* tool generates _ephemeral_ EC keypair 

### **Card** Secure Channel Initialization
* pairingSecret not set yet
* card generates _persistent_ EC keypair

### Pairing
* creation of pairing secret
* should be performed in secure environment
* workflow:
  1. tool sends APDU for getting card's public key
  2. card sends its public key to tool {SecureChannel:`getCardPublicKey`}
  3. tool does
      * simpleDerivedSecret = ECDH(tool.privatekey, card.publickey)
      * IV = randomData(16)
      * pairingSecret = randomData(32)
      * payload = AES_encrypt(IV; PIN | pairingSecret)
  4. tool sends [tool.pubkey | IV | payload] to the card
  5. card does
      * simpleDerivedSecret = ECDH(card.privatekey, tool.publickey) {SecureChannel:`initDecrypt`}
      * PIN | pairingSecret = AES_decrypt(IV; payload) {SecureChannel:`initDecrypt`}
      * set pairing secret {SecureChannel:`initSecureChannel`}
      * set PIN {CardSmartApplet:`init`}
  => both card and tool has pairing secret

### Open Secure Channel
* pairing secret and EC keypair are set
* workflow:
  1. tool sends APDU for getting card's public key
  2. card sends its public key to tool {SecureChannel:`getCardPublicKey`}
  3. tool sends APDU for creating secure channel {CardSmartApplet:`init`}
     * DATA: tool's public key
  4. _card_ creates **secrets**
     * derivedSecret = ECDH(card.privatekey, tool.publicKey) {SecureChannel:`openSecureChannel`}
     * salt | IV = randomData(32 + 16) {SecureChannel:`openSecureChannel`}
     * hash = SHA512(derivedSecret | pairingSecret | salt) {SecureChannel:`openSecureChannel`}
     * encryptionKey = hash[0:32] {SecureChannel:`openSecureChannel`}
     * macKey = hash[32:64] {SecureChannel:`openSecureChannel`}
  5. card sends **salt** and **IV** to tool in plaintext
  6. _tool_ creates **secrets**
     * derivedSecret = ECDH(tool.privatekey, card.publicKey)
     * hash = SHA512(derivedSecret | pairingSecret | salt)
     * encryptionKey = hash[0:32]
     * macKey = hash[32:64]
  => both card and tool have symmetric key for AES encryption and MAC tag, IV is used by tool as first IV in encryption

### APDU and responses on the **card**'s site
#### Encryption of response - {SecureChannel:`encryptResponse`}
* main (unencrypted and unMACed) SW1 and SW2 are always `0x9000`
* return codes are packed inside the data part
```
0x90 | 0x00 | encrypted(data [max 221 B] | SW1 | SW2) [max 223 B] | MAC tag [16 B]
```
* MAC is created from `encrypted(data [max 238 B] | SW1 | SW2)`

#### Decryption of APDU - {SecureChannel:`decryptAPDU`}
* CLA, INS, P1/P2, l_c bytes are plain (but part of the MAC)
* payload is encrypted
* MAC tag [16 B] is appended after encrypted payload
```
CLA | INS | P1 | P2 | L_C | encrypted(data) [max 240 B] | MAC tag [16 B]
```
* MAC is verified from `CLA | INS | P1 | P2 | L_C | encrypted(data)`

### APDU and responses on the **tool**'s site
#### Encryption of APDU
* only data encrypted
* MAC computed after encryption from CLA, INS, P1, P2, Lc and payload
* {ToolSecureChannel:`prepareSecureAPDU`}

#### Decryption of response
* SW is by default set to success (`0x9000`)
  * original SW is appended into data part of response and encrypted
* different SW than success means that encryption/decryption or MAC computing failed
  * 2 possible reasons
    1. user fault - wrong pairing secret used
    2. possible attack - attacker does not know secret and cannot create correct keys for encryption/MAC

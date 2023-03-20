# Secure Channel in CardSmart Applet and Tool

## Used Algorithms
* **encryption of communication** - _data confidentiality_
  * AES256 cipher in CBC mode
    * symmetric key used both by tool and applet
* **MAC tags** - _data integrity, authentication of origin_
  * based on AES 128 with not padding

## Secure Channel Establishment
### **Tool** Secure Channel Initialization
* tool generated _ephemeral_ EC keypair 

### **Card** Secure Channel Initialization
* pairingSecret is set
* new _persistent_ static EC keypair is generated

### Pairing
1. tool sends APDU for getting card's public key
2. card sends its public key to tool
3. tool does
    * simpleDerivedSecret = ECDH(tool.privatekey, card.publickey)
    * IV = randomData(16)
    * pairingSecret = randomData(32)
    * payload = AES_encrypt(IV; PIN | pairingSecret)
4. tool sends [tool.pubkey | IV | payload] to the card
5. card does
    * simpleDerivedSecret = ECDH(card.privatekey, tool.publickey)
    * PIN | pairingSecret = AES_decrypt(IV; payload)
=> both card and tool has pairing secret

### Open Secure Channel
1. tool sends APDU for getting card's public key
2. card sends its public key to tool
3. tool sends APDU for creating secure channel
   * DATA: tool's public key
4. _card_ creates **secrets**
   * derivedSecret = ECDH(card.privatekey, tool.publicKey)
   * salt = randomData(256)
   * hash = SHA512(derivedSecret | pairingSecret | salt)
   * encryptionKey = hash[0:32]
   * macKey = hash[32:64]
5. card sends **salt** to tool
6. _tool_ creates **secrets**
   * derivedSecret = ECDH(tool.privatekey, card.publicKey)
   * hash = SHA512(derivedSecret | pairingSecret | salt)
   * encryptionKey = hash[0:32]
   * macKey = hash[32:64]

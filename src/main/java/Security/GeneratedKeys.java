package Security;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GeneratedKeys {
    //  public key that will be sent to the client for message encryption
    private String publicRsaKey;

    //  private key that will be used for decryption of messages
    private String privateRsaKey;

    //  aes key that will be used for encryption of messages and their decryption
    private String aesKey;

    //  key received from client that will be used for encryption
    private String receivedPublicRSAKey;

    //  key generator that makes both RSA and AES keys in String form
    public GeneratedKeys(boolean isItServer) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //  create rsa 2048-bit keys
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        //  append them and save them
        Key publicKey = keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        //  create encoder for saving them into String form
        Base64.Encoder encoder = Base64.getEncoder();

        //  append key-strings
        this.publicRsaKey = encoder.encodeToString(publicKey.getEncoded());
        this.privateRsaKey = encoder.encodeToString(privateKey.getEncoded());

        //  initialize AES key if it is a server (client will receive this key from server)
        if(isItServer) {
            this.aesKey = "beatrixKido";
        }
    }

    /**
     * decode rsa private key from string to classic key entity
     * @return private rsa key entity
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public PrivateKey decodeRsaPrivateKeyFromString() throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(this.privateRsaKey.getBytes()));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(ks);
    }

    /**
     * decode rsa public key string into entity
     * @return key entity
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public PublicKey decodeRsaPublicKeyFromString() throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec ks = new X509EncodedKeySpec(Base64.getDecoder().decode(this.publicRsaKey.getBytes()));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(ks);
    }

    /**
     * decode received public rsa key string into entity
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public PublicKey decodeRsaReceivedPublicKeyFromString() throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec ks = new X509EncodedKeySpec(Base64.getDecoder().decode(this.receivedPublicRSAKey.getBytes()));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(ks);
    }

    public String getPublicRsaKey() {
        return publicRsaKey;
    }

    public void setPublicRsaKey(String publicRsaKey) {
        this.publicRsaKey = publicRsaKey;
    }

    public String getPrivateRsaKey() {
        return privateRsaKey;
    }

    public void setPrivateRsaKey(String privateRsaKey) {
        this.privateRsaKey = privateRsaKey;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getReceivedPublicRSAKey() {
        return receivedPublicRSAKey;
    }

    public void setReceivedPublicRSAKey(String receivedPublicRSAKey) {
        this.receivedPublicRSAKey = receivedPublicRSAKey;
    }
}
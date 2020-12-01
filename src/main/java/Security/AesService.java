package Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for AES encryption algorithm operations over transmitted data
 */
public class AesService {

    //  key for encrypting and decrypting
    private SecretKeySpec secretAesKeySpec;

    /**
     * Set key for coding all messages
     * @param clientKey Key that will be used for encryption of all messages
     * @throws NoSuchAlgorithmException Invalid encryption method
     */
    public void setKey(String clientKey, GeneratedKeys generatedKeys) throws NoSuchAlgorithmException {
        generatedKeys.setAesKey(clientKey);

        // make byte-form of message
        byte[] key = generatedKeys.getAesKey().getBytes(StandardCharsets.UTF_8);

        // set type of key that will be used for encryption
        MessageDigest sha = MessageDigest.getInstance("SHA-1");

        // setting private key for encryption
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        secretAesKeySpec = new SecretKeySpec(key, "AES");
    }

    /**
     * encrypt data
     * @param dataToEncrypt Data that requires encryption
     * @return Encrypted data
     */
    public String encrypt(String dataToEncrypt) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
            // set encryption method for data
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretAesKeySpec);

        return Base64.getEncoder().encodeToString(cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Decryption of data with AES algorithm
     * @param dataToDecrypt Data that requires encryption
     * @return Decrypted data
     * @throws BadPaddingException Error in decryption
     * @throws IllegalBlockSizeException Invalid buffer format
     * @throws InvalidKeyException Invalid key
     * @throws NoSuchPaddingException Error in decryption
     * @throws NoSuchAlgorithmException Invalid method of decryption
     */
    public String decrypt(String dataToDecrypt) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        // set decryption method for encrypted data
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretAesKeySpec);

        //  decode received data into String
        return new String(cipher.doFinal(Base64.getMimeDecoder().decode(dataToDecrypt)), StandardCharsets.UTF_8);
    }

    public SecretKeySpec getSecretAesKeySpec() {
        return secretAesKeySpec;
    }
}

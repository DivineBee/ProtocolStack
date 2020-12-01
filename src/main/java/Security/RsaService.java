package Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Service for RSA encryption algorithm operations over transmitted data
 */
public class RsaService {
    /**
     * encrypt data with received rsa public key
     * @param dataToEncrypt what to encrypt
     * @param generatedKeys reference to key storage
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public String encrypt(String dataToEncrypt, GeneratedKeys generatedKeys)
            throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, generatedKeys.decodeRsaReceivedPublicKeyFromString());

        String encodedAndEncryptedData = Base64.getEncoder().encodeToString(cipher.doFinal(dataToEncrypt.getBytes()));

        return encodedAndEncryptedData;
    }

    /**
     * decrypt data using private key
     * @param dataToDecrypt what to decrypt
     * @param rsaPrivateKey private key
     * @return decrypted data
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeySpecException
     */
    private String decrypt(byte[] dataToDecrypt, PrivateKey rsaPrivateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

        return new String(cipher.doFinal(dataToDecrypt));
    }

    /**
     * decrypt data using private Rsa key
     * @param dataToDecrypt what to decrypt
     * @param generatedKeys key storage reference
     * @return decrypted data
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     */
    public String decrypt(String dataToDecrypt, GeneratedKeys generatedKeys)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException {

        return decrypt(Base64.getDecoder().decode(dataToDecrypt.getBytes()), generatedKeys.decodeRsaPrivateKeyFromString());
    }
}

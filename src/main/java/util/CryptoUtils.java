package util;


import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;


public class CryptoUtils {
    final static private Logger LOG = LogManager.getLogger(CryptoUtils.class);

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

    public static final String SIGNATURE_ALGO = "SHA-256";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;


    private static String password = "eRcNrtt=nR*42xxv$uNh?GfSyM5AvK?kcKZDa@h^8ZcWp8jLF-MkHcF_uuutH?45YE&cqk&G&QGBRbGFzZbR?dFKC6sJn2ACN&Aj&?L#f3@6fprRn-=YHBPyrPa9HH+2ZjBySRpBhEV?AR9dZ+#%aPD4W#LP!-JC?RQP%4whnbPn@gj%5_CtYp$vtBcdyP8g+sq?H5Q=nvAZUh+$qQ6Y2HN-#7fu*@%t^bbwu9F6xRUSB85zkKbqeUWm2WS8V";


    public static String decrypt(String cText) {

        String rtn = null;

        try {
            CryptoUtils crypto = new CryptoUtils();

            byte[] decode = Base64.getDecoder().decode(cText.getBytes(UTF_8));
            ByteBuffer bb = ByteBuffer.wrap(decode);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            bb.get(salt);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);
            SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            //log.info("decrypted Text {}",new String(plainText, UTF_8));
            rtn = new String(plainText, UTF_8);

        } catch (Exception e) {
            //log.info("error occurred {} ", e);
            e.printStackTrace();
        }
        return rtn;
    }


    public static SecretKey getAESKeyFromPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }


    public static String encrypt(String text) {

        try {
            //log.info("Text to Encrypt : {}", text);
            CryptoUtils cryptoUtils = new CryptoUtils();
            byte[] pText = text.getBytes(UTF_8);
            byte[] salt = getSaltRandomByte(SALT_LENGTH_BYTE);
            byte[] iv = getSaltRandomByte(IV_LENGTH_BYTE);
            //log.info("password to Encrypt : {}", password);
            SecretKey aesKeyFromPassword = getAESKeyFromPassword(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(pText);
            byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                    .put(iv)
                    .put(salt)
                    .put(cipherText)
                    .array();
            return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);
        } catch (Exception e) {
            //log.info("error occurred {}", e);

        }
        return null;
    }

    public static byte[] getSaltRandomByte(int numBytes) {
        byte[] nonce = new byte[numBytes];
        for (int i = 0; i < nonce.length; i++) {
            nonce[i] = fetchByte();
        }
        return nonce;
    }


    public static byte fetchByte() {
        Random random = new Random();
        int i = random.nextInt((100 - 1) + 1) + 1;
//    System.out.println("ramdom : "+ i);
        return ((byte) Math.abs(i));
    }

    public static String base64Encode(String base) {
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(base.getBytes()));
    }

    private static byte[] base64Decode(String message) {
        byte[] res = null;
        try {
            res = Base64.getDecoder().decode(message);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
        return res;
    }

    public static String sha256Encrypt(String s) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(SIGNATURE_ALGO);
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encodeHex(hash));
    }


    public static void main(String[] args) throws Exception {
        String encryptMessage = "{\n" +
                "    \"username\":\"michael.afolabi\",\n" +
                "    \"password\":\"59984112\",\n" +
                "    \"token\":\"73772207\"\n" +
                "}";
        String decryptMessage = "Cls5P2IpUg1fJUZKP0AVRhUdC2ElBw1eQE8oI/6v0ulRNRcaV2zqqdPE4zdQD4C4Q92qs6j0hw==";
//        System.out.println(decrypt(encrypt(encryptMessage)));
        System.out.println("Message: "+encrypt(encryptMessage));
        System.out.println(decrypt(decryptMessage));

//        // Hash the password using MD5
//        MessageDigest md = MessageDigest.getInstance("MD5");
//        byte[] hashedKey = md.digest(password.getBytes("UTF-8"));
//
//        // Use the first 16 bytes of the hash as the AES key
//        SecretKeySpec secretKey = new SecretKeySpec(hashedKey, "AES");
//
//        System.out.println("Generated Secret Key: " + bytesToHex(hashedKey));
//        System.out.println(decrypt("JWXYxmePC34hSiahgMO0YArk0w=="));
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }




}






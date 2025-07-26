package util;


import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
//port java.util.prefs.*;

public class Encrypter {
        Cipher ecipher;
        Cipher dcipher;
        
        static private String phrase="sunday.kusoro@infometics.net&*;.,1234";
        // 8-byte Salt
        byte[] salt = {
            (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
            (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
        };

        //Iteration count
        int iterationCount = 19;

        public Encrypter(){
            try {
                //Create the key
                KeySpec keySpec = new PBEKeySpec(phrase.toCharArray(), salt, iterationCount);
                SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
                ecipher = Cipher.getInstance(key.getAlgorithm());
                dcipher = Cipher.getInstance(key.getAlgorithm());

                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
                // Create the ciphers
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
                
            } catch (InvalidAlgorithmParameterException | InvalidKeySpecException | NoSuchPaddingException |
                     NoSuchAlgorithmException | InvalidKeyException e) {
            }
        }

        final public String encryptWithKey(String str) throws IllegalBlockSizeException, BadPaddingException {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);
            // Encrypt
                byte[] enc = ecipher.doFinal(utf8);

            return Base64.getEncoder().encodeToString(enc);

        }

        final public String encrypt(String str) {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes(StandardCharsets.UTF_8);

            // Encrypt
//                byte[] enc = ecipher.doFinal(utf8);

            // Encode bytes to base64 to get a string
            //String BasicBase64format

            return  Base64.getEncoder().encodeToString(utf8);

            //return new sun.misc.BASE64Encoder().encode(enc);

        }

        
       final  public String decryptWithKey(String str){
            try{
                byte[] dec = Base64.getDecoder().decode(str);
                byte[] utf8 = dcipher.doFinal(dec);

                return new String(utf8, "UTF8");
            } catch (BadPaddingException e) {
            } catch (IllegalBlockSizeException e) {
            } catch (IOException e) {
            }
            return null;
        }

       final  public String decrypt(String str){
            try{
                // Decode base64 to get bytes

            	byte[] dec = Base64.getDecoder().decode(str);


                return new String(dec, "UTF8");

            } catch (IOException e) {
            }
            return null;
        }


    public static void main(String[] args) {
        try {
            System.out.println(new Encrypter().encryptWithKey("software"));
            System.out.println(new Encrypter().decryptWithKey("zmeJtBFWrqm+PhHormIMYg=="));
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}

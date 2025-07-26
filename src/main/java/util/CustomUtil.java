package util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import constants.AppModules;
import constants.Services;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import persistence.DBHelper;

import javax.servlet.ServletRequest;
import javax.servlet.http.Part;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class CustomUtil {

    public static BaseBean toBaseBean(Header[] hdrs) {

        if (hdrs == null) {
            return null;
        }

        if (hdrs.length == 0) {
            return null;
        }

        BaseBean baseBean = new BaseBean();
        for (Header str : hdrs) {

            baseBean.setString(str.getName(), str.getValue());

        }

        return baseBean;
    }


    public static void fillBaseBean(Header[] hdrs, BaseBean baseBean) {

        for (Header str : hdrs) {

            baseBean.setString(str.getName(), str.getValue());

        }

        //return baseBean;
    }


    public static RequestConfig getHttpRequestTimeoutConfig(BaseBean configInfo) {

        int readTimeout = 5;
        int connRequestTimeout = 1;
        int connTimeout = 2;

        try {
            readTimeout = Integer.parseInt(configInfo.get("read_timeout"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            connRequestTimeout = Integer.parseInt(configInfo.get("conn_request_timeout"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            connTimeout = Integer.parseInt(configInfo.get("conn_timeout"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connTimeout * 1000)
                .setConnectionRequestTimeout(connRequestTimeout * 1000)
                .setSocketTimeout(readTimeout * 1000).build();


        return config;
    }

    public static void createKeyPairs(BaseBean requestBean) {

        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

            kpg.initialize(2048);

            KeyPair kp = kpg.generateKeyPair();

            Key pub = kp.getPublic();

            Key pvt = kp.getPrivate();

            Base64.Encoder encoder = Base64.getEncoder();
            requestBean.setString("pub_key", encoder.encodeToString(pub.getEncoded()));
            requestBean.setString("priv_key", encoder.encodeToString(pvt.getEncoded()));
            System.out.println("Public Key:: ".concat(requestBean.getString("pub_key")));
            System.out.println("Private Key:: ".concat(requestBean.getString("priv_key")));

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Key convertBase64ToRSAKey(String base64String, boolean isPrivateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);

            if (isPrivateKey) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            } else {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new InvalidKeySpecException("Error converting Base64 to RSA Key: " + e.getMessage());
        }
    }


    public static byte[] convertBase64ToPKCS8(String base64String) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            return privateKey.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new InvalidKeySpecException("Error converting Base64 to PKCS#8: " + e.getMessage());
        }
    }

    public static byte[] convertBase64ToPKCS1(String base64String) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            return publicKey.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new InvalidKeySpecException("Error converting Base64 to PKCS#1: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
//        String base64EncodedPrivateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCFcS9lzAN0STCmuxEO+x/lR8MTROwRKPgmAQbqqZSBHC8FcUTNt5OF3vI4MwwdKr4zRYz5gYzcIv8tnFFH0Y6LCpSsLpGnG7F2prrHZdQw37S5gs0t39gVPLPFpDTivN6jP18aCxbdzMa7B284aQ8IhBxcPd9GvvDUY3Dze+WqzJdsnAM8FZ2wxhKOmFTGBaNP4g4KIwPdb7Y6B6ehkQAfioFr8zRG3wrh7HRmv2j/5IXXeCCmpA9z35hum8W7pQdEmffcNOjhUdGhKy5iWkejw3VIX7MwEGZ/9dUxRJLNiyQYz4PXRAj/ph5YmhDudx31OS/5JPlaq0layJmnOFTrAgMBAAECggEAOiI4GuoJKzTjfA+M+/DNUW60/oUNLuChUrUp1Ttxldm4Zm+VIOXz3+NjtLYQdfh0ChQnuV8GBbU9ZBpwkpApwEsov+Y90AZRhrZWMp4bFfuvWQ4x4uVc1suuKdFCtPDBMW9fOBJSsOgQP9SaUIH8aaJcVJcj2Uc/1ddIktqMEJ8Uy7jjZ97CfHFHT4scn9t0+cWPqf3Id7uIYonVhDF88y5bTA41sQYb8zRBkOmGYj6GDLgDebOFcfp++xtuGLHljn8usrS9QY2zz5vkis10DfEHD2aWku32AiVIS/SKW7H3u4xlk4XF7/8J2qYanmUpE2/I3m6Q2C6RCJBgUKb9qQKBgQDPmKoL5gsSWLMI9ofYKuI6073nlGXohMw9napQqN8k+og7323CFb4fU0qjyUI9qTx2NeKCT5GGc+WG97MrMuDsdIinXjBUyRz266cLBl5SR0RATWg4wzyjZ+RODYPBHch9bw0nEP9deW0dEY9cUBlPQXnCZvOR3Irn41lf9ogGHwKBgQCkjkm4Ik/+tD4BuRxdoJke5UIzpQFvi5FZVdeBr+xemqU7Xkt9TJgxC0W2yA063tI8hhoRj6HvA+eWvF4iL5qmmHghaR9KLorLSwG/SbXw5sayNdovaE/yixZEZD235/DjELFFdB1m6AvjKYHAC9t1X5ccvvMMo2e3Lyulh2nftQKBgQCzkBpt5lJnUR+zPgLsgNNkDOizWdH4GH3NQgYidJ9nDeku2KjIeyQTVmk2WIwaZ7sriJpfGtIGWVMMtk5crEqPXJEjK75kZ/zMu7KmP6DwTEKF2C6xAnb95Iw+00PGk6tqi9b1lqc7xB8USK8XKxVpT3oxI/nEjd38i9MvxT+NAwKBgQCL9tavPsgpzac8EfYVkOcMh1Wi1cN93Al6IfCWx0rWQNUvxQmSftaoSH04dboDC8loX2vG6hODWB/gb0Hl8/Wno+HyERbjIuk89wR6brjJOZFhvTpivmfZ1gHPlC+GwEp86BLQD25+u/p4cjKBZdR5ZiAC/EuAwXATOJT4S1B7TQKBgHDoNaGSrTbfzFTZj9bzDBVJOvIawxvXQFihL5cMD/C5x7ZVrSfZdLNHzJEzRtjFKyFLCHRxWoVONgkGMiBZ/ETH2AQm9syw4UeKfZGDU2aDkSrdJIzO0BrAzO6IMdh3yp8d4KXPvYwmoeVAz/3vWy6EIYJTgDz6W7JnR/xad5q3";
//        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhXEvZcwDdEkwprsRDvsf5UfDE0TsESj4JgEG6qmUgRwvBXFEzbeThd7yODMMHSq+M0WM+YGM3CL/LZxRR9GOiwqUrC6Rpxuxdqa6x2XUMN+0uYLNLd/YFTyzxaQ04rzeoz9fGgsW3czGuwdvOGkPCIQcXD3fRr7w1GNw83vlqsyXbJwDPBWdsMYSjphUxgWjT+IOCiMD3W+2OgenoZEAH4qBa/M0Rt8K4ex0Zr9o/+SF13ggpqQPc9+YbpvFu6UHRJn33DTo4VHRoSsuYlpHo8N1SF+zMBBmf/XVMUSSzYskGM+D10QI/6YeWJoQ7ncd9Tkv+ST5WqtJWsiZpzhU6wIDAQAB";
//
//        try {
//            byte[] pkcs8PrivateKeyBytes = convertBase64ToPKCS8(base64EncodedPrivateKey);
//            System.out.println("PKCS#8 Private Key: " + Base64.getEncoder().encodeToString(pkcs8PrivateKeyBytes));
//
//            byte[] pkcs1PublicKeyBytes = convertBase64ToPKCS1(base64EncodedPublicKey);
//            System.out.println("PKCS#1 Public Key: " + Base64.getEncoder().encodeToString(pkcs1PublicKeyBytes));
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
//            System.err.println("Error: " + e.getMessage());
//        }

        createKeyPairs(new BaseBean());
    }


    final public static boolean verifyToken(String token, String pub_key, String priv_key, ServletRequest request) {

        String subj = null;

        String usern = null;
        String role = null;
        String actionId = null;
        boolean verified = false;
        if (token != null) {


            try {

                RSAPublicKey publickey = (RSAPublicKey) getPublicKey(pub_key);
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(priv_key);

                JWTVerifier jwtV = JWT.require(Algorithm.RSA256(publickey, privateKey)).build();
                DecodedJWT jwtD = jwtV.verify(token.replace("Bearer ", ""));

                subj = jwtD.getSubject();
                usern = jwtD.getClaim("username").asString();
                request.setAttribute("username", usern);
                role = jwtD.getClaim("role").asString();
                request.setAttribute("role", role);
                actionId = jwtD.getClaim("action-id").asString();
                request.setAttribute("action-id", actionId);
                verified = true;

            } catch (JWTVerificationException e) {
                e.printStackTrace();
            }

        }
        return verified;

    }

    final public static boolean verifyToken(String token, String pub_key, String priv_key) {

        boolean verified = false;
        if (token != null) {


            try {

                RSAPublicKey publickey = (RSAPublicKey) getPublicKey(pub_key);
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(priv_key);

                JWTVerifier jwtV = JWT.require(Algorithm.RSA256(publickey, privateKey)).build();
                DecodedJWT jwtD = jwtV.verify(token.replace("Bearer ", ""));
                verified = true;

            } catch (JWTVerificationException e) {
                e.printStackTrace();
            }

        }
        return verified;

    }

    final public static PrivateKey getPrivateKey(String base64PrivateKey) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(
                        base64PrivateKey.getBytes()));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
        }
        return privateKey;
    }

    final public static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey = null;

        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static String extractFileName(Part part, BaseBean requestBean) {
        String contentDisposition = part.getHeader("content-disposition");
        int start = contentDisposition.indexOf("filename=\"");
        int end = contentDisposition.lastIndexOf("\"");
        String fileName = contentDisposition.substring(start + 10, end);
        requestBean.setString("fileName", fileName);
        requestBean.setString("extension", fileName.split("\\.")[fileName.split("\\.").length - 1]);
        return contentDisposition.substring(start + 10, end);
    }

    public static void createJsonToken(BaseBean requestBean) throws JOSEException {

        // Your secret key for signing (for production, store this securely)
        RSAPrivateKey privateKey = (RSAPrivateKey) CustomUtil.getPrivateKey(requestBean.getString("private_key"));

        // Claims to include in the JWT payload
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("Auth")  // Subject of the token (typically the user ID)
                .issuer("app")  // Issuer of the token
                .expirationTime(new Date(new Date().getTime() + Long.parseLong(System.getProperty("auth-token-time"))))  // Token validity period (1 hour)
                .claim("role", requestBean.get("role_name"))
                .claim("username", requestBean.get("username"))
                .claim("action-id", UUID.randomUUID().toString())
                .jwtID(UUID.randomUUID().toString())  // Unique identifier for the JWT
                .build();

        JWTClaimsSet refreshClaimSet = new JWTClaimsSet.Builder()
                .expirationTime(new Date(new Date().getTime() + Long.parseLong(System.getProperty("refresh-token-time"))))  // Token validity period (1 hour)
                .build();

        // Create the signed JWT using RSASSA-PKCS-v1_5 SHA-256 signing algorithm
        JWSSigner signer = new RSASSASigner(privateKey);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        SignedJWT refreshTokenJWT = new SignedJWT(header, refreshClaimSet);
        signedJWT.sign(signer);
        refreshTokenJWT.sign(signer);

        // Serialize the signed JWT to a string representation
        String jwtString = signedJWT.serialize();
        String refreshToken = refreshTokenJWT.serialize();
        requestBean.setString("jwt", jwtString);
        requestBean.setString("refresh-token", refreshToken);
        requestBean.setString("token-expiration", String.valueOf(claimsSet.getExpirationTime().getTime()));
    }

    public static boolean getUserDetailsAndPersistUserData(BaseBean requestBean) {
       return DBHelper.insertUserIntoDB(requestBean);
    }

    public static BaseBean createErrorBean(BaseBean requestBean) {
        BaseBean errorBean = new BaseBean();
        errorBean.setString("message", requestBean.getString("message"));
        errorBean.setString("statusCode", requestBean.getString("statusCode"));
        errorBean.setString("responseCode", requestBean.getString("responseCode"));
        errorBean.setString("responseBody", requestBean.getString("errorBody"));

        return errorBean;

    }

//    public static String extractFileName(Part part, BaseBean requestBean) {
//        String contentDisposition = part.getHeader("content-disposition");
//        int start = contentDisposition.indexOf("filename=\"");
//        int end = contentDisposition.lastIndexOf("\"");
//        String fileName = contentDisposition.substring(start + 10, end);
//        requestBean.setString("fileName", fileName);
//        requestBean.setString("extension", fileName.split("\\.")[fileName.split("\\.").length - 1]);
//        return contentDisposition.substring(start + 10, end);
//    }

    public static Services fetchServiceInfo(String serviceType) {
        Services services;
        switch (serviceType.toLowerCase()) {
            case "airtime":
            case "nip_inflow":
            case "nip_outflow":
            case "up_inflow":
            case "up_outflow":
                services = Services.valueOf(serviceType.toUpperCase());
                break;
            default:
                throw new IllegalArgumentException("Invalid service type");

        }
        return services;
    }

    public static String generateDocumentId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new Date());


        Random random = new Random();
        int randomDigits = 10000000 + random.nextInt(90000000);  // Ensures 8 digits

        return currentDate + randomDigits;
    }

    public static AppModules fetchApplicationModuleInfo(String moduleName) {
        AppModules module;
        switch (moduleName.toLowerCase()) {
            case "funds_recoup":
                module = AppModules.valueOf("funds_recoup".toUpperCase());
                break;
            case "stand_rev":
                module = AppModules.valueOf("standard_reversal".toUpperCase());
                break;
            case "excep_rev":
                module = AppModules.valueOf("exceptional_reversal".toUpperCase());
                break;
            case "unsettled_trxn":
                module = AppModules.valueOf("UNSETTLED_TRANSACTION".toUpperCase());
                break;
            case "settlement_report":
                module = AppModules.valueOf("SETTLEMENT_REPORT".toUpperCase());
                break;
            case "failed_transaction":
                module = AppModules.valueOf("FAILED_TRANSACTION".toUpperCase());
                break;

            default:
                throw new IllegalArgumentException("Invalid module name");

        }
        return module;
    }

}

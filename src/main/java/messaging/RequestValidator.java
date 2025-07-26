package messaging;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import exceptions.CustomException;
import org.apache.commons.validator.routines.EmailValidator;
import util.BaseBean;
import util.JsonServiceConfig;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.JsonObject;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static util.CustomUtil.getPrivateKey;
import static util.CustomUtil.getPublicKey;

public class RequestValidator extends ValidatorUtils {
    public final JsonObject validateRequestFormat(String request, BaseBean valBean) throws IOException {

        JsonObject jobj = JsonUtil.toJsonObject(request);
        valBean.setString(VALIDATION_REPORT, "");

        if (jobj == null) {

            LOG.info("Invalid message format");
            valBean.setString("status_type", ResponseUtil.FAIL);
            valBean.setString("status_code", ResponseUtil.INVALID_MSG_FORMAT);
            valBean.setString(VALIDATION_REPORT, VALIDATION_FAILURE);
            valBean.setString(VALIDATION_MESSAGE, createDefaultResponse(valBean));
            throw new IOException("Invalid Request Format");
        }

        return jobj;

    }

    JsonObject validateBillerId(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateBillerId(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateSessionId(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateSessionId(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateTransactionRequest(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateTransactionRequest(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateCategory(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateTransactionRequest(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    protected void validateParameter(JsonObject request, BaseBean requestBean, String parameter, boolean isString) throws IOException {
        if (!request.containsKey(parameter)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", parameter + " not present");
            throw new IOException(requestBean.get("message"));
        }
        String value = "";
        if (!isString) {
            value = String.valueOf(request.get(parameter));
        } else {
            value = request.getString(parameter);
        }
        requestBean.setString(parameter, value);
    }

    public void validateEmail(JsonObject request, BaseBean requestBean) throws IOException {
        String parameter = "email";
        if (!request.containsKey(parameter)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", parameter + " not present");
            throw new IOException(requestBean.get("message"));
        }
        EmailValidator emailValidator = EmailValidator.getInstance();
        boolean status = emailValidator.isValid(request.getString(parameter));
        if (!status) {
            throw new IOException("Invalid email address");
        }
        if (!request.getString(parameter).endsWith("@ubagroup.com")) {
            throw new IOException("Invalid email domain: ");
        }
        String value = request.getString(parameter);
        requestBean.setString(parameter, value);
    }


    public void validateParameter(JsonObject request, BaseBean requestBean, String parameter) throws IOException {
        validateParameter(request, requestBean, parameter, true);
    }

    public void validateDateParameter(JsonObject request, BaseBean requestBean, String parameter, DateTimeFormatter format) throws IOException {
        if (!request.containsKey(parameter)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", parameter + " not present");
            throw new IOException(requestBean.get("message"));
        }
        try {
            LocalDateTime.parse(request.getString(parameter), format);
            requestBean.setString(parameter, request.getString(parameter));
        }catch (Exception ex) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", ex.getMessage());
            throw new IOException(requestBean.get("message"));

        }

    }

    public void validateDateRange(JsonObject request, BaseBean requestBean,  DateTimeFormatter format) throws IOException {
        String startDateParam = "start_date";   String endDateParam = "end_date";
        LocalDateTime startDate = parseDate(request, startDateParam, format);
        LocalDateTime endDate = parseDate(request, endDateParam, format);

        if (!startDate.isBefore(endDate)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", startDateParam + " must be earlier than " + endDateParam);
            throw new IOException(requestBean.get("message"));
        }
        if (endDate.isAfter(LocalDateTime.now())) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", endDateParam + " cannot be in the future.");
            throw new IOException(requestBean.get("message"));
        }
    }

    private LocalDateTime parseDate(JsonObject request, String dateParam, DateTimeFormatter format) throws IOException {
        try {
            return LocalDateTime.parse(request.getString(dateParam), format);
        } catch (Exception ex) {
            throw new IOException("Invalid date format for parameter: " + dateParam);
        }
    }


    public void validateOptionalDateRange(JsonObject request, BaseBean requestBean, DateTimeFormatter format) throws IOException {
        String startDateParam = "start_date";   String endDateParam = "end_date";
        if (!request.containsKey(startDateParam) || !request.containsKey(endDateParam)) {
            return;
        }

        LocalDateTime startDate = parseDate(request, startDateParam, format);
        LocalDateTime endDate = parseDate(request, endDateParam, format);

        if (!startDate.isBefore(endDate)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", startDateParam + " must be earlier than " + endDateParam);
            throw new IOException(requestBean.get("message"));
        }
        if (endDate.isAfter(LocalDateTime.now())) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", endDateParam + " cannot be in the future.");
            throw new IOException(requestBean.get("message"));
        }
    }


    public void validateOptionalDateParameter(JsonObject request, BaseBean requestBean, String parameter, DateTimeFormatter format) throws IOException {
        if (!request.containsKey(parameter)) {
            return;
        }
        try {
            LocalDateTime.parse(request.getString(parameter), format);
            requestBean.setString(parameter, request.getString(parameter));
        }catch (Exception ex) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", ex.getMessage());
            throw new IOException(requestBean.get("message"));

        }

    }

    public void validateOptionalParameter(JsonObject request, BaseBean requestBean, String parameter, boolean isString) {
        try {
            String value = "";
            if (request.containsKey(parameter)) {
                if (!isString) {
                    value = String.valueOf(request.get(parameter));
                } else {
                    value = request.getString(parameter);
                }
                requestBean.setString(parameter, value);
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void verifyRefreshToken(BaseBean requestBean, String token) throws IOException {
        boolean verified = false;
        if (token != null) {

            try {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");

                RSAPublicKey publickey = (RSAPublicKey) getPublicKey(configBean.getString("public_key"));
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(configBean.getString("private_key"));

                JWTVerifier jwtV = JWT.require(Algorithm.RSA256(publickey, privateKey)).build();
                DecodedJWT jwtD = jwtV.verify(token);

            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "unable to verify refresh token");
                throw new IOException(requestBean.get("message"));
            }
        }
    }

    public static void verifyToken(BaseBean requestBean, String token) throws IOException {
        if (token != null) {

            try {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
                RSAPublicKey publicKey = (RSAPublicKey) getPublicKey(configBean.getString("public_key"));
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(configBean.getString("private_key"));
                JWTVerifier jwtV = JWT
                        .require(Algorithm.RSA256(publicKey, privateKey))
                        .build();
                try {
                    DecodedJWT jwtD = jwtV.verify(token);
                } catch (JWTVerificationException e) {
                    LOG.info(e.getMessage());
                    return;
                }
                throw new IOException("Token is still valid");

            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", ex.getMessage());
                throw new IOException(requestBean.get("message"));
            }
        }
    }


    public static void validateFieldNotEmpty(String fieldName, BaseBean row, BaseBean requestBean) {
        if (row.getString(fieldName).isEmpty()) {
            requestBean.setString("message", "Invalid field: "+fieldName);
            throw new CustomException(requestBean);
        }
    }
}
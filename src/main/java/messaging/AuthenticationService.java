package messaging;

import constants.AppConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import services.executors.Executor;
import util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static util.CustomUtil.createJsonToken;
import static util.CustomUtil.getUserDetailsAndPersistUserData;

public class AuthenticationService extends Common implements Executor {

    final static private Logger LOG = LogManager.getLogger(Common.class);


    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();
        String requestString = validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, true);
        }
        requestBean.setString("ad_request", request);
        BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
        requestBean.setString("private_key", configBean.get("private_key"));
        if (!DBHelper.checkUserDetails(requestBean)) {
            return createReply(requestBean, true);
        }
        boolean status = sendRequest(requestBean, configBean, requestString);
        return createReply(requestBean, status);
    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        //validate email is firstname.lastname
        JsonObject authRequest = null;
        try {
            authRequest = JsonUtil.toJsonObject(request);
            validateParameter(authRequest, requestBean, "username");
            validateParameter(authRequest, requestBean, "password");
            validateParameter(authRequest, requestBean, "token");
            String username = requestBean.getString("username");
            if (username.contains("@ubagroup.com")) {
                username = username.replace("@ubagroup.com", "");
                requestBean.setString("username", username);
            }
        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return request;
    }

    @Override
    protected void processResponse(BaseBean requestBean) {
        try {
            if (requestBean.getString("auth-status").equals("true")) {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
                BaseBean entrustBean = new BaseBean();
                entrustBean.setString("username", requestBean.getString("username"));
                entrustBean.setString("token", requestBean.getString("token"));
                String request = TokenService.generateEntrustRequest(entrustBean);
                String username = CryptoUtils.decrypt(configBean.getString("entrust_name"));
                String password = CryptoUtils.decrypt(configBean.getString("entrust_word"));
                String auth = "Basic ".concat(CryptoUtils.base64Encode(username.concat(":").concat(password)));
                entrustBean.setString("auth_details", auth);
                entrustBean.setString("path", "entrust");
                entrustBean.setString("ad_request", request);
                String env = configBean.getString("cmp");
                boolean isError = false;
                if (env.equals(AppConstants.Constants.LOCAL)) {
                    isError =  validateToken(entrustBean, request);
                    LOG.info("Token status: {}", isError);
//                    isError = false;

                } else if (env.equals(AppConstants.Constants.UBA)) {
                    isError = sendRequest(entrustBean, configBean, "");
                }

                if (!isError && entrustBean.getString("code").equals(ResponseUtil.SUCCESS)) {
                    createJsonToken(requestBean);
                    boolean success = getUserDetailsAndPersistUserData(requestBean);
                    if (!success) {
                        throw new Exception("Authentication failed");
                    }

                } else {
                    requestBean.setString("status_type", ResponseUtil.FAIL);
                    requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
                    requestBean.setString("message", "Entrust authentication failed, please contact admin");
                }
            } else {
                requestBean.setString("status_type", ResponseUtil.FAIL);
                requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
                requestBean.setString("message", "Active directory authentication failed, please contact admin");
            }
        } catch (Exception ex) {
            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
            requestBean.setString("message", "Error occurred during authentication, please contact admin");
            LOG.error(ex.getMessage(), ex);
        }
    }

    private boolean validateToken(BaseBean requestBean, String request) {
        requestBean.setString("code", ResponseUtil.SUCCESS);
        return !DBHelper.validateToken(requestBean, request);
    }


    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp = null;
        JsonObjectBuilder jObjBuil = Json.createObjectBuilder();


        if (procErr) {
            if (requestBean.getString("status_type").isEmpty()) {
                requestBean.setString("status_type", ResponseUtil.FAIL);
            }
            if (requestBean.getString("staus_code").isEmpty()) {
                requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
            }
            if (requestBean.getString("mesage").isEmpty()) {
                requestBean.setString("mesage", "Error authenticating user please contact admin");
            }

        }
        if (requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jObjBuil.add("message", requestBean.getString("message"));
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }

        JsonObjectBuilder response = Json.createObjectBuilder();
        if (    requestBean.get("x-http-status-code").equals(ResponseUtil.HTTP_OK_STATUS) && requestBean.getString("auth-status").equals("true")) {
            response.add("message", "Authentication successful");
            response.add("token", requestBean.getString("jwt"))
                    .add("details", Json.createObjectBuilder()
                            .add("username", requestBean.getString("user"))
                            .add("role", requestBean.getString("role_name"))
                            .add("refresh-token", requestBean.getString("refresh-token"))
                            .add("token-expiration", requestBean.getString("token-expiration"))
                            .build()
                    );
            jsonResp = response.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
        }
//        LOG.info("Response is: " + jsonResp);
        return JsonUtil.toStr(jsonResp);
    }


}


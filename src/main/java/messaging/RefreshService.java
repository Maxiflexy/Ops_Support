package messaging;

import persistence.DBHelper;
import services.executors.Executor;
import util.BaseBean;
import util.JsonServiceConfig;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;

import static util.CustomUtil.createJsonToken;
import static util.CustomUtil.getUserDetailsAndPersistUserData;

public class RefreshService extends Common implements Executor {

    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();String requestString = validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, false);
        }
        BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
        requestBean.setString("private_key", configBean.get("private_key"));
        boolean status = false;
        try {
            if (DBHelper.verifyUserCredentials(requestBean)) {
                createJsonToken(requestBean);
                getUserDetailsAndPersistUserData(requestBean);
                status = true;
            }
        } catch (Exception ex) {
            requestBean.setString("message", ex.getMessage());
        }
        return createReply(requestBean, status);
    }


    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject authRequest = null;
        try {
            authRequest = JsonUtil.toJsonObject(request);
            validateRefreshTokenParameter(authRequest, requestBean);
//            validateTokenParameter(authRequest, requestBean);
            validateParameter(authRequest, requestBean, "username");
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

    }

    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp = null;

        JsonObjectBuilder jObjBuil = Json.createObjectBuilder();


        if (!procErr) {

            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);

        }
        if (requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }

        JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("token", requestBean.getString("jwt"))
                    .add("details", Json.createObjectBuilder()
                            .add("username", requestBean.getString("user"))
                            .add("role", requestBean.getString("role_name"))
                            .add("refresh-token", requestBean.getString("refresh-token"))
                            .add("token-expiration", requestBean.getString("token-expiration"))
                            .build()
                    );
            jsonResp = response.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString(ResponseUtil.APP_SUCCESS))
                    .add("code", requestBean.getString(ResponseUtil.SUCCESS))
            ).build();

//        LOG.info("Response is: " + jsonResp);
        return JsonUtil.toStr(jsonResp);
    }

    private void validateRefreshTokenParameter(JsonObject request, BaseBean requestBean) throws IOException {
        if (!request.containsKey("refresh-token")) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", "refresh-token" + " not present");
            throw new IOException(requestBean.get("message"));
        }
        String token = request.getString("refresh-token");
        requestBean.setString("refresh-token", token);
        verifyRefreshToken(requestBean, token);
    }

    private void validateTokenParameter(JsonObject request, BaseBean requestBean) throws IOException {
        if (!request.containsKey("token")) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", "token" + " not present");
            throw new IOException(requestBean.get("message"));
        }
        String token = request.getString("token");
        requestBean.setString("token", token);
        verifyToken(requestBean, token);
    }

}

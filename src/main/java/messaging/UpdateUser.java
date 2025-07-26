package messaging;


import persistence.DBHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UpdateUser extends Common implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("modified_by", currentUser);
        validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode") && requestBean.getString("validationcode").equals("01")) {
            return createReply(requestBean, false);
        }
        boolean procErr = DBHelper.updateUserRole(requestBean);
        if (procErr) {
            processResponse(requestBean);
        }
        return createReply(requestBean, procErr);
    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject subscriptionRequest = null;
        try {
            subscriptionRequest = JsonUtil.toJsonObject(request);
            validateParameter(subscriptionRequest, requestBean, "user_id", false);
            validateOptionalParameter(subscriptionRequest, requestBean, "new_role_id", false);
            validateOptionalParameter(subscriptionRequest, requestBean, "email", true);
            validateOptionalParameter(subscriptionRequest, requestBean, "firstname", true);
            validateOptionalParameter(subscriptionRequest, requestBean, "lastname", true);
            validateOptionalParameter(subscriptionRequest, requestBean, "username", true);
        } catch (Exception Ex) {
            if (!requestBean.containsKey("message")) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return null;
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
            requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);

        }
        if (requestBean.containsKey("status_type") && requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
                    .add("message", requestBean.getString("message"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("message", "User updated successfully")
                .add("user_id", requestBean.getString("user_id"));
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();
        return JsonUtil.toStr(jsonResp);
    }


}

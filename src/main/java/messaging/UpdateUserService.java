package messaging;

import persistence.DBHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UpdateUserService extends Common implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        String requestString = validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, true);
        }
        boolean status = DBHelper.updateUser(requestBean, request);
        return createReply(requestBean, !status);
    }

    protected String validateRequestBody(String request, BaseBean requestBean) {
        //validate email is firstname.lastname
        JsonObject userRequest = null;
        try {
            userRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(userRequest, requestBean, "firstName", true);
            validateOptionalParameter(userRequest, requestBean, "lastName", true);
            validateOptionalParameter(userRequest, requestBean, "email", true);
            validateParameter(userRequest, requestBean, "id");
            validateOptionalParameter(userRequest, requestBean, "role", true);
            validateOptionalParameter(userRequest, requestBean, "status", true);
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
        if (procErr) {

            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);

        }
        if (requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
                    .add("message", requestBean.getString("message"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("message", "User updated successfully");
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();
        return JsonUtil.toStr(jsonResp);
    }
}

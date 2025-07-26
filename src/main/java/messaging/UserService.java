package messaging;


import persistence.DBHelper;
import services.executors.Executor;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UserService extends Common implements Executor, RequestExecutor {

    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();
//        validateRequestBody(request, requestBean);
        String[] parameters = request.split("::");
        String page = "1";
        String size = "10";
        String id = "";
        try {
            page = parameters[0].equals("null") ? "1" : parameters[0];
            size = parameters[1].equals("null") ? "10" : parameters[1];
            id = parameters[2];
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        requestBean.setString("size", size);
        requestBean.setString("page", page);
        if (!id.equals("null"))
            requestBean.setString("id", id);

//        if (requestBean.containsKey("validationcode") && requestBean.getString("validationcode").equals("01")) {
//            return createReply(requestBean, false);
//        }
        boolean procErr = DBHelper.getUsers(requestBean);
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
            validateParameter(subscriptionRequest, requestBean, "page", false);
            validateParameter(subscriptionRequest, requestBean, "size", false);
            int page = Integer.parseInt(requestBean.get("page"));
            int size = Integer.parseInt(requestBean.get("size"));
            if (page <= 0 || size <= 0) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "page and size should be greater than zero");
            }
        } catch (NumberFormatException ex) {
            if (!requestBean.containsKey("message")) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", ex.getMessage());
            }
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
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        int count = Integer.parseInt(requestBean.getString("user_count"));
        for (int i = 1; i <= count; i++) {
            JsonObject subscription = Json.createObjectBuilder()
                    .add("id", requestBean.get("id_" + i))
                    .add("firstname", requestBean.get("firstname_" + i))
                    .add("lastname", requestBean.getString("lastname_" + i))
                    .add("email", requestBean.getString("email_" + i))
                    .add("role_id", requestBean.getString("role_id_" + i))
                    .add("role_name", requestBean.getString("role_name_" + i))
                    .build();
            jsonArray.add(subscription);
        }
        requestBean.setString("jsonData", JsonUtil.toStr(Json.createObjectBuilder().add("data", jsonArray).build()));

    }

    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp = null;
        JsonObjectBuilder jObjBuil = Json.createObjectBuilder();
        if (!procErr) {
            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
        }
        if (requestBean.containsKey("status_type") &&
                requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
                    .add("message", requestBean.getString("message"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("message", "Users Fetched")
                .add("page", requestBean.getString("page"))
                .add("users_count", requestBean.getString("total_users"))
                .add("size", requestBean.getString("size"))
                .add("data", JsonUtil.toJsonObject(requestBean.getString("jsondata"))
                        .getJsonArray("data"));
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();
        return JsonUtil.toStr(jsonResp);
    }

    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        String page = "1";
        String size = "10";
        String id = "";
        if (request != null) {
            requestBean.setString("id", request);
        }
        requestBean.setString("size", size);
        requestBean.setString("page", page);
        boolean procErr = DBHelper.getUsers(requestBean);
        if (procErr) {
            processResponse(requestBean);
        }
        return createReply(requestBean, procErr);
    }
}

package messaging;


import persistence.DBHelper;
import services.executors.Executor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class RoleService extends Common implements Executor {

    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();
        boolean procErr = DBHelper.getRoles(requestBean);
        if (procErr) {
            processResponse(requestBean);
        }
        return createReply(requestBean, procErr);    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        return null;
    }

    @Override
    protected void processResponse(BaseBean requestBean) {
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        int count = Integer.parseInt(requestBean.getString("role_count"));
        for (int i = 1; i <= count; i++) {
            JsonObject subscription = Json.createObjectBuilder()
                    .add("id", requestBean.get("id_" + i))
                    .add("role_name", requestBean.get("role_name_" + i))
                    .build();
            jsonArray.add(subscription);
        }
        requestBean.setString("jsonData", JsonUtil.toStr(Json.createObjectBuilder().add("data",jsonArray).build()));

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
        response.add("message", "Roles Fetched")
                .add("data", JsonUtil.toJsonObject(requestBean.getString("jsondata"))
                        .getJsonArray("data"));
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();
        return JsonUtil.toStr(jsonResp);
    }

}

package messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import services.executors.Executor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


public class OtpService extends Common implements Executor {

    final static Logger LOG = LogManager.getLogger(DBHelper.class);

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject otpRequest = null;
        try {
            otpRequest = JsonUtil.toJsonObject(request);
            validateParameter(otpRequest, requestBean, "username");
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
            requestBean.setString("status_code", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);

        }

        if (requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }

        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("token", requestBean.getString("token"))
                .add("exp-time", requestBean.getString("exp-date"));
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();

        LOG.info("Response is: " + jsonResp);

        return JsonUtil.toStr(jsonResp);

    }

    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();
         validateRequestBody(request, requestBean);

        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, false);
        }

        boolean status = false;
        try {
                status = DBHelper.createToken(requestBean, request);
        } catch (Exception ex) {
            requestBean.setString("message", ex.getMessage());
        }

        return createReply(requestBean, status);
    }
}

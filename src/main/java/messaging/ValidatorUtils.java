package messaging;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;


public class ValidatorUtils {
    final static Logger LOG = LogManager.getLogger(ValidatorUtils.class);
    final static protected String VALIDATION_SUCCESS = "true";
    final static protected String VALIDATION_FAILURE = "false";
    final static protected String VALIDATION_MESSAGE = "message";
    final static protected String VALIDATION_REPORT = "success";

    protected static final String createDefaultResponse(BaseBean dataBean) {

        JsonObject jsonResp = null;
        String jsonStr = null;
        try {
            jsonResp = Json.createObjectBuilder()
                    .add("status", Json.createObjectBuilder()
                            .add("type", dataBean.getString("status_type"))
                            .add("code", dataBean.getString("status_code"))
                    ).build();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }

        jsonStr = JsonUtil.toStr(jsonResp);
        LOG.info("Response to Client");
        LOG.info(jsonStr);
        return jsonStr;
    }

    void validateBillerId(JsonObject jobj, BaseBean valBean) throws IOException {
        String attribute = "billerId";
        doParameterCheck(jobj, valBean, attribute, ResponseUtil.INVALID_BILLER_ID);

    }

    void validateSessionId(JsonObject jobj, BaseBean valBean) throws IOException {
        String attribute = "sessionId";
        doParameterCheck(jobj, valBean, attribute, ResponseUtil.INVALID_SESSION_ID);

    }
    void validateCategory(JsonObject jobj, BaseBean valBean) throws IOException {
        String attribute = "category";
        doParameterCheck(jobj, valBean, attribute, ResponseUtil.INVALID_CATEGORY);

    }
    void validateTransactionRequest(JsonObject jobj, BaseBean valBean) throws IOException {

    }

    private void doParameterCheck(JsonObject jobj, BaseBean valBean, String attribute) throws IOException {
        doParameterCheck(jobj, valBean, attribute, ResponseUtil.INVALID_BVN);

    }
    private void doParameterCheck(JsonObject jobj, BaseBean valBean, String attribute, String responseCode) throws IOException {
        String tmp = JsonUtil.getJsonObjValue2(jobj, attribute);
        valBean.setString(VALIDATION_REPORT, "");

        if (tmp.trim().equals("")) {
            valBean.setString("status_type", ResponseUtil.FAIL);
            valBean.setString("status_code", responseCode);
            valBean.setString(VALIDATION_REPORT, VALIDATION_FAILURE);
            valBean.setString(VALIDATION_MESSAGE, createDefaultResponse(valBean));
            throw new IOException(attribute + " Invalid");


        }
    }


}

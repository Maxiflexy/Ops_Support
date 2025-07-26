package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;

public class ResponseUtil {


    public static final String ERROR_PERSISTING_ENTRY = "78";
    public static final String CBS_STATUS_TYPE_FAILURE = "12";
    public static final String INVALID_BVN = "12";
    public static final String RESPONSE_NOT_RECEIVED = "23";
    public static final String PEN = "233";
    public static final String CBS_STATUS_TYPE_SUCCESS = "24";
    public static final String UNSUPPORTED_MEDIA_TYPE = "415";
    public static final String INALID_REQUEST = "400";
    public static final String PRECONDITIONS_FAILED = "412";
    public static final String CHANGE_UNSUCCESSFUL = "304";
    public static final String FAIL = "FAIL";
    public static final String APP_SUCCESS = "SUCC";
    public static final String INVALID_TRAN_REF = "13";
    public static final String INVALID_AUTHID = "14";
    public static final String INVALID_BRANCHID = "15";
    public static final String INVALID_BILLER_PRD_ID = "16";
    public static final String INVALID_BILLER_ID = "17";
    public static final String INVALID_TRANS_CHNL = "18";
    public static final String INVALID_BILLER_NAME = "19";
    public static final String INVALID_AMOUNT = "20";
    public static final String INVALID_FEE = "21";
    public static final String INVALID_DEBIT_ACCOUNT_NO = "22";
    public static final String INVALID_PRODUCT_NAME = "23";
    public static final String INVALID_SESSION_ID = "34";
    public static final String INVALID_CREDENTIALS = "44";
    public static final String DATABASE_CONNECTIONO_ERROR = "78";
    public static final String INVALID_CATEGORY = "46";
    public static final String EBILLS_API = "EBILLSAPI";

    final static Logger LOG = LogManager.getLogger(ResponseUtil.class);

    public static final String INVALID_SENDER = "1018";
    public static final String INVALID_RECIVER = "1027";
    public static final String INVALID_MSG_FORMAT = "400";
    public static final String MISSING_TRAN_REF = "5328";
    public static final String INVALID_CREDITORS_ACCOUNT = "1009";
    public static final String MISSING_SENDER = "5329";
    public static final String MISSING_RECEIVER = "5330";
    public static final String MISSING_RECEIVER_CRNCY = "5331";
    public static final String MISSING_REQUIRED_FILED = "5332";
    public static final String GENERIC_PROCESSING_ERROR = "501";
    public static final String SUCCESS = "00";
    public static final String READ_TIMEOUT = "500";

    public static final String ACCP = "ACCP";
    public static final String ACSC = "ACSC";
    public static final String RJCT = "RJCT";
    public static final String UNKW = "UNKW";

    public static final String HTTP_OK_STATUS_1_STR = "201";
    public static final int HTTP_OK_STATUS_1_INT = 201;
    public static final String HTTP_OK_STATUS = "200";
    public static final String HTTP_BAD_REQUEST_STATUS = "400";
    public static final String HTTP_UNAUTHORIZED_STATUS = "401";
    public static final String HTTP_INTERNAL_SERVER_ERROR = "500";
    public static final String HTTP_SERVICE_NOT_AVAILABLE = "503";
    public static final String PAPSS_INST_ID = "XA0001";

    public static enum RespType {
        ACCP, ACSC, RJCT
    }

    public final static String createDefaultResponse(BaseBean dataBean) {

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
            e.printStackTrace();
        }

        jsonStr = JsonUtil.toStr(jsonResp);
        LOG.info("Response to Client");
        LOG.info(jsonStr);
        return jsonStr;
    }


    public final static String createDefaultResponse(String responseCode, int statusCode, String message) {
        JsonObject jsonResp = null;
        String jsonStr = null;
        try {
            jsonResp = Json.createObjectBuilder()
                    .add("status", responseCode)
                    .add("message", message)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        jsonStr = JsonUtil.toStr(jsonResp);
        LOG.info("Response to Client");
        LOG.info(jsonStr);
        return jsonStr;

    }
}
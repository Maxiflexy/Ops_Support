package messaging;

import constants.AppModules;
import persistence.TransactionDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.format.DateTimeFormatter;

public class FailedTransactionExecutor extends Common implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        boolean result = false;
        validateRequestBody(request, requestBean);
        requestBean.setString("username", currentUser);
        requestBean.setString("module_name", AppModules.FAILED_TRANSACTION.name());

        if (!requestBean.getString("validationcode").equals("01")) {
            if (requestBean.containsKey("report") && requestBean.getString("report").equals("true")) {
                result = TransactionDbHelper.fetchFailedTransactions(requestBean, true);
            } else {
                result = TransactionDbHelper.fetchFailedTransactions(requestBean, false);
            }
        }
        return createReply(requestBean, !result);
    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject jsonRequest = null;
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateDateParameter(jsonRequest, requestBean, "start_date", format);
            validateDateParameter(jsonRequest, requestBean, "end_date", format);
            validateDateRange(jsonRequest, requestBean, format);
            validateParameter(jsonRequest, requestBean, "service_type", true);
            validateOptionalParameter(jsonRequest, requestBean, "transaction_status", true);
            validateOptionalParameter(jsonRequest, requestBean, "reversal_status", true);
            validateOptionalParameter(jsonRequest, requestBean, "account_debit_status", true);
            validateOptionalParameter(jsonRequest, requestBean, "page", true);
            validateOptionalParameter(jsonRequest, requestBean, "size", true);
            validateOptionalParameter(jsonRequest, requestBean, "report", true);

        } catch (Exception ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("validationcode", "Bad request");
            }
        }
        return null;
    }

    @Override
    protected void processResponse(BaseBean requestBean) {

    }

    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (!procErr) {
            if (requestBean.containsKey("report") && requestBean.getString("report").equals("true")) {
                builder.add("status", ResponseUtil.SUCCESS);
                builder.add("message", "Request Submitted, you will receive the report via mail.");
            } else {
                builder.add("status", ResponseUtil.SUCCESS);
                builder.add("message", "Success");
                builder.add("page", requestBean.getString("page"));
                builder.add("size", requestBean.getString("size"));
                builder.add("total_rows", requestBean.getString("total"));
                double totalPages = Math.ceil(Integer.parseInt(requestBean.getString("total")) / Double.parseDouble(requestBean.getString("size")));
                builder.add("total_pages", totalPages);
                builder.add("data", JsonUtil.toJsonArray(requestBean.getString("data")));
            }
        }else {
            builder.add("status", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
            builder.add("message", requestBean.getString("message").equals("") ? "An error occured processing data" : requestBean.getString("message"));
        }
        return builder.build().toString();
    }

}

package messaging;

import net.sf.jasperreports.engine.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SettlementReportDbHelper;
import services.executors.DownloadExecutor;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.NumberToWordsConverter;

import javax.json.JsonObject;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReceiptExecutor extends Common implements DownloadExecutor {

    final static Logger LOG = LogManager.getLogger(ReceiptExecutor.class);


    @Override
    public String execute(String request, String currentUser,  HttpServletResponse response) {
        BaseBean requestBean = new BaseBean();
        boolean result = false;
        validateRequestBody(request, requestBean);
        requestBean.setString("username", currentUser);
        BaseBean transactionDetails = new BaseBean();
        if (!requestBean.getString("validationcode").equals("01")) {
            transactionDetails = SettlementReportDbHelper.fetchTransactionDetails(requestBean);
        }
        if (transactionDetails.isEmpty()) {
            throw new IllegalArgumentException("Unable to fetch transaction details");
        }
        createReceipt(transactionDetails, response);
        return "hello";
    }

    private void createReceipt(BaseBean requestBean, HttpServletResponse response) {
        try {
            JasperReport report = JasperCompileManager.compileReport(System.getProperty("report_path"));
            Map<String, Object> properties = new HashMap<>();
            properties.put("SESSION_ID", requestBean.getString("session_id"));
            properties.put("DATE", requestBean.getString("tran_time"));
            properties.put("ACCT_NO", requestBean.getString("accountNo"));
            properties.put("ACCT_NAME", requestBean.getString("biller"));
            properties.put("BEN_ACCT", requestBean.getString("dest_acct_no"));
            properties.put("BEN_NAME", requestBean.getString("dest_acct_name"));
            properties.put("BEN_BANK", requestBean.getString("dest_inst"));
            properties.put("CHANNEL", requestBean.getString("channel"));
            properties.put("AMOUNT", requestBean.getString("amount"));
            properties.put("AMOUNT_IN_WORDS", new NumberToWordsConverter().convertStringValue(requestBean.getString("amount")));
            properties.put("TRAN_STATUS", requestBean.getString("status"));
            properties.put("TRAN_DESC", requestBean.getString("narration"));
            JasperPrint jprint = JasperFillManager.fillReport(report, properties, new JREmptyDataSource());

            OutputStream outStream = response.getOutputStream();
            JasperExportManager.exportReportToPdfStream(jprint, outStream);
            response.setContentType("application/pdf");

        } catch (JRException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject jsonRequest = null;
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "sno", true);
            validateParameter(jsonRequest, requestBean, "document_id", true);
            validateParameter(jsonRequest, requestBean, "service_type", true);

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
        return null;
    }
}

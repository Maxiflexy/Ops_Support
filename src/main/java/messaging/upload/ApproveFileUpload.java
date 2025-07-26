package messaging.upload;

import constants.Roles;
import exceptions.CustomException;
import messaging.Common;
import messaging.CreateRequestMessage;
import messaging.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import persistence.FileUploadDbHelper;
import services.executors.RequestExecutor;
import util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.mail.internet.AddressException;

public class ApproveFileUpload extends Common implements RequestExecutor, CreateRequestMessage {

    final static Logger LOG = LogManager.getLogger(ApproveFileUpload.class);


    @Override
    public String execute(String request, String currentUser, String actionId) {

        BaseBean requestBean = new BaseBean();
        BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
        requestBean.setString("email", currentUser);
        JsonObject requestObject = JsonUtil.toJsonObject(request);
        boolean success = validateRequestBody(request, requestBean).equals("true") && !"01".equals(requestBean.getString("validationcode"));
        final CreateRequestMessage requestMessage = this;

//        send message to ESB service
        try {
            requestBean.setString("action", "unapproved");
            if (success) {
                success = FileUploadDbHelper.approveUpload(requestBean, requestObject);
                if (success) {
                    sendRequest(requestBean, configBean, request, requestMessage);
                    sendEmailNotification(requestBean);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            success = false;
        }

        return createReply(requestBean, success);
    }


    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "document_id", true);
            validateParameter(jsonRequest, requestBean, "status", false);
            validateOptionalParameter(jsonRequest, requestBean, "message", true);
            response = true;

        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }

        return String.valueOf(response);
    }

    @Override
    protected void processResponse(BaseBean requestBean) {
        try {
            String response = requestBean.getString("auth-status");
            requestBean.setString("ibm_status", "");
            requestBean.setString("resp_flag", "N");
            if (!response.isEmpty()) {
                JsonObject jsonResponse = JsonUtil.toJsonObject(response);
                String esbStatus = jsonResponse.getString("status");
                String documentId = jsonResponse.getString("documentId");
                requestBean.setString("ibm_status", esbStatus);
                requestBean.setString("resp_flag", "Y");
                requestBean.setString("ibm_document_id", documentId);
            }

            boolean status = FileUploadDbHelper.saveESBResponse(requestBean);
            requestBean.setString("db_status", String.valueOf(status));
        } catch (Exception Ex) {
            LOG.error(Ex.getMessage(), Ex);
            requestBean.setString("db_status", String.valueOf(false));

        }
    }

    public String createReply(BaseBean requestBean, Boolean error) {

        JsonObject jsonResp;
        if (!error) {
            requestBean.setString("responseCode", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
            throw new CustomException(requestBean);
        }

        String approvalMessage = "Approval process have been initialized";
        String cancelledMessage = "Request cancelled ";
        String message = requestBean.getString("status").equals("true") ? approvalMessage : cancelledMessage;
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("id", requestBean.getString("document_id"))
                        .build())
                .build();
        return jsonResp.toString();
    }

    @Override
    public String createRequestMessage(BaseBean requestBean, BaseBean configBean, String request) {
        if (FileUploadDbHelper.fetchUploadSummaryWithDocumentId(requestBean)) {
            return requestBean.getString("data");
        }
        LOG.info("Unable to fetch approved summary");
        throw new CustomException(requestBean);
    }

    private void sendEmailNotification(BaseBean requestBean) throws AddressException {
        try {
            Email email = new Email();
            email.setHasAttachment(false);
            email.setTitle("Approval Status Notification");
            BaseBean userBean = new BaseBean();
            userBean.setString("username", requestBean.getString("initiator"));
            DBHelper.fetchUserDetailsWithUsername(userBean);
            email.setTo(userBean.getString("email"));
            email.setCc(DBHelper.fetchAllUsersWithRole(Roles.APPROVER.name(), requestBean));
            email.setContentType("text/html");
            email.setContent(NotificationService.generateApprovedOrCancelledEmailNotification(requestBean));
            EmailUtil.sendAsyncEmailNotification(email);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }
}

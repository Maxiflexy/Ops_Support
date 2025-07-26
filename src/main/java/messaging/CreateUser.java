package messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import services.executors.RequestExecutor;
import util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Collections;

public class CreateUser extends Common implements RequestExecutor {

    static final private Logger LOG = LogManager.getLogger(CreateUser.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        String requestString = validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, true);
        }
        boolean status = DBHelper.createUser(requestBean, request);

        try {
            Email email = new Email();
            email.setTo(requestBean.get("email"));
            email.setCc(Collections.singletonList(currentUser));
            email.setHasAttachment(true);
            email.setTitle(requestBean.getString("Comprehensive Airtime Management System"));
            email.setContent(NotificationService.generateUserCreationNotificationTemplate(requestBean));
            email.setContentType("text/html");
            EmailUtil.sendAsyncEmailNotification(email);
        } catch (Exception e) {
            LOG.error(e);
        }
        return createReply(requestBean, !status);
    }

    protected String validateRequestBody(String request, BaseBean requestBean) {
        //validate email is firstname.lastname
        JsonObject userRequest = null;
        try {
            userRequest = JsonUtil.toJsonObject(request);
            validateParameter(userRequest, requestBean, "firstName");
            validateParameter(userRequest, requestBean, "lastName");
            validateParameter(userRequest, requestBean, "email");
            validateParameter(userRequest, requestBean, "role");
            validateEmail(userRequest, requestBean);

            String username = requestBean.getString("email");
            if (username.contains("@ubagroup.com")) {
                username = username.replace("@ubagroup.com", "");
            }
            requestBean.setString("username", username);
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
        response.add("message", "User created successfully");
        jsonResp = response.add("status", Json.createObjectBuilder()
                .add("type", ResponseUtil.APP_SUCCESS)
                .add("code", ResponseUtil.SUCCESS)
        ).build();
        return JsonUtil.toStr(jsonResp);
    }

}

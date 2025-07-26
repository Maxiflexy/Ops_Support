package messaging.upload;

import exceptions.CustomException;
import messaging.RequestValidator;
import persistence.FileUploadDbHelper;
import services.executors.FileExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.Part;
import java.util.List;
import java.util.UUID;

public class DeleteRequest extends RequestValidator implements FileExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId, List<Part> parts) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("action", "delete");
        requestBean.setString("email", currentUser);
        requestBean.setString("document-id", UUID.randomUUID().toString());
        boolean success = false;

        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            success = FileUploadDbHelper.saveNewRecord(null, requestBean);

        }
        return createReply(requestBean, success);
    }

    private boolean validateRequest(BaseBean requestBean, String request) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "module_name", true);
            validateParameter(jsonRequest, requestBean, "id", true);

            response = true;

        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return response;
    }

    public String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(requestBean);
        }

        String message = "Delete request created, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("id", requestBean.getString("document-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}

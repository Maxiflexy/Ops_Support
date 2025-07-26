package services.fileservlets;

import exceptions.CustomException;
import messaging.upload.ApproveFileUpload;
import services.servlets.CustomBaseServlet;
import util.JsonUtil;
import util.ResponseUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static util.ResponseUtil.HTTP_INTERNAL_SERVER_ERROR;
import static util.ResponseUtil.createDefaultResponse;

public class Approveupload extends CustomBaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            String user = (String) servletRequest.getAttribute("username");
            String actionId = servletRequest.getParameter("action-id");
            String id = servletRequest.getParameter("document_id");
            String request = JsonUtil.toBuilder(JsonUtil.toJsonObject(requestStr))
                    .add("document_id", id)
                    .build().toString();
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new ApproveFileUpload());
                respStr = getExecutor().execute(request, user, actionId);
                out.print(respStr);

            } else {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(respStr);

            }
        } catch (CustomException e) {
            LOG.error("An error occurred:: {}", e.getMessage());
            servletResponse.setStatus(e.getStatusCode());
            assert out != null;
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
        } catch (Exception e) {
            assert out != null;
            servletResponse.setStatus(Integer.parseInt(HTTP_INTERNAL_SERVER_ERROR));
            out.print(createDefaultResponse(HTTP_INTERNAL_SERVER_ERROR, Integer.parseInt(HTTP_INTERNAL_SERVER_ERROR), e.getMessage()));
            LOG.error(e.getMessage(), e);
        }
        try {
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}

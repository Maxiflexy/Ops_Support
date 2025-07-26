package services;

import messaging.NotificationService;
import messaging.OtpService;
import services.servlets.BaseServlet;
import util.*;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class EmailTest extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                JsonObject jobj = JsonUtil.toJsonObject(requestStr);
                String to = jobj.getString("to");
                String emailTemp = NotificationService.generateApprovedOrCanceledFundsRecoupNotification(new BaseBean());
                Email email = new Email();
                email.setTo(to);
                email.setContentType("text/html");
                email.setContent(emailTemp);
                email.setTitle("Testing");
                email.setHasAttachment(false);
                EmailUtil.sendAsyncEmailNotification(email);
                respStr = "Okay";
                out.print(respStr);

            } else {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(respStr);

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        doPost(req, resp);
    }
}

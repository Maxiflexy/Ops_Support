package services;

import messaging.CreateUser;
import messaging.UpdateUserService;
import messaging.UserService;
import services.servlets.CustomBaseServlet;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static util.JsonUtil.addObject;

public class UserManagementServlet extends CustomBaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;
            String id = servletRequest.getParameter("id");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new UserService());
            respStr = getExecutor().execute(id, "", "");
            out.print(respStr);

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
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String email = (String) servletRequest.getAttribute("username");
            String respStr = null;
            String actionId = UUID.randomUUID().toString();
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new CreateUser());
                respStr = getExecutor().execute(requestStr, email, actionId);
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
    protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String email = (String) servletRequest.getAttribute("username");
            String id = servletRequest.getParameter("id");
            String respStr = null;
            String actionId = UUID.randomUUID().toString();
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            JsonObjectBuilder builder = Json.createObjectBuilder();
            JsonObject jsonRequest = JsonUtil.toJsonObject(requestStr);
            if (jsonRequest != null) {
                addObject(builder, "firstName", jsonRequest.getString("firstName"));
                addObject(builder, "lastName", jsonRequest.getString("lastName"));
                addObject(builder, "email", jsonRequest.getString("email"));
                addObject(builder, "role", jsonRequest.getString("role"));
                addObject(builder, "status", jsonRequest.getString("status"));
                addObject(builder, "id",id);
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new UpdateUserService());
                respStr = getExecutor().execute(builder.build().toString(), email, actionId);
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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}

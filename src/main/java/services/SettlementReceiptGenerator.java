package services;

import messaging.ReceiptExecutor;
import services.servlets.BaseFileDownloadServlet;
import services.servlets.CustomBaseServlet;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.createDefaultResponse;

public class SettlementReceiptGenerator extends BaseFileDownloadServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        try {
            String username = (String) servletRequest.getAttribute("username");
            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "sno", servletRequest.getParameter("sno"));
            addObject(builder, "document_id", servletRequest.getParameter("document_id"));
            addObject(builder, "service_type", servletRequest.getParameter("service_type"));

            servletResponse.setStatus(Integer.parseInt(ResponseUtil.HTTP_OK_STATUS));
            setExecutor(new ReceiptExecutor());
            getExecutor().execute(builder.build().toString(), username, servletResponse);
        }catch (IllegalArgumentException e){
            servletResponse.setContentType("application/json");
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter out = servletResponse.getWriter();
            out.println(createDefaultResponse(String.valueOf(HttpServletResponse.SC_BAD_REQUEST), HttpServletResponse.SC_BAD_REQUEST, e.getMessage()));
            out.close();
            LOG.error(e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

}

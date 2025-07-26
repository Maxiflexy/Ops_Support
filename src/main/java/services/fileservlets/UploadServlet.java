package services.fileservlets;

import exceptions.CustomException;
import messaging.upload.DeleteRequest;
import messaging.upload.FetchFile;
import messaging.upload.UploadFileService;
import services.servlets.FileServlet;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.createDefaultResponse;

public class UploadServlet extends FileServlet {


    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            String actionId = UUID.randomUUID().toString();

            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "module_name", servletRequest.getParameter("module"));
            addObject(builder, "action", servletRequest.getParameter("category"));
            addObject(builder, "document_id", servletRequest.getParameter("document_id"));
            addObject(builder, "fetch_content", servletRequest.getParameter("fetch-content"));
            addObject(builder, "id", servletRequest.getParameter("id"));

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new FetchFile());


            respStr = getExecutor().execute(builder.build().toString(), user, actionId, null);
            out.print(respStr);
        } catch (CustomException e) {
            assert out != null;
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            servletResponse.setStatus(e.getStatusCode());
            LOG.error(e.getMessage(), e);

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
        PrintWriter out = null;
        try {
            LOG.info("IN UPLOAD FILE SERVLET");
//            String requestStr = getBody(servletRequest);
            String path = getServletContext().getRealPath("/");
            List<Part> parts = (List<Part>) servletRequest.getParts();

            out = servletResponse.getWriter();
            String respStr = null;
            String user = (String) servletRequest.getAttribute("username");
            String actionId = servletRequest.getParameter("action-id");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
//            if (APPLICATION_FORMDATA.equalsIgnoreCase(servletRequest.getContentType())) {
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new UploadFileService());
            LOG.info("FILE:::{}", parts);
            respStr = getExecutor().execute(path, user, actionId, parts);
            out.print(respStr);
        } catch (CustomException e) {
            LOG.info("Custom Exception ::::::::::::");
            e.printStackTrace();
            assert out != null;
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            servletResponse.setStatus(e.getStatusCode());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.info("AN ERROR OCCURRED:: {}", e.getMessage());
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
        PrintWriter out = null;
        try {
            LOG.info("IN UPLOAD FILE SERVLET");
//            String requestStr = getBody(servletRequest);
            String path = getServletContext().getRealPath("/");
            List<Part> parts = null;

            out = servletResponse.getWriter();
            String respStr = null;
            String user = (String) servletRequest.getAttribute("username");
            String actionId = servletRequest.getParameter("action-id");
            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "module_name", servletRequest.getParameter("module_name"));
            addObject(builder, "id", servletRequest.getParameter("id"));
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
//            if (APPLICATION_FORMDATA.equalsIgnoreCase(servletRequest.getContentType())) {
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new DeleteRequest());
            LOG.info("FILE:::{}", parts);
            respStr = getExecutor().execute(builder.build().toString(), user, actionId, null);
            out.print(respStr);
        } catch (CustomException e) {
            LOG.info("Custom Exception ::::::::::::");
            e.printStackTrace();
            assert out != null;
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            servletResponse.setStatus(e.getStatusCode());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.info("AN ERROR OCCURRED:: {}", e.getMessage());
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
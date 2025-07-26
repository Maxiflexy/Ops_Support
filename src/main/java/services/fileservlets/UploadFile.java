package services.fileservlets;

import constants.ModuleName;
import constants.ServiceType;
import exceptions.CustomException;
import exceptions.InvalidFileException;
import messaging.upload.FetchFile;
import org.apache.poi.ss.usermodel.*;
import services.FileUploadService;
import services.FileValidator;
import services.servlets.FileServlet;
import util.CustomUtil;
import util.EmailNotificationService;
import util.FileProcessingResult;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.HTTP_INTERNAL_SERVER_ERROR;
import static util.ResponseUtil.createDefaultResponse;


@MultipartConfig
public class UploadFile extends FileServlet {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final FileUploadService fileUploadService = FileUploadService.getInstance();
    private final EmailNotificationService emailNotificationService = EmailNotificationService.getInstance();

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            String actionId = UUID.randomUUID().toString();

            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "module_name", servletRequest.getParameter("module_name"));
            addObject(builder, "service_type", servletRequest.getParameter("service_type"));
            addObject(builder, "action", servletRequest.getParameter("category"));
            addObject(builder, "document_id", servletRequest.getParameter("document_id"));
            addObject(builder, "fetch_content", servletRequest.getParameter("fetch_content"));
            addObject(builder, "page", servletRequest.getParameter("page"));
            addObject(builder, "size", servletRequest.getParameter("size"));
            addObject(builder, "start_date", servletRequest.getParameter("start_date"));
            addObject(builder, "end_date", servletRequest.getParameter("end_date"));
            addObject(builder, "tran_status", servletRequest.getParameter("tran_status"));
            addObject(builder, "report", servletRequest.getParameter("report"));


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
            assert out != null;
            servletResponse.setStatus(Integer.parseInt(HTTP_INTERNAL_SERVER_ERROR));
            out.print(createDefaultResponse(HTTP_INTERNAL_SERVER_ERROR, Integer.parseInt(HTTP_INTERNAL_SERVER_ERROR), e.getMessage()));
            LOG.error(e.getMessage(), e);
        }
        try {
            out.flush();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        Part filePart = request.getPart("file");
        String moduleNameStr = request.getParameter("module_name");
        String serviceTypeStr = request.getParameter("service_type");

        // Validate file and parameters
        FileValidator fileValidator = new FileValidator();
        try {
            fileValidator.validateFile(filePart);
            ModuleName moduleName = fileValidator.validateModuleName(moduleNameStr);
            ServiceType serviceType = fileValidator.validateServiceType(serviceTypeStr);

            // Generate document ID
            String documentId = CustomUtil.generateDocumentId();
            String user = (String) request.getAttribute("username");

            // Send immediate response to the client
            response.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            response.setContentType(APPLICATION_JSON);
            response.getWriter().write("{\"status\":\"00\",\"message\":\"File uploaded successfully, pending approval from admin\",\"data\":{\"id\":\""
                    + documentId + "\"}}");

            // Process the file asynchronously
            //executorService.submit(() -> fileUploadService.processFile(filePart, moduleName, serviceType, documentId, user));
            executorService.submit(() -> {
                try {
                    FileProcessingResult result = null;

                    if (moduleName == ModuleName.SETTLEMENT_REPORT) {

                        fileUploadService.uploadReportFile(
                                filePart, serviceType, documentId, user, moduleName);

                        // Send notification emails
                        emailNotificationService.sendUploadNotification(
                                documentId,
                                moduleName,
                                user
                        );

                    } else {

                        result = fileUploadService.processFile(
                                filePart, moduleName, serviceType, documentId, user);

                        // Send notification emails
                        emailNotificationService.sendUploadNotification(
                                documentId,
                                moduleNameStr,
                                result.getTotalUnapproved(),
                                result.getTotalValidated(),
                                result.getTotalFailed(),
                                user
                        );
                    }

                } catch (Exception e) {
                    e.printStackTrace(); // Log properly
                }
            });

        } catch (InvalidFileException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"99\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {}
}
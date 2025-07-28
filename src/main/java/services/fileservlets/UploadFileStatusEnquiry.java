package services.fileservlets;

import constants.ServiceType;
import exceptions.InvalidFileException;
import services.FileValidator;
import services.StatusEnquiryService;
import services.servlets.FileServlet;
import util.CustomUtil;
import util.ResponseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@MultipartConfig
public class UploadFileStatusEnquiry extends FileServlet {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final StatusEnquiryService statusEnquiryService = StatusEnquiryService.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        Part filePart = request.getPart("file");
        String serviceTypeStr = request.getParameter("service_type");

        // Validate file and parameters
        FileValidator fileValidator = new FileValidator();
        try {
            fileValidator.validateFile(filePart);
            ServiceType serviceType = validateServiceTypeForStatusEnquiry(serviceTypeStr);

            // Generate document ID
            String documentId = CustomUtil.generateDocumentId();
            String user = (String) request.getAttribute("username");
            //String user = "test_user";

            // Send immediate response to the client
            response.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            response.setContentType(APPLICATION_JSON);
            response.getWriter().write("{\"status\":\"00\",\"message\":\"" +
                    "File Uploaded successfully, processing is in progress and you will be " +
                    "notified when processing is completed\",\"data\":{\"id\":\""
                    + documentId + "\"}}");

            // Process the file asynchronously
            executorService.submit(() -> {
                try {
                    statusEnquiryService.processStatusEnquiryFile(filePart, serviceType, documentId, user);

                    //String userEmail = statusEnquiryService.getUserEmail(user);
                    // Send notification emails
//                    emailNotificationService.sendUploadNotification(
//                            documentId,
//                            user
//                    );
                } catch (Exception e) {
                    LOG.error("Error processing status enquiry file: " + e.getMessage(), e);
                }
            });

        } catch (InvalidFileException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"99\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

        try {
            // Get pagination parameters
            String pageStr = servletRequest.getParameter("page");
            String sizeStr = servletRequest.getParameter("size");

            int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
            int size = (sizeStr != null && !sizeStr.isEmpty()) ? Integer.parseInt(sizeStr) : 10;

            // Ensure size doesn't exceed maximum of 10
            if (size > 10) {
                size = 10;
            }

            // Ensure page is at least 1
            if (page < 1) {
                page = 1;
            }

            // Get filter parameters
            String documentId = servletRequest.getParameter("document_id");
            String serviceType = servletRequest.getParameter("service_type");
            String creationDate = servletRequest.getParameter("creation_date");

            // Get file upload records
            String jsonResponse = statusEnquiryService.getFileUploadRecords(
                    page, size, documentId, serviceType, creationDate);

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            servletResponse.getWriter().write(jsonResponse);

        } catch (NumberFormatException e) {
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            servletResponse.getWriter().write("{\"status\":\"99\",\"message\":\"Invalid page or size parameter\"}");
        } catch (Exception e) {
            LOG.error("Error in doGet: " + e.getMessage(), e);
            servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            servletResponse.getWriter().write("{\"status\":\"99\",\"message\":\"Internal server error\"}");
        }
    }

    private ServiceType validateServiceTypeForStatusEnquiry(String serviceTypeStr) {
        if (serviceTypeStr == null || serviceTypeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("service_type parameter is required");
        }

        try {
            ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase().replace("-", "_"));

            // Validate that it's one of the allowed service types for status enquiry
            switch (serviceType) {
                case AIRTIME:
                case NIP_OUTFLOW:
                case NIP_INFLOW:
                case UP_INFLOW:
                case UP_OUTFLOW:
                    return serviceType;
                default:
                    throw new IllegalArgumentException("Invalid service_type. Allowed values: AIRTIME, NIP_OUTFLOW, NIP_INFLOW, UP_INFLOW, UP_OUTFLOW");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid service_type. Allowed values: AIRTIME, NIP_OUTFLOW, NIP_INFLOW, UP_INFLOW, UP_OUTFLOW");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().write("{\"status\":\"99\",\"message\":\"PUT method not supported\"}");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().write("{\"status\":\"99\",\"message\":\"DELETE method not supported\"}");
    }

    @Override
    public void destroy() {
        super.destroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
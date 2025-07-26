package messaging.upload;

import com.opencsv.exceptions.CsvValidationException;
import constants.FileUploadModules;
import exceptions.CustomException;
import messaging.fileUtils.FileValidator;
import messaging.fileUtils.CSVReader;
import messaging.fileUtils.Reader;
import messaging.fileUtils.XLSXReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.executors.FileExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UploadFileService implements FileExecutor {

    public final static Logger LOG = LogManager.getLogger(UploadFileService.class);

    @Override
    public String execute(String request, String currentUser, String actionId, List<Part> parts) {

        BaseBean requestBean = new BaseBean();
        boolean isSuccess = false;
        String moduleName = "";
        Part filePart = null;
        try {
            for (Part part : parts) {
                switch (part.getName()) {
                    case "module_name":
                        moduleName = convertInputStreamToString(part.getInputStream());
                        requestBean.setString("module_name", moduleName);
                        requestBean.setString("action", "create");
                        requestBean.setString("email", currentUser);

                        break;
                    case "file":
                        String id = UUID.randomUUID().toString();
                        filePart = part;
                        String fileName = CustomUtil.extractFileName(part, requestBean);
                        requestBean.setString("document-id", id);
                        requestBean.setString("fileName", fileName);
                        break;
                }
            }
            isSuccess = initiateFileProcessing(moduleName, filePart, requestBean);


        } catch (CsvValidationException e) {
            LOG.error("Exception occurred processing file: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error("Exception occurred processing file: {}", e.getMessage());
            e.printStackTrace();
        }
        return createReply(requestBean, isSuccess);
    }

    private static boolean initiateFileProcessing(String moduleName, Part inputStream, BaseBean requestBean) throws CsvValidationException, IOException {
        boolean success = false;
        if (inputStream == null) {
            requestBean.setString("message", "csv file required");
            throw new CustomException(requestBean);
        }

        String fileType = requestBean.getString("extension");
        Reader reader = fileType.equals("csv") ? new CSVReader() : fileType.equals("xlsx") ? new XLSXReader() : null;

        FileValidator validator = getFileValidator(moduleName, requestBean, reader);
        if (validator.validateFile(inputStream, requestBean, reader)) {
            LOG.info("Initiate file processing file: Document ID: {}", requestBean.getString("document-id") );
            success = validator.persistData(requestBean);
        }
        return success;
    }


    public static String convertInputStreamToString(InputStream inputStream) {

        StringBuilder textBuilder = new StringBuilder();
        try (java.io.Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, StandardCharsets.UTF_8))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return textBuilder.toString();
    }

    public String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(requestBean);
        }

        String message = "File Uploaded successfully, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("id", requestBean.getString("document-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }

    private static FileValidator getFileValidator(String moduleName, BaseBean requestBean, Reader reader) {

        FileValidator validator;
        try {
            switch (FileUploadModules.valueOf(moduleName.toUpperCase())) {
                case FEE_CONFIG:
//                    validator = new FeeConfigValidator(reader);
                    validator =null;
                    break;

                default:
                    requestBean.setString("message", "Invalid module name");
                    requestBean.setString("statusCode", "400");
                    throw new CustomException(requestBean);

            }
        } catch (IllegalArgumentException ex) {
            requestBean.setString("message", ex.getMessage());
            requestBean.setString("statusCode", "400");
            throw new CustomException(requestBean);

        }
        return validator;

    }
}

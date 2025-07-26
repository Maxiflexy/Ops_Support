package services;

import constants.ModuleName;
import constants.ServiceType;
import exceptions.InvalidFileException;
import org.apache.poi.ss.usermodel.*;
import javax.servlet.http.Part;
import java.io.*;

public class FileValidator {

    private static final long MAX_FILE_SIZE = (long) (1.5 * 1024 * 1024); // 1.5 MB in bytes

    public void validateFile(Part filePart) throws InvalidFileException {
        if (filePart == null || !isValidExcelFile(filePart.getSubmittedFileName())) {
            throw new InvalidFileException("Invalid file type, please upload an Excel file.");
        }
        if (filePart.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds the 5 MB limit.");
        }
    }

    private boolean isValidExcelFile(String fileName) {
        return fileName.endsWith(".xls") || fileName.endsWith(".xlsx");
    }

    public ModuleName validateModuleName(String moduleNameStr) {
        try {
            return ModuleName.valueOf(moduleNameStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid module_name parameter");
        }
    }

    public ServiceType validateServiceType(String serviceTypeStr) {
        try {
            return ServiceType.valueOf(serviceTypeStr.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid service_type parameter");
        }
    }
}


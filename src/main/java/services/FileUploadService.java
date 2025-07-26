package services;

import constants.ModuleName;
import constants.ServiceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import persistence.TransactionService;
import util.FileProcessingResult;
import util.SettlementReportRecord;

import javax.servlet.http.Part;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUploadService {

    private final TransactionService transactionService;
    final static Logger LOG = LogManager.getLogger(FileUploadService.class);

    private static final class FileUploadServiceHolder {
        private static final FileUploadService instance = new FileUploadService();
    }

    public static FileUploadService getInstance() {
        return FileUploadServiceHolder.instance;
    }

    private FileUploadService() {
        this.transactionService = TransactionService.getInstance();
    }

    public void uploadReportFile(
            Part filePart, ServiceType serviceType, String documentId, String currentUser, ModuleName moduleName) {

        List<SettlementReportRecord> settlementRecords = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream fileContent = filePart.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(fileContent);
            Sheet sheet = workbook.getSheetAt(0);

            boolean isHeader = true;

            // Iterate over rows and extract data
            for (Row row : sheet) {
                if (isHeader) {
                    isHeader = false; // Skip header row
                    continue;
                }

                SettlementReportRecord record = new SettlementReportRecord();

                record.setSn((dataFormatter.formatCellValue(row.getCell(0))));
                record.setChannel(dataFormatter.formatCellValue(row.getCell(1)));
                record.setSessionId(dataFormatter.formatCellValue(row.getCell(2)));
                record.setTransactionType(dataFormatter.formatCellValue(row.getCell(3)));
                record.setResponse(dataFormatter.formatCellValue(row.getCell(4)));
                String amountStr = dataFormatter.formatCellValue(row.getCell(5)).trim().replace(",", "");
                if (amountStr.startsWith("(") && amountStr.endsWith(")")) {
                    amountStr = amountStr.substring(1, amountStr.length() - 1);
                    //record.setAmount(-Double.parseDouble(amountStr));
                    record.setAmount(Double.parseDouble(amountStr));
                } else {
                    record.setAmount(Double.parseDouble(amountStr));
                }
                record.setTransactionTime(dataFormatter.formatCellValue(row.getCell(6)));
                record.setOriginatorInstitution(dataFormatter.formatCellValue(row.getCell(7)));
                record.setOriginatorBiller(dataFormatter.formatCellValue(row.getCell(8)));
                record.setDestinationInstitution(dataFormatter.formatCellValue(row.getCell(9)));
                record.setDestinationAccountName(dataFormatter.formatCellValue(row.getCell(10)));
                record.setDestinationAccountNo((dataFormatter.formatCellValue(row.getCell(11))));
                record.setNarration(dataFormatter.formatCellValue(row.getCell(12)));
                record.setPaymentReference(dataFormatter.formatCellValue(row.getCell(13)));
                record.setLast12DigitsOfSessionId(dataFormatter.formatCellValue(row.getCell(14)));

                record.setDocumentId(documentId);
                record.setServiceType(serviceType.name());
                record.setUploadedBy(currentUser);

                settlementRecords.add(record);
            }

            transactionService.saveFileUploadMetadata(
                    filePart.getSubmittedFileName(),
                    documentId,
                    moduleName,
                    serviceType,
                    0,
                    0,
                    0,
                    currentUser
            );

            transactionService.batchSaveSettlementReport(settlementRecords);

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    public FileProcessingResult processFile(Part filePart, ModuleName moduleName, ServiceType serviceType, String documentId, String currentUser) {
        List<String> transactionReferences = new ArrayList<>();
        int totalUnapprovedTran = 0;
        int totalValidated = 0;
        int totalFailed = 0;

        try (InputStream fileContent = filePart.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(fileContent);
            Sheet sheet = workbook.getSheetAt(0);

            boolean isHeader = true;

            // Extract transaction references
            for (Row row : sheet) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                Cell cell = row.getCell(0);
                if (cell != null) {
                    String reference = cell.getStringCellValue();
                    transactionReferences.add(reference);
                }
            }

            totalUnapprovedTran = transactionReferences.size();

            transactionService.saveFileUploadMetadata(
                    filePart.getSubmittedFileName(),
                    documentId,
                    moduleName,
                    serviceType,
                    totalUnapprovedTran,
                    0, // Initial placeholder for totalValidated
                    0, // Initial placeholder for totalFailed
                    currentUser
            );

            // Process transactions and save metadata
            totalValidated = transactionService.validateAndSaveTransactions(transactionReferences, moduleName, serviceType, documentId);
            totalFailed = totalUnapprovedTran - totalValidated;

            transactionService.updateFileUploadMetadata(documentId, totalValidated, totalFailed);

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return new FileProcessingResult(totalUnapprovedTran, totalValidated, totalFailed);
    }
}
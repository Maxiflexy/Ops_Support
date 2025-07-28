package services;

import constants.AppConstants;
import constants.ServiceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import persistence.ConnectionUtil;

import javax.servlet.http.Part;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatusEnquiryService {

    private static final Logger LOG = LogManager.getLogger(StatusEnquiryService.class);

    private static final class StatusEnquiryServiceHolder {
        private static final StatusEnquiryService instance = new StatusEnquiryService();
    }

    public static StatusEnquiryService getInstance() {
        return StatusEnquiryServiceHolder.instance;
    }

    private StatusEnquiryService() {}

    public void processStatusEnquiryFile(Part filePart, ServiceType serviceType,
                                         String documentId, String user) {
        List<String> referenceIds = new ArrayList<>();
        int recordCount = 0;

        try (InputStream fileContent = filePart.getInputStream()) {
            // Extract reference IDs from Excel file (Session ID or TOPUP_REF_ID)
            Workbook workbook = new XSSFWorkbook(fileContent);
            Sheet sheet = workbook.getSheetAt(0);

            boolean isHeader = true;
            for (Row row : sheet) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                Cell cell = row.getCell(0);
                if (cell != null) {
                    String referenceId = cell.getStringCellValue().trim();
                    if (!referenceId.isEmpty()) {
                        referenceIds.add(referenceId);
                        recordCount++;
                    }
                }
            }

            // Save initial metadata
            saveFileUploadMetadata(documentId, serviceType, filePart.getSubmittedFileName(),
                    user, recordCount);

            // Process and validate records
            int validatedCount = processAndValidateRecords(referenceIds, serviceType, documentId);

            // Update validation status
            updateValidationStatus(documentId, validatedCount > 0 ? "true" : "false");

            LOG.info("Processed status enquiry file. DocumentId: {}, Total Records: {}, Validated: {}",
                    documentId, recordCount, validatedCount);

        } catch (Exception e) {
            LOG.error("Error processing status enquiry file for documentId: " + documentId, e);
            try {
                updateValidationStatus(documentId, "false");
            } catch (Exception updateEx) {
                LOG.error("Error updating validation status", updateEx);
            }
        }
    }

    private void saveFileUploadMetadata(String documentId, ServiceType serviceType,
                                        String fileName, String user, int recordCount) {
        String query = "INSERT INTO " + AppConstants.DbTables.FILE_METADATA +
                " (DOCUMENT_ID, SERVICE_TYPE, FILE_NAME, UPLOADED_BY, RECORD_COUNT, DOCUMENT_TYPE) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionUtil.getConnection();
            stmt = conn.prepareStatement(query);

            stmt.setString(1, documentId);
            stmt.setString(2, serviceType.name());
            stmt.setString(3, fileName);
            stmt.setString(4, user);
            stmt.setInt(5, recordCount);
            stmt.setString(6, "Excel");

            stmt.executeUpdate();
            LOG.info("Saved file upload metadata for documentId: {}", documentId);

        } catch (SQLException e) {
            LOG.error("Error saving file upload metadata", e);
            throw new RuntimeException("Error saving file upload metadata", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private int processAndValidateRecords(List<String> referenceIds, ServiceType serviceType,
                                          String documentId) {
        int batchSize = Math.max(referenceIds.size() / 10, 100);
        int processedCount = 0;

        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement insertStmt = null;

        try {
            conn = ConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            String selectQuery = getSelectQuery(serviceType);
            String insertQuery = getInsertQuery();

            selectStmt = conn.prepareStatement(selectQuery);
            insertStmt = conn.prepareStatement(insertQuery);

            int batchCount = 0;

            for (String referenceId : referenceIds) {
                selectStmt.setString(1, referenceId);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        // Record found, extract data and prepare for insertion
                        setInsertParameters(insertStmt, rs, serviceType, documentId, referenceId);
                        insertStmt.addBatch();
                        processedCount++;
                    } else {
                        // Record not found, insert with minimal data
                        setInsertParametersForNotFound(insertStmt, documentId, referenceId);
                        insertStmt.addBatch();
                    }

                    batchCount++;

                    // Execute batch when reaching batch size
                    if (batchCount % batchSize == 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        batchCount = 0;
                    }
                }
            }

            // Execute remaining batch
            if (batchCount > 0) {
                insertStmt.executeBatch();
                conn.commit();
            }

        } catch (SQLException e) {
            LOG.error("Error processing and validating records", e);
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error during rollback", rollbackEx);
            }
            throw new RuntimeException("Error processing records", e);
        } finally {
            closeResources(conn, selectStmt, null);
            closeResources(null, insertStmt, null);
        }

        return processedCount;
    }

    private String getSelectQuery(ServiceType serviceType) {
        switch (serviceType) {
            case AIRTIME:
                return "SELECT TOPUP_REF_ID, ENTRYDATE, TXNAMT, NARRATION, ACCTNO, SOLID, " +
                        "CHANNELID, TSQ_RSP_CODE FROM ESBUSER.CHL_AIRTIMETOPUP_3 WHERE TOPUP_REF_ID = ?";

            case NIP_INFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL FROM ESBUSER.NIP_IN_FLW_V2 WHERE SESSIONID = ?";

            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL, FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1, " +
                        "FEECRNCY2, FEEAMT2, DRACCTNO2, CRACCTNO2, FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3, " +
                        "FEECRNCY4, FEEAMT4, DRACCTNO4, CRACCTNO4, FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5, " +
                        "ITS_RSP_CODE, DEBIT_RSP_CODE, REVERSAL_RSP_CODE, ITS_TSQ_RSP_CODE " +
                        "FROM ESBUSER.IBT_OUT_FLW WHERE SESSIONID = ?";

            case UP_INFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL FROM ESBUSER.IBT_IN_FLW WHERE SESSIONID = ?";

            default:
                throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
    }

    private String getInsertQuery() {
        return "INSERT INTO " + AppConstants.DbTables.STATUS_ENQUIRY_RECORD +
                " (DOCUMENT_ID, SESSION_ID, ORIGINATOR_ACCOUNT_NAME, ORIGINATOR_ACCOUNT_NUMBER, " +
                "NARRATION, AMOUNT, ACCT_SOL, FEE_CRNCY_1, FEE_AMT_1, DR_ACCT_NO_1, CR_ACCT_NO_1, " +
                "FEE_CRNCY_2, FEE_AMT_2, DR_ACCT_NO_2, CR_ACCT_NO_2, FEE_CRNCY_3, FEE_AMT_3, " +
                "DR_ACCT_NO_3, CR_ACCT_NO_3, FEE_CRNCY_4, FEE_AMT_4, DR_ACCT_NO_4, CR_ACCT_NO_4, " +
                "FEE_CRNCY_5, FEE_AMT_5, DR_ACCT_NO_5, CR_ACCT_NO_5, ITS_RSP_CODE, DEBIT_RSP_CODE, " +
                "REVERSAL_RSP_CODE, ITS_TSQ_RSP_CODE, TOPUP_REF_ID, ENTRYDATE, CHANNELID, TSQ_RSP_CODE, VALIDATION_STATUS) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private void setInsertParameters(PreparedStatement stmt, ResultSet rs, ServiceType serviceType,
                                     String documentId, String referenceId) throws SQLException {

        stmt.setString(1, documentId);
        stmt.setString(2, referenceId); // This will be SESSION_ID or TOPUP_REF_ID depending on service type

        if (serviceType == ServiceType.AIRTIME) {
            // For AIRTIME, map fields from CHL_AIRTIMETOPUP_3 table
            stmt.setString(3, ""); // ORIGINATOR_ACCOUNT_NAME (not available for AIRTIME)
            stmt.setString(4, rs.getString("ACCTNO")); // ORIGINATOR_ACCOUNT_NUMBER -> ACCTNO
            stmt.setString(5, rs.getString("NARRATION")); // NARRATION -> NARRATION
            stmt.setBigDecimal(6, rs.getBigDecimal("TXNAMT")); // AMOUNT -> TXNAMT
            stmt.setString(7, rs.getString("SOLID")); // ACCT_SOL -> SOLID

            // Set null values for fee fields (not applicable for AIRTIME)
            for (int i = 8; i <= 31; i++) {
                stmt.setString(i, "");
            }

            // Set AIRTIME-specific fields
            stmt.setString(32, rs.getString("TOPUP_REF_ID")); // TOPUP_REF_ID
            stmt.setDate(33, rs.getDate("ENTRYDATE")); // ENTRYDATE
            stmt.setString(34, rs.getString("CHANNELID")); // CHANNELID
            stmt.setString(35, rs.getString("TSQ_RSP_CODE")); // TSQ_RSP_CODE
            stmt.setString(36, "true"); // VALIDATION_STATUS

        } else {
            // For other service types (NIP/UP INFLOW/OUTFLOW)
            stmt.setString(3, rs.getString("ORIGINATORACCOUNTNAME"));
            stmt.setString(4, rs.getString("ORIGINATORACCOUNTNUMBER"));
            stmt.setString(5, rs.getString("NARRATION"));
            stmt.setBigDecimal(6, rs.getBigDecimal("AMOUNT"));
            stmt.setString(7, rs.getString("ACCT_SOL"));

            if (serviceType == ServiceType.NIP_OUTFLOW || serviceType == ServiceType.UP_OUTFLOW) {
                // Set fee and response code fields
                stmt.setString(8, rs.getString("FEECRNCY1"));
                stmt.setBigDecimal(9, rs.getBigDecimal("FEEAMT1"));
                stmt.setString(10, rs.getString("DRACCTNO1"));
                stmt.setString(11, rs.getString("CRACCTNO1"));
                stmt.setString(12, rs.getString("FEECRNCY2"));
                stmt.setBigDecimal(13, rs.getBigDecimal("FEEAMT2"));
                stmt.setString(14, rs.getString("DRACCTNO2"));
                stmt.setString(15, rs.getString("CRACCTNO2"));
                stmt.setString(16, rs.getString("FEECRNCY3"));
                stmt.setBigDecimal(17, rs.getBigDecimal("FEEAMT3"));
                stmt.setString(18, rs.getString("DRACCTNO3"));
                stmt.setString(19, rs.getString("CRACCTNO3"));
                stmt.setString(20, rs.getString("FEECRNCY4"));
                stmt.setBigDecimal(21, rs.getBigDecimal("FEEAMT4"));
                stmt.setString(22, rs.getString("DRACCTNO4"));
                stmt.setString(23, rs.getString("CRACCTNO4"));
                stmt.setString(24, rs.getString("FEECRNCY5"));
                stmt.setBigDecimal(25, rs.getBigDecimal("FEEAMT5"));
                stmt.setString(26, rs.getString("DRACCTNO5"));
                stmt.setString(27, rs.getString("CRACCTNO5"));
                stmt.setString(28, rs.getString("ITS_RSP_CODE"));
                stmt.setString(29, rs.getString("DEBIT_RSP_CODE"));
                stmt.setString(30, rs.getString("REVERSAL_RSP_CODE"));
                stmt.setString(31, rs.getString("ITS_TSQ_RSP_CODE"));
            } else {
                // Set null values for fee and response code fields for inflow types
                for (int i = 8; i <= 31; i++) {
                    stmt.setString(i, "");
                }
            }

            // Set null values for AIRTIME-specific fields
            stmt.setString(32, ""); // TOPUP_REF_ID
            stmt.setDate(33, null); // ENTRYDATE
            stmt.setString(34, ""); // CHANNELID
            stmt.setString(35, ""); // TSQ_RSP_CODE
            stmt.setString(36, "true"); // VALIDATION_STATUS
        }
    }

    private void setInsertParametersForNotFound(PreparedStatement stmt, String documentId,
                                                String referenceId) throws SQLException {
        stmt.setString(1, documentId);
        stmt.setString(2, referenceId);

        // Set null values for all other fields except validation status
        for (int i = 3; i <= 35; i++) {
            if (i == 33) {
                // ENTRYDATE is a DATE field, set to null explicitly
                stmt.setDate(i, null);
            } else {
                stmt.setString(i, "");
            }
        }

        stmt.setString(36, "false"); // VALIDATION_STATUS
    }

    private void updateValidationStatus(String documentId, String validationStatus) {
        String query = "UPDATE ESBUSER.SUPPORT_FILE_UPLOAD_MC_V2 SET VALIDATION_STATUS = ? WHERE DOCUMENT_ID = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ConnectionUtil.getConnection();
            stmt = conn.prepareStatement(query);

            stmt.setString(1, validationStatus);
            stmt.setString(2, documentId);

            stmt.executeUpdate();
            conn.commit();
            LOG.info("Updated validation status to {} for documentId: {}", validationStatus, documentId);

        } catch (SQLException e) {
            LOG.error("Error updating validation status", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public String getFileUploadRecords(int page, int size, String documentId,
                                       String serviceType, String creationDate) {
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder countQueryBuilder = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        // Build base query
        queryBuilder.append("SELECT * FROM (")
                .append("SELECT DOCUMENT_ID, SERVICE_TYPE, FILE_NAME, UPLOADED_BY, UPLOAD_DATE, ")
                .append("RECORD_COUNT, VALIDATION_STATUS, ")
                .append("ROW_NUMBER() OVER (ORDER BY UPLOAD_DATE DESC) AS row_num ")
                .append("FROM ESBUSER.SUPPORT_FILE_UPLOAD_MC_V2 WHERE 1=1");

        countQueryBuilder.append("SELECT COUNT(*) AS total_count FROM ESBUSER.SUPPORT_FILE_UPLOAD_MC_V2 WHERE 1=1");

        // Add filters
        if (documentId != null && !documentId.trim().isEmpty()) {
            queryBuilder.append(" AND DOCUMENT_ID = ?");
            countQueryBuilder.append(" AND DOCUMENT_ID = ?");
            parameters.add(documentId.trim());
        }

        if (serviceType != null && !serviceType.trim().isEmpty()) {
            queryBuilder.append(" AND SERVICE_TYPE = ?");
            countQueryBuilder.append(" AND SERVICE_TYPE = ?");
            parameters.add(serviceType.trim().toUpperCase());
        }

        if (creationDate != null && !creationDate.trim().isEmpty()) {
            queryBuilder.append(" AND TRUNC(UPLOAD_DATE) = TO_DATE(?, 'DD-MM-YYYY')");
            countQueryBuilder.append(" AND TRUNC(UPLOAD_DATE) = TO_DATE(?, 'DD-MM-YYYY')");
            parameters.add(creationDate.trim());
        }

        // Complete pagination query
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        queryBuilder.append(") WHERE row_num BETWEEN ? AND ?");

        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement countStmt = null;
        ResultSet rs = null;
        ResultSet countRs = null;

        try {
            conn = ConnectionUtil.getConnection();

            // Get total count
            countStmt = conn.prepareStatement(countQueryBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                countStmt.setObject(i + 1, parameters.get(i));
            }
            countRs = countStmt.executeQuery();

            int totalRows = 0;
            if (countRs.next()) {
                totalRows = countRs.getInt("total_count");
            }

            // Calculate total pages
            double totalPages = Math.ceil((double) totalRows / size);

            // Get paginated data
            stmt = conn.prepareStatement(queryBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            stmt.setInt(parameters.size() + 1, startRow);
            stmt.setInt(parameters.size() + 2, endRow);

            rs = stmt.executeQuery();

            // Build JSON response
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                    .append("\"status\":\"00\",")
                    .append("\"message\":\"Files fetched successfully\",")
                    .append("\"page\":\"").append(page).append("\",")
                    .append("\"size\":\"").append(size).append("\",")
                    .append("\"total_rows\":\"").append(totalRows).append("\",")
                    .append("\"total_pages\":").append(totalPages).append(",")
                    .append("\"data\":[");

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                // Format upload date to "DD-MM-YYYY HH:MM:SS"
                String uploadDate = "";
                if (rs.getTimestamp("UPLOAD_DATE") != null) {
                    uploadDate = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                            .format(rs.getTimestamp("UPLOAD_DATE"));
                }

                jsonBuilder.append("{")
                        .append("\"service_type\":\"").append(escapeJson(rs.getString("SERVICE_TYPE"))).append("\",")
                        .append("\"file_name\":\"").append(escapeJson(rs.getString("FILE_NAME"))).append("\",")
                        .append("\"uploaded_by\":\"").append(escapeJson(rs.getString("UPLOADED_BY"))).append("\",")
                        .append("\"document_id\":\"").append(escapeJson(rs.getString("DOCUMENT_ID"))).append("\",")
                        .append("\"uploaded_date\":\"").append(uploadDate).append("\",")
                        .append("\"record_count\":{")
                        .append("\"total\":\"").append(rs.getInt("RECORD_COUNT")).append("\"")
                        .append("},")
                        .append("\"validation_status\":\"").append(escapeJson(rs.getString("VALIDATION_STATUS"))).append("\"")
                        .append("}");
            }

            jsonBuilder.append("]}");

            return jsonBuilder.toString();

        } catch (SQLException e) {
            LOG.error("Error fetching file upload records", e);
            return "{\"status\":\"99\",\"message\":\"Error fetching records: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            LOG.error("Unexpected error fetching file upload records", e);
            return "{\"status\":\"99\",\"message\":\"Unexpected error occurred\"}";
        } finally {
            closeResources(null, countStmt, countRs);
            closeResources(conn, stmt, rs);
        }
    }

    public String getUserEmail(String username) {
        if (username == null || username.trim().isEmpty()) {
            LOG.warn("Username is null or empty, cannot fetch email");
            return null;
        }

        String query = "SELECT EMAIL FROM ESBUSER.BCK_USERS WHERE USERNAME = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = ConnectionUtil.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username.trim());

            rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("EMAIL");
                LOG.info("Email found for username: {}", username);
                return email;
            } else {
                LOG.warn("No email found for username: {}", username);
                return null;
            }

        } catch (SQLException e) {
            LOG.error("Error fetching email for username: " + username, e);
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.error("Error closing ResultSet", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOG.error("Error closing PreparedStatement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.error("Error closing Connection", e);
            }
        }
    }
}
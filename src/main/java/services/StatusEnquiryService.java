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
import java.util.Arrays;
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
                        "CHANNELID, TSQ_RSP_CODE, TOPUP_RSP_CODE, TOPUP_RSP_CODE_2, DEBIT_RSP_CODE, " +
                        "DEBIT_REVERSAL_RSP_CODE, DEBIT_REVERSAL_RSP_FLG " +
                        "FROM ESBUSER.CHL_AIRTIMETOPUP_3 WHERE TOPUP_REF_ID = ?";

            case NIP_INFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL, RESPONSECODE, TSQ_2_RSP_CODE, C24_RSP_CODE, C24_RSP_FLG, REQUESTDATE " +
                        "FROM ESBUSER.NIP_IN_FLW_V2 WHERE SESSIONID = ?";

            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL, FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1, " +
                        "FEECRNCY2, FEEAMT2, DRACCTNO2, CRACCTNO2, FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3, " +
                        "FEECRNCY4, FEEAMT4, DRACCTNO4, CRACCTNO4, FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5, " +
                        "ITS_RSP_CODE, DEBIT_RSP_CODE, REVERSAL_RSP_CODE, ITS_TSQ_RSP_CODE, ITS_RSP_FLG, ITS_TSQ_FLG, REVERSAL_FLG, REQUESTDATE " +
                        "FROM ESBUSER.IBT_OUT_FLW WHERE SESSIONID = ?";

            case UP_INFLOW:
                return "SELECT SESSIONID, ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, " +
                        "NARRATION, AMOUNT, ACCT_SOL, RESPONSECODE, TSQ_2_RSP_CODE, C24_RSP_CODE, REQUESTDATE " +
                        "FROM ESBUSER.IBT_IN_FLW WHERE SESSIONID = ?";

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
                "REVERSAL_RSP_CODE, ITS_TSQ_RSP_CODE, TOPUP_REF_ID, ENTRYDATE, CHANNELID, TSQ_RSP_CODE, " +
                "STATUS, REVERSAL_STATUS, ACCOUNT_DEBIT_STATUS, VALIDATION_STATUS) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private String evaluateTransactionStatus(ResultSet rs, ServiceType serviceType) throws SQLException {
        switch (serviceType) {
            case NIP_INFLOW:
                return evaluateNipInflowStatus(rs);
            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return evaluateOutflowStatus(rs);
            case UP_INFLOW:
                return evaluateUpInflowStatus(rs);
            case AIRTIME:
                return evaluateAirtimeStatus(rs);
            default:
                return "unknown";
        }
    }

    private String evaluateNipInflowStatus(ResultSet rs) throws SQLException {
        String responseCode = rs.getString("RESPONSECODE");
        String tsq2RspCode = rs.getString("TSQ_2_RSP_CODE");
        String c24RspCode = rs.getString("C24_RSP_CODE");
        String c24RspFlg = rs.getString("C24_RSP_FLG");

        // Success: responsecode = '00' AND tsq_2_rsp_code = '00' and C24_RSP_CODE IN ( '000','913')
        if ("00".equals(responseCode) && "00".equals(tsq2RspCode) &&
                (c24RspCode != null && ("000".equals(c24RspCode) || "913".equals(c24RspCode)))) {
            return "success";
        }

        // Pending: responsecode = '00' AND tsq_2_rsp_code = '00' AND (C24_RSP_CODE NOT IN ('000','913') OR C24_RSP_FLG = 'N')
        if ("00".equals(responseCode) && "00".equals(tsq2RspCode) &&
                ((c24RspCode != null && !("000".equals(c24RspCode) || "913".equals(c24RspCode))) || "N".equals(c24RspFlg))) {
            return "pending";
        }

        // Failed: responsecode = '00' AND ((tsq_2_rsp_code <> '00') OR (C24_RSP_CODE NOT IN ('000','913')))
        if ("00".equals(responseCode) &&
                (!"00".equals(tsq2RspCode) || (c24RspCode != null && !("000".equals(c24RspCode) || "913".equals(c24RspCode))))) {
            return "failed";
        }

        return "unknown";
    }

    private String evaluateOutflowStatus(ResultSet rs) throws SQLException {
        String itsRspCode = rs.getString("ITS_RSP_CODE");
        String itsTsqRspCode = rs.getString("ITS_TSQ_RSP_CODE");
        String itsRspFlg = rs.getString("ITS_RSP_FLG");
        String itsTsqFlg = rs.getString("ITS_TSQ_FLG");

        // Success: (its_rsp_code = '00' OR its_tsq_rsp_code = '00')
        if ("00".equals(itsRspCode) || "00".equals(itsTsqRspCode)) {
            return "success";
        }

        // Pending: (its_rsp_code IN ('09', '99', '25', '26', '94', '01') OR its_rsp_flg = 'N') AND (its_tsq_rsp_code IN ('09', '99', '25', '94', '01') OR its_tsq_flg = 'N')
        boolean rspPending = (itsRspCode != null && Arrays.asList("09", "99", "25", "26", "94", "01").contains(itsRspCode)) || "N".equals(itsRspFlg);
        boolean tsqPending = (itsTsqRspCode != null && Arrays.asList("09", "99", "25", "94", "01").contains(itsTsqRspCode)) || "N".equals(itsTsqFlg);
        if (rspPending && tsqPending) {
            return "pending";
        }

        // Failed: (its_rsp_code NOT IN ('00','25') OR its_tsq_rsp_code NOT IN ('00', '25') AND its_tsq_flg = 'N')
        boolean rspFailed = itsRspCode == null || (!("00".equals(itsRspCode) || "25".equals(itsRspCode)));
        boolean tsqFailed = (itsTsqRspCode == null || (!("00".equals(itsTsqRspCode) || "25".equals(itsTsqRspCode)))) && "N".equals(itsTsqFlg);
        if (rspFailed || tsqFailed) {
            return "failed";
        }

        return "unknown";
    }

    private String evaluateUpInflowStatus(ResultSet rs) throws SQLException {
        String responseCode = rs.getString("RESPONSECODE");
        String tsq2RspCode = rs.getString("TSQ_2_RSP_CODE");
        String c24RspCode = rs.getString("C24_RSP_CODE");

        // Success: (RESPONSECODE = '00' or TSQ_2_RSP_CODE = '00') and C24_RSP_CODE = '000'
        if (("00".equals(responseCode) || "00".equals(tsq2RspCode)) && "000".equals(c24RspCode)) {
            return "success";
        }

        // Pending: (RESPONSECODE = '05' or TSQ_2_RSP_CODE = '05')
        if ("05".equals(responseCode) || "05".equals(tsq2RspCode)) {
            return "pending";
        }

        // Failed: (RESPONSECODE = '03' or TSQ_2_RSP_CODE = '03')
        if ("03".equals(responseCode) || "03".equals(tsq2RspCode)) {
            return "failed";
        }

        return "unknown";
    }

    private String evaluateAirtimeStatus(ResultSet rs) throws SQLException {
        String topupRspCode = rs.getString("TOPUP_RSP_CODE");
        String topupRspCode2 = rs.getString("TOPUP_RSP_CODE_2");
        String tsqRspCode = rs.getString("TSQ_RSP_CODE");
        String debitRspCode = rs.getString("DEBIT_RSP_CODE");

        // Success: (topup_rsp_code = 'SUC' or topup_rsp_code_2 = '00' or tsq_rsp_code = 'SUC') and DEBIT_RSP_CODE in ('000')
        if (("SUC".equals(topupRspCode) || "00".equals(topupRspCode2) || "SUC".equals(tsqRspCode)) &&
                "000".equals(debitRspCode)) {
            return "success";
        }

        // Pending: (topup_rsp_code = 'UNKW' and tsq_rsp_code in ('UNKW', 'QER')) OR ((topup_rsp_code = 'UNKW' or topup_rsp_code is null) and tsq_rsp_code is null) and DEBIT_RSP_CODE in ('000')
        boolean pendingCondition1 = "UNKW".equals(topupRspCode) && (tsqRspCode != null && Arrays.asList("UNKW", "QER").contains(tsqRspCode));
        boolean pendingCondition2 = ("UNKW".equals(topupRspCode) || topupRspCode == null) && tsqRspCode == null;
        if ((pendingCondition1 || pendingCondition2) && "000".equals(debitRspCode)) {
            return "pending";
        }

        // Failed: (topup_rsp_code <> 'SUC' or topup_rsp_code_2 <> '00' or tsq_rsp_code <> 'SUC') and DEBIT_RSP_CODE in ('000')
        if ((topupRspCode != null && !"SUC".equals(topupRspCode)) ||
                (topupRspCode2 != null && !"00".equals(topupRspCode2)) ||
                (tsqRspCode != null && !"SUC".equals(tsqRspCode)) && "000".equals(debitRspCode)) {
            return "failed";
        }

        return "unknown";
    }

    private void setInsertParameters(PreparedStatement stmt, ResultSet rs, ServiceType serviceType,
                                     String documentId, String referenceId) throws SQLException {

        stmt.setString(1, documentId);
        stmt.setString(2, referenceId); // This will be SESSION_ID or TOPUP_REF_ID depending on service type

        // Evaluate transaction status
        String transactionStatus = evaluateTransactionStatus(rs, serviceType);
        String reversalStatus = evaluateReversalStatus(rs, serviceType);
        String accountDebitStatus = evaluateAccountDebitStatus(rs, serviceType);

        if (serviceType == ServiceType.AIRTIME) {
            // For AIRTIME, map fields from CHL_AIRTIMETOPUP_3 table
            stmt.setString(3, ""); // ORIGINATOR_ACCOUNT_NAME (not available for AIRTIME)
            stmt.setString(4, rs.getString("ACCTNO"));
            stmt.setString(5, rs.getString("NARRATION"));
            stmt.setBigDecimal(6, rs.getBigDecimal("TXNAMT"));
            stmt.setString(7, rs.getString("SOLID"));

            // Set null values for fee fields (not applicable for AIRTIME)
            for (int i = 8; i <= 31; i++) {
                stmt.setString(i, "");
            }

            // Set AIRTIME-specific fields
            stmt.setString(32, rs.getString("TOPUP_REF_ID"));
            stmt.setDate(33, rs.getDate("ENTRYDATE")); // ENTRYDATE for AIRTIME
            stmt.setString(34, rs.getString("CHANNELID"));
            stmt.setString(35, rs.getString("TSQ_RSP_CODE"));
            stmt.setString(36, transactionStatus); // STATUS
            stmt.setString(37, reversalStatus); // REVERSAL_STATUS
            stmt.setString(38, accountDebitStatus); // ACCOUNT_DEBIT_STATUS
            stmt.setString(39, "true"); // VALIDATION_STATUS

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
            stmt.setDate(33, rs.getDate("REQUESTDATE")); // REQUESTDATE for NIP/UP services
            stmt.setString(34, ""); // CHANNELID
            stmt.setString(35, ""); // TSQ_RSP_CODE
            stmt.setString(36, transactionStatus); // STATUS
            stmt.setString(37, reversalStatus); // REVERSAL_STATUS
            stmt.setString(38, accountDebitStatus); // ACCOUNT_DEBIT_STATUS
            stmt.setString(39, "true"); // VALIDATION_STATUS
        }
    }

    private void setInsertParametersForNotFound(PreparedStatement stmt, String documentId,
                                                String referenceId) throws SQLException {
        stmt.setString(1, documentId);
        stmt.setString(2, referenceId);

        // Set null values for all other fields except status fields and validation status
        for (int i = 3; i <= 35; i++) {
            if (i == 33) {
                // ENTRYDATE is a DATE field, set to null explicitly
                stmt.setDate(i, null);
            } else {
                stmt.setString(i, "");
            }
        }

        stmt.setString(36, "unknown"); // STATUS - unknown for not found records
        stmt.setString(37, ""); // REVERSAL_STATUS - empty for not found records
        stmt.setString(38, ""); // ACCOUNT_DEBIT_STATUS - empty for not found records
        stmt.setString(39, "false"); // VALIDATION_STATUS
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

    private String evaluateAccountDebitStatus(ResultSet rs, ServiceType serviceType) throws SQLException {
        switch (serviceType) {
            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                String debitRspCode = rs.getString("DEBIT_RSP_CODE");
                // Success: DEBIT_RSP_CODE in ('000')
                if ("000".equals(debitRspCode)) {
                    return "success";
                }
                // Failed: DEBIT_RSP_CODE not in ('000', '913')
                if (debitRspCode != null && !("000".equals(debitRspCode) || "913".equals(debitRspCode))) {
                    return "failed";
                }
                return "failed";

            case AIRTIME:
                String airtimeDebitRspCode = rs.getString("DEBIT_RSP_CODE");
                // Success: DEBIT_RSP_CODE in ('000', '911')
                if (airtimeDebitRspCode != null && ("000".equals(airtimeDebitRspCode) || "911".equals(airtimeDebitRspCode))) {
                    return "success";
                }
                // Failed: DEBIT_RSP_CODE NOT IN ('000', '911')
                if (airtimeDebitRspCode != null && !("000".equals(airtimeDebitRspCode) || "911".equals(airtimeDebitRspCode))) {
                    return "failed";
                }
                return "failed";

            case NIP_INFLOW:
            case UP_INFLOW:
            default:
                return ""; // Empty string for service types that don't have these checks
        }
    }

    private String evaluateReversalStatus(ResultSet rs, ServiceType serviceType) throws SQLException {
        switch (serviceType) {
            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                String reversalRspCode = rs.getString("REVERSAL_RSP_CODE");
                String reversalFlg = rs.getString("REVERSAL_FLG");

                // Success: REVERSAL_RSP_CODE IN ('000','913') and REVERSAL_FLG='Y'
                if ((reversalRspCode != null && ("000".equals(reversalRspCode) || "913".equals(reversalRspCode))) && "Y".equals(reversalFlg)) {
                    return "success";
                }
                // Failed: REVERSAL_RSP_CODE NOT IN ('000','913') and REVERSAL_FLG='Y'
                if ((reversalRspCode != null && !("000".equals(reversalRspCode) || "913".equals(reversalRspCode))) && "Y".equals(reversalFlg)) {
                    return "failed";
                }
                return "failed";

            case AIRTIME:
                String debitReversalRspCode = rs.getString("DEBIT_REVERSAL_RSP_CODE");
                String debitReversalRspFlg = rs.getString("DEBIT_REVERSAL_RSP_FLG");

                // Success: DEBIT_REVERSAL_RSP_CODE = '000' AND DEBIT_REVERSAL_RSP_FLG='Y'
                if ("000".equals(debitReversalRspCode) && "Y".equals(debitReversalRspFlg)) {
                    return "success";
                }
                // Failed: DEBIT_REVERSAL_RSP_CODE <> '000' AND DEBIT_REVERSAL_RSP_FLG='Y'
                if ((debitReversalRspCode != null && !"000".equals(debitReversalRspCode)) && "Y".equals(debitReversalRspFlg)) {
                    return "failed";
                }
                return "failed";

            case NIP_INFLOW:
            case UP_INFLOW:
            default:
                return ""; // Empty string for service types that don't have these checks
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

        String query = "SELECT EMAIL FROM ESBUSER.BCK_USERS WHERE USERNAME = ? AND APP_CODE = 'SUPPORT'";

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
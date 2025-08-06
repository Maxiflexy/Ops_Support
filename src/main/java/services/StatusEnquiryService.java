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

import static constants.AppConstants.DbTables.*;

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
            String insertQuery = getInsertQuery(serviceType);

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
                        setInsertParametersForNotFound(insertStmt, serviceType, documentId, referenceId);
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
                return "SELECT MSISDN, ACCTNO, ENTRYDATE, CHANNELID, BILLERID, BILLERNAME, COUNTRYCODE, " +
                        "ATTRIBUTES, SOLID, TXNAMT, ERRORFLAG, TOPUP_RSP_CODE, TOPUP_RSP_DATE, TOPUP_RSP_FLG, " +
                        "DEBIT_RSP_CODE, DEBIT_RSP_DATE, DEBIT_RSP_FLG, TOPUP_REF_ID, TOPUP_RSP_TYPE, VENDOR_REF, " +
                        "TRNPT_RSP_CODE, OPERATOR_REF, TOPUP_RSP_DESC, SERVICE_PROVIDER, TELCO, TXNCRNCY, PRODUCTID, " +
                        "TOPUP_RSP_CODE_2, TRANID, SOURCEOFFUND, RCHG_TYPE, TSQ_RSP_CODE_2, TSQ_RSP_CODE, " +
                        "TSQ_RSP_DATE, TSQ_RSP_FLG, TSQ_RSP_TYPE, TSQ_RSP_DESC, TSQ_TRSPT_CODE, " +
                        "DEBIT_REVERSAL_RSP_CODE, DEBIT_REVERSAL_RSP_DATE, DEBIT_REVERSAL_RSP_FLG, " +
                        "DEBIT_REVERSAL_TRIAL_COUNT, NARRATION " +
                        "FROM ESBUSER.CHL_AIRTIMETOPUP_3 WHERE TOPUP_REF_ID = ?";

            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return "SELECT TRANID, CLIENTNAME, TRANTYPE, REQUESTDATE, RESPONSEDATE, SESSIONID, " +
                        "DESTINATIONINSTITUTIONCODE, BENEFICIARYACCOUNTNAME, BENEFICIARYACCOUNTNUMBER, " +
                        "ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, NARRATION, PAYMENTREFERENCE, AMOUNT, " +
                        "ACCT_SOL, FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1, FEECRNCY2, FEEAMT2, DRACCTNO2, " +
                        "CRACCTNO2, FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3, FEECRNCY4, FEEAMT4, DRACCTNO4, " +
                        "CRACCTNO4, FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5, ITS_RSP_CODE, ITS_RSP_DATE, " +
                        "ITS_RSP_FLG, DEBIT_RSP_CODE, DEBIT_RSP_DATE, DEBIT_RSP_FLG, CRNCY_CODE, COUNTRY_CODE, " +
                        "REVERSAL_FLG, REVERSAL_RSP_DATE, REVERSAL_RSP_CODE, TSQ_RETRIAL_ID, ITS_TSQ_FLG, " +
                        "ITS_TSQ_DATE, ITS_TSQ_RSP_CODE, ITS_TSQ_COUNT, REVERSAL_NUM_TRIAL, DEBIT_NUM_TRIAL, " +
                        "TSQ_2_FLG, TSQ_2_DATE, TSQ_2_RSP_CODE, TSQ_2_COUNT, SENT, FIN_EXP_SENT, FALLBACK_FLG, " +
                        "REVERSAL_FALLBACK_FLG, REVERSAL_FALLBACK_DATE, TSQ_FALLBACK_FLG, TSQ_FALLBACK_DATE, " +
                        "TSQ_2_FALLBACK_FLG, TSQ_2_FALLBACK_DATE, TXN_RETRIAL_COUNT, FSP_TSQ_RSP_CODE " +
                        "FROM ESBUSER.IBT_OUT_FLW WHERE SESSIONID = ?";

            case NIP_INFLOW:
                return "SELECT CLIENTNAME, TRANTYPE, REQUESTDATE, RESPONSEDATE, SESSIONID, " +
                        "DESTINATIONINSTITUTIONCODE, CHANNELCODE, RESPONSECODE, NAMEENQUIRYREF, " +
                        "BENEFICIARYACCOUNTNAME, BENEFICIARYACCOUNTNUMBER, ORIGINATORACCOUNTNAME, " +
                        "ORIGINATORACCOUNTNUMBER, ORIGINATORKYCLEVEL, NARRATION, PAYMENTREFERENCE, AMOUNT, " +
                        "DEBITACCOUNTNAME, DEBITACCOUNTNUMBER, DEBITKYCLEVEL, BENEFICIARYKYCLEVEL, ACCT_SOL, " +
                        "SUBCODE, WS_FINISHED_FLG, TSQ_2_FLG, TSQ_2_DATE, TSQ_2_RSP_CODE, TSQ_2_COUNT, " +
                        "C24_RSP_CODE, C24_RSP_DATE, C24_RSP_FLG, C24_NUM_TRIAL " +
                        "FROM ESBUSER.NIP_IN_FLW_V2 WHERE SESSIONID = ?";

            case UP_INFLOW:
                return "SELECT CLIENTNAME, TRANTYPE, REQUESTDATE, RESPONSEDATE, SESSIONID, " +
                        "DESTINATIONINSTITUTIONCODE, CHANNELCODE, RESPONSECODE, NAMEENQUIRYREF, " +
                        "BENEFICIARYACCOUNTNAME, BENEFICIARYACCOUNTNUMBER, ORIGINATORACCOUNTNAME, " +
                        "ORIGINATORACCOUNTNUMBER, ORIGINATORKYCLEVEL, NARRATION, PAYMENTREFERENCE, AMOUNT, " +
                        "BENEFICIARYKYCLEVEL, ACCT_SOL, SUBCODE, WS_FINISHED_FLG, TSQ_2_FLG, TSQ_2_DATE, " +
                        "TSQ_2_RSP_CODE, TSQ_2_COUNT, C24_RSP_CODE, C24_RSP_DATE, C24_RSP_FLG, C24_NUM_TRIAL " +
                        "FROM ESBUSER.IBT_IN_FLW WHERE SESSIONID = ?";

            default:
                throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
    }

    private String getInsertQuery(ServiceType serviceType) {
        switch (serviceType) {
            case AIRTIME:
                return "INSERT INTO " + STATUS_ENQUIRY_RECORD_AIRTIME +
                        " (DOCUMENT_ID, TOPUP_REF_ID, MSISDN, ACCTNO, ENTRYDATE, CHANNELID, BILLERID, " +
                        "BILLERNAME, COUNTRYCODE, ATTRIBUTES, SOLID, TXNAMT, ERRORFLAG, TOPUP_RSP_CODE, " +
                        "TOPUP_RSP_DATE, TOPUP_RSP_FLG, DEBIT_RSP_CODE, DEBIT_RSP_DATE, DEBIT_RSP_FLG, " +
                        "TOPUP_RSP_TYPE, VENDOR_REF, TRNPT_RSP_CODE, OPERATOR_REF, TOPUP_RSP_DESC, " +
                        "SERVICE_PROVIDER, TELCO, TXNCRNCY, PRODUCTID, TOPUP_RSP_CODE_2, TRANID, " +
                        "SOURCEOFFUND, RCHG_TYPE, TSQ_RSP_CODE_2, TSQ_RSP_CODE, TSQ_RSP_DATE, TSQ_RSP_FLG, " +
                        "TSQ_RSP_TYPE, TSQ_RSP_DESC, TSQ_TRSPT_CODE, DEBIT_REVERSAL_RSP_CODE, " +
                        "DEBIT_REVERSAL_RSP_DATE, DEBIT_REVERSAL_RSP_FLG, DEBIT_REVERSAL_TRIAL_COUNT, " +
                        "NARRATION, TRANSACTION_STATUS, REVERSAL_STATUS, ACCOUNT_DEBIT_STATUS, VALIDATION_STATUS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return "INSERT INTO " + STATUS_ENQUIRY_RECORD_OUTFLOW +
                        " (DOCUMENT_ID, SESSIONID, TRANID, CLIENTNAME, TRANTYPE, REQUESTDATE, RESPONSEDATE, " +
                        "DESTINATIONINSTITUTIONCODE, BENEFICIARYACCOUNTNAME, BENEFICIARYACCOUNTNUMBER, " +
                        "ORIGINATORACCOUNTNAME, ORIGINATORACCOUNTNUMBER, NARRATION, PAYMENTREFERENCE, AMOUNT, " +
                        "ACCT_SOL, FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1, FEECRNCY2, FEEAMT2, DRACCTNO2, " +
                        "CRACCTNO2, FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3, FEECRNCY4, FEEAMT4, DRACCTNO4, " +
                        "CRACCTNO4, FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5, ITS_RSP_CODE, ITS_RSP_DATE, " +
                        "ITS_RSP_FLG, DEBIT_RSP_CODE, DEBIT_RSP_DATE, DEBIT_RSP_FLG, CRNCY_CODE, COUNTRY_CODE, " +
                        "REVERSAL_FLG, REVERSAL_RSP_DATE, REVERSAL_RSP_CODE, TSQ_RETRIAL_ID, ITS_TSQ_FLG, " +
                        "ITS_TSQ_DATE, ITS_TSQ_RSP_CODE, ITS_TSQ_COUNT, REVERSAL_NUM_TRIAL, DEBIT_NUM_TRIAL, " +
                        "TSQ_2_FLG, TSQ_2_DATE, TSQ_2_RSP_CODE, TSQ_2_COUNT, SENT, FIN_EXP_SENT, FALLBACK_FLG, " +
                        "REVERSAL_FALLBACK_FLG, REVERSAL_FALLBACK_DATE, TSQ_FALLBACK_FLG, TSQ_FALLBACK_DATE, " +
                        "TSQ_2_FALLBACK_FLG, TSQ_2_FALLBACK_DATE, TXN_RETRIAL_COUNT, FSP_TSQ_RSP_CODE, " +
                        "TRANSACTION_STATUS, REVERSAL_STATUS, ACCOUNT_DEBIT_STATUS, VALIDATION_STATUS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            case NIP_INFLOW:
            case UP_INFLOW:
                return "INSERT INTO " + STATUS_ENQUIRY_RECORD_INFLOW +
                        " (DOCUMENT_ID, SESSIONID, CLIENTNAME, TRANTYPE, REQUESTDATE, RESPONSEDATE, " +
                        "DESTINATIONINSTITUTIONCODE, CHANNELCODE, RESPONSECODE, NAMEENQUIRYREF, " +
                        "BENEFICIARYACCOUNTNAME, BENEFICIARYACCOUNTNUMBER, ORIGINATORACCOUNTNAME, " +
                        "ORIGINATORACCOUNTNUMBER, ORIGINATORKYCLEVEL, NARRATION, PAYMENTREFERENCE, AMOUNT, " +
                        "DEBITACCOUNTNAME, DEBITACCOUNTNUMBER, DEBITKYCLEVEL, BENEFICIARYKYCLEVEL, ACCT_SOL, " +
                        "SUBCODE, WS_FINISHED_FLG, TSQ_2_FLG, TSQ_2_DATE, TSQ_2_RSP_CODE, TSQ_2_COUNT, " +
                        "C24_RSP_CODE, C24_RSP_DATE, C24_RSP_FLG, C24_NUM_TRIAL, TRANSACTION_STATUS, VALIDATION_STATUS) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            default:
                throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
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

        if (serviceType == ServiceType.AIRTIME) {
            // Map AIRTIME specific fields
            stmt.setString(3, rs.getString("MSISDN"));
            stmt.setString(4, rs.getString("ACCTNO"));
            stmt.setDate(5, rs.getDate("ENTRYDATE"));
            stmt.setString(6, rs.getString("CHANNELID"));
            stmt.setString(7, rs.getString("BILLERID"));
            stmt.setString(8, rs.getString("BILLERNAME"));
            stmt.setString(9, rs.getString("COUNTRYCODE"));
            stmt.setString(10, rs.getString("ATTRIBUTES"));
            stmt.setString(11, rs.getString("SOLID"));
            stmt.setBigDecimal(12, rs.getBigDecimal("TXNAMT"));
            stmt.setString(13, rs.getString("ERRORFLAG"));
            stmt.setString(14, rs.getString("TOPUP_RSP_CODE"));
            stmt.setDate(15, rs.getDate("TOPUP_RSP_DATE"));
            stmt.setString(16, rs.getString("TOPUP_RSP_FLG"));
            stmt.setString(17, rs.getString("DEBIT_RSP_CODE"));
            stmt.setDate(18, rs.getDate("DEBIT_RSP_DATE"));
            stmt.setString(19, rs.getString("DEBIT_RSP_FLG"));
            stmt.setString(20, rs.getString("TOPUP_RSP_TYPE"));
            stmt.setString(21, rs.getString("VENDOR_REF"));
            stmt.setString(22, rs.getString("TRNPT_RSP_CODE"));
            stmt.setString(23, rs.getString("OPERATOR_REF"));
            stmt.setString(24, rs.getString("TOPUP_RSP_DESC"));
            stmt.setString(25, rs.getString("SERVICE_PROVIDER"));
            stmt.setString(26, rs.getString("TELCO"));
            stmt.setString(27, rs.getString("TXNCRNCY"));
            stmt.setString(28, rs.getString("PRODUCTID"));
            stmt.setString(29, rs.getString("TOPUP_RSP_CODE_2"));
            stmt.setString(30, rs.getString("TRANID"));
            stmt.setString(31, rs.getString("SOURCEOFFUND"));
            stmt.setString(32, rs.getString("RCHG_TYPE"));
            stmt.setString(33, rs.getString("TSQ_RSP_CODE_2"));
            stmt.setString(34, rs.getString("TSQ_RSP_CODE"));
            stmt.setDate(35, rs.getDate("TSQ_RSP_DATE"));
            stmt.setString(36, rs.getString("TSQ_RSP_FLG"));
            stmt.setString(37, rs.getString("TSQ_RSP_TYPE"));
            stmt.setString(38, rs.getString("TSQ_RSP_DESC"));
            stmt.setString(39, rs.getString("TSQ_TRSPT_CODE"));
            stmt.setString(40, rs.getString("DEBIT_REVERSAL_RSP_CODE"));
            stmt.setDate(41, rs.getDate("DEBIT_REVERSAL_RSP_DATE"));
            stmt.setString(42, rs.getString("DEBIT_REVERSAL_RSP_FLG"));

            // Handle potential null for DEBIT_REVERSAL_TRIAL_COUNT
            Object trialCount = rs.getObject("DEBIT_REVERSAL_TRIAL_COUNT");
            if (trialCount != null) {
                stmt.setInt(43, ((Number) trialCount).intValue());
            } else {
                stmt.setNull(43, java.sql.Types.INTEGER);
            }

            stmt.setString(44, rs.getString("NARRATION"));
            stmt.setString(45, transactionStatus);
            stmt.setString(46, evaluateReversalStatus(rs, serviceType));
            stmt.setString(47, evaluateAccountDebitStatus(rs, serviceType));
            stmt.setString(48, "true");

        } else if (serviceType == ServiceType.NIP_OUTFLOW || serviceType == ServiceType.UP_OUTFLOW) {
            // Map OUTFLOW specific fields

            // Handle potential null for TRANID
            Object tranId = rs.getObject("TRANID");
            if (tranId != null) {
                stmt.setLong(3, ((Number) tranId).longValue());
            } else {
                stmt.setNull(3, java.sql.Types.NUMERIC);
            }

            stmt.setString(4, rs.getString("CLIENTNAME"));
            stmt.setString(5, rs.getString("TRANTYPE"));
            stmt.setDate(6, rs.getDate("REQUESTDATE"));
            stmt.setDate(7, rs.getDate("RESPONSEDATE"));
            stmt.setString(8, rs.getString("DESTINATIONINSTITUTIONCODE"));
            stmt.setString(9, rs.getString("BENEFICIARYACCOUNTNAME"));
            stmt.setString(10, rs.getString("BENEFICIARYACCOUNTNUMBER"));
            stmt.setString(11, rs.getString("ORIGINATORACCOUNTNAME"));
            stmt.setString(12, rs.getString("ORIGINATORACCOUNTNUMBER"));
            stmt.setString(13, rs.getString("NARRATION"));
            stmt.setString(14, rs.getString("PAYMENTREFERENCE"));
            stmt.setBigDecimal(15, rs.getBigDecimal("AMOUNT"));
            stmt.setString(16, rs.getString("ACCT_SOL"));

            // Fee fields
            stmt.setString(17, rs.getString("FEECRNCY1"));
            stmt.setBigDecimal(18, rs.getBigDecimal("FEEAMT1"));
            stmt.setString(19, rs.getString("DRACCTNO1"));
            stmt.setString(20, rs.getString("CRACCTNO1"));
            stmt.setString(21, rs.getString("FEECRNCY2"));
            stmt.setBigDecimal(22, rs.getBigDecimal("FEEAMT2"));
            stmt.setString(23, rs.getString("DRACCTNO2"));
            stmt.setString(24, rs.getString("CRACCTNO2"));
            stmt.setString(25, rs.getString("FEECRNCY3"));
            stmt.setBigDecimal(26, rs.getBigDecimal("FEEAMT3"));
            stmt.setString(27, rs.getString("DRACCTNO3"));
            stmt.setString(28, rs.getString("CRACCTNO3"));
            stmt.setString(29, rs.getString("FEECRNCY4"));
            stmt.setBigDecimal(30, rs.getBigDecimal("FEEAMT4"));
            stmt.setString(31, rs.getString("DRACCTNO4"));
            stmt.setString(32, rs.getString("CRACCTNO4"));
            stmt.setString(33, rs.getString("FEECRNCY5"));
            stmt.setBigDecimal(34, rs.getBigDecimal("FEEAMT5"));
            stmt.setString(35, rs.getString("DRACCTNO5"));
            stmt.setString(36, rs.getString("CRACCTNO5"));

            // Response code fields
            stmt.setString(37, rs.getString("ITS_RSP_CODE"));
            stmt.setDate(38, rs.getDate("ITS_RSP_DATE"));
            stmt.setString(39, rs.getString("ITS_RSP_FLG"));
            stmt.setString(40, rs.getString("DEBIT_RSP_CODE"));
            stmt.setDate(41, rs.getDate("DEBIT_RSP_DATE"));
            stmt.setString(42, rs.getString("DEBIT_RSP_FLG"));
            stmt.setString(43, rs.getString("CRNCY_CODE"));
            stmt.setString(44, rs.getString("COUNTRY_CODE"));
            stmt.setString(45, rs.getString("REVERSAL_FLG"));
            stmt.setDate(46, rs.getDate("REVERSAL_RSP_DATE"));
            stmt.setString(47, rs.getString("REVERSAL_RSP_CODE"));
            stmt.setString(48, rs.getString("TSQ_RETRIAL_ID"));
            stmt.setString(49, rs.getString("ITS_TSQ_FLG"));
            stmt.setDate(50, rs.getDate("ITS_TSQ_DATE"));
            stmt.setString(51, rs.getString("ITS_TSQ_RSP_CODE"));

            // Handle numeric fields that might be null
            setIntegerOrNull(stmt, 52, rs, "ITS_TSQ_COUNT");
            setIntegerOrNull(stmt, 53, rs, "REVERSAL_NUM_TRIAL");
            setIntegerOrNull(stmt, 54, rs, "DEBIT_NUM_TRIAL");

            stmt.setString(55, rs.getString("TSQ_2_FLG"));
            stmt.setDate(56, rs.getDate("TSQ_2_DATE"));
            stmt.setString(57, rs.getString("TSQ_2_RSP_CODE"));
            setIntegerOrNull(stmt, 58, rs, "TSQ_2_COUNT");

            stmt.setString(59, rs.getString("SENT"));
            stmt.setString(60, rs.getString("FIN_EXP_SENT"));
            stmt.setString(61, rs.getString("FALLBACK_FLG"));
            stmt.setString(62, rs.getString("REVERSAL_FALLBACK_FLG"));
            stmt.setDate(63, rs.getDate("REVERSAL_FALLBACK_DATE"));
            stmt.setString(64, rs.getString("TSQ_FALLBACK_FLG"));
            stmt.setDate(65, rs.getDate("TSQ_FALLBACK_DATE"));
            stmt.setString(66, rs.getString("TSQ_2_FALLBACK_FLG"));
            stmt.setDate(67, rs.getDate("TSQ_2_FALLBACK_DATE"));
            setIntegerOrNull(stmt, 68, rs, "TXN_RETRIAL_COUNT");

            stmt.setString(69, rs.getString("FSP_TSQ_RSP_CODE"));
            stmt.setString(70, transactionStatus);
            stmt.setString(71, evaluateReversalStatus(rs, serviceType));
            stmt.setString(72, evaluateAccountDebitStatus(rs, serviceType));
            stmt.setString(73, "true");

        } else if (serviceType == ServiceType.NIP_INFLOW || serviceType == ServiceType.UP_INFLOW) {
            // Map INFLOW specific fields
            stmt.setString(3, rs.getString("CLIENTNAME"));
            stmt.setString(4, rs.getString("TRANTYPE"));
            stmt.setDate(5, rs.getDate("REQUESTDATE"));
            stmt.setDate(6, rs.getDate("RESPONSEDATE"));
            stmt.setString(7, rs.getString("DESTINATIONINSTITUTIONCODE"));
            stmt.setString(8, rs.getString("CHANNELCODE"));
            stmt.setString(9, rs.getString("RESPONSECODE"));
            stmt.setString(10, rs.getString("NAMEENQUIRYREF"));
            stmt.setString(11, rs.getString("BENEFICIARYACCOUNTNAME"));
            stmt.setString(12, rs.getString("BENEFICIARYACCOUNTNUMBER"));
            stmt.setString(13, rs.getString("ORIGINATORACCOUNTNAME"));
            stmt.setString(14, rs.getString("ORIGINATORACCOUNTNUMBER"));
            stmt.setString(15, rs.getString("ORIGINATORKYCLEVEL"));
            stmt.setString(16, rs.getString("NARRATION"));
            stmt.setString(17, rs.getString("PAYMENTREFERENCE"));
            stmt.setBigDecimal(18, rs.getBigDecimal("AMOUNT"));

            // Handle debit account fields - only available for NIP_INFLOW
            if (serviceType == ServiceType.NIP_INFLOW) {
                stmt.setString(19, rs.getString("DEBITACCOUNTNAME"));
                stmt.setString(20, rs.getString("DEBITACCOUNTNUMBER"));
                stmt.setString(21, rs.getString("DEBITKYCLEVEL"));
            } else {
                // UP_INFLOW - set empty strings for missing debit fields
                stmt.setString(19, "");
                stmt.setString(20, "");
                stmt.setString(21, "");
            }

            stmt.setString(22, rs.getString("BENEFICIARYKYCLEVEL"));
            stmt.setString(23, rs.getString("ACCT_SOL"));
            stmt.setString(24, rs.getString("SUBCODE"));
            stmt.setString(25, rs.getString("WS_FINISHED_FLG"));
            stmt.setString(26, rs.getString("TSQ_2_FLG"));
            stmt.setDate(27, rs.getDate("TSQ_2_DATE"));
            stmt.setString(28, rs.getString("TSQ_2_RSP_CODE"));
            setIntegerOrNull(stmt, 29, rs, "TSQ_2_COUNT");
            stmt.setString(30, rs.getString("C24_RSP_CODE"));
            stmt.setDate(31, rs.getDate("C24_RSP_DATE"));
            stmt.setString(32, rs.getString("C24_RSP_FLG"));
            setIntegerOrNull(stmt, 33, rs, "C24_NUM_TRIAL");
            stmt.setString(34, transactionStatus);
            stmt.setString(35, "true");
        }
    }

    private void setIntegerOrNull(PreparedStatement stmt, int paramIndex, ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value != null) {
            stmt.setInt(paramIndex, ((Number) value).intValue());
        } else {
            stmt.setNull(paramIndex, java.sql.Types.INTEGER);
        }
    }

    private void setInsertParametersForNotFound(PreparedStatement stmt, ServiceType serviceType,
                                                String documentId, String referenceId) throws SQLException {
        stmt.setString(1, documentId);
        stmt.setString(2, referenceId);

        if (serviceType == ServiceType.AIRTIME) {
            // Set null/empty values for all AIRTIME fields except status fields
            for (int i = 3; i <= 44; i++) {
                if (i == 5 || i == 15 || i == 18 || i == 35 || i == 41) { // DATE fields
                    stmt.setDate(i, null);
                } else if (i == 12) { // TXNAMT - numeric field
                    stmt.setNull(i, java.sql.Types.NUMERIC);
                } else if (i == 43) { // DEBIT_REVERSAL_TRIAL_COUNT - integer field
                    stmt.setNull(i, java.sql.Types.INTEGER);
                } else {
                    stmt.setString(i, "");
                }
            }
            stmt.setString(45, "unknown"); // TRANSACTION_STATUS
            stmt.setString(46, ""); // REVERSAL_STATUS
            stmt.setString(47, ""); // ACCOUNT_DEBIT_STATUS
            stmt.setString(48, "false"); // VALIDATION_STATUS

        } else if (serviceType == ServiceType.NIP_OUTFLOW || serviceType == ServiceType.UP_OUTFLOW) {
            // Set null/empty values for all OUTFLOW fields except status fields
            stmt.setNull(3, java.sql.Types.NUMERIC); // TRANID
            for (int i = 4; i <= 69; i++) {
                if (i == 6 || i == 7 || i == 38 || i == 41 || i == 46 || i == 50 || i == 56 || i == 63 || i == 65 || i == 67) { // DATE fields
                    stmt.setDate(i, null);
                } else if (i == 15 || i == 18 || i == 22 || i == 26 || i == 30 || i == 34) { // NUMERIC/DECIMAL fields
                    stmt.setNull(i, java.sql.Types.NUMERIC);
                } else if (i == 52 || i == 53 || i == 54 || i == 58 || i == 68) { // INTEGER fields
                    stmt.setNull(i, java.sql.Types.INTEGER);
                } else {
                    stmt.setString(i, "");
                }
            }
            stmt.setString(70, "unknown"); // TRANSACTION_STATUS
            stmt.setString(71, ""); // REVERSAL_STATUS
            stmt.setString(72, ""); // ACCOUNT_DEBIT_STATUS
            stmt.setString(73, "false"); // VALIDATION_STATUS

        } else if (serviceType == ServiceType.NIP_INFLOW || serviceType == ServiceType.UP_INFLOW) {
            // Set null/empty values for all INFLOW fields except status fields
            for (int i = 3; i <= 33; i++) {
                if (i == 5 || i == 6 || i == 27 || i == 31) { // DATE fields
                    stmt.setDate(i, null);
                } else if (i == 18) { // AMOUNT - numeric field
                    stmt.setNull(i, java.sql.Types.NUMERIC);
                } else if (i == 29 || i == 33) { // INTEGER fields
                    stmt.setNull(i, java.sql.Types.INTEGER);
                } else {
                    stmt.setString(i, "");
                }
            }
            stmt.setString(34, "unknown"); // TRANSACTION_STATUS
            stmt.setString(35, "false"); // VALIDATION_STATUS
        }
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
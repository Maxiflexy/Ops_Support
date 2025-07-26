package persistence;

import constants.AppConstants;
import constants.AppModules;
import constants.ModuleName;
import constants.ServiceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.SettlementReportRecord;

import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class TransactionService {

    final static Logger LOG = LogManager.getLogger(TransactionService.class);

    private TransactionService() {}

    private static final class InstanceHolder {
        private static final TransactionService instance = new TransactionService();
    }

    public static TransactionService getInstance() {
        return InstanceHolder.instance;
    }


    public int validateAndSaveTransactions(List<String> transactionReferences, ModuleName moduleName, ServiceType serviceType, String documentId) {
        int totalValidated = 0;
        int batchCount = 0;
        int batchSize = Math.max(transactionReferences.size() / 10, 100);

        Connection conn = null;
        PreparedStatement selectStatement = null;
        PreparedStatement insertStatement = null;
        ResultSet rs = null;

        try {
            conn = ConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            String serviceTable = getServiceTable(serviceType);
            String referenceColumn = getReferenceColumn(serviceType);
            String moduleTable = getModuleTable(moduleName);
            String query = getQuery(serviceType, serviceTable, referenceColumn);
            List<String> queryColumns = getQueryColumns(serviceType);

            String insert = null;

            String insertQueryForAirtimeService = "INSERT INTO " + moduleTable +
                    " (tran_ref, TRAN_DATE_TIME, tran_amt, narration, debit_acct_no, credit_acct_no, document_id, TRANID, SOL_ID, CLIENT_ID, APPL_CODE, TRAN_CRNCY, VALIDATION_STATUS) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


            String insertQueryNIP_UP_InflowService = "INSERT INTO " + moduleTable +
                    " (tran_ref, TRAN_DATE_TIME, tran_amt, narration, debit_acct_no, credit_acct_no, document_id, TRANID, SOL_ID, CLIENT_ID, APPL_CODE, TRAN_CRNCY, VALIDATION_STATUS," +
                    " FEE_CRNCY_1, FEE_AMT_1, FEE_DEBIT_ACCT_NO_1, FEE_CREDIT_ACCT_NO_1, FEE_CRNCY_2, FEE_AMT_2, FEE_DEBIT_ACCT_NO_2, FEE_CREDIT_ACCT_NO_2," +
                    " FEE_CRNCY_3, FEE_AMT_3, FEE_DEBIT_ACCT_NO_3, FEE_CREDIT_ACCT_NO_3, FEE_CRNCY_4, FEE_AMT_4, FEE_DEBIT_ACCT_NO_4, FEE_CREDIT_ACCT_NO_4," +
                    " FEE_CRNCY_5, FEE_AMT_5, FEE_DEBIT_ACCT_NO_5, FEE_CREDIT_ACCT_NO_5) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


            String insertQueryNIP_UP_OutflowService = "INSERT INTO " + moduleTable +
                    " (tran_ref, TRAN_DATE_TIME, tran_amt, narration, debit_acct_no, credit_acct_no, document_id, TRANID, SOL_ID, CLIENT_ID, APPL_CODE, TRAN_CRNCY, VALIDATION_STATUS," +
                    " FEE_CRNCY_1, FEE_AMT_1, FEE_DEBIT_ACCT_NO_1, FEE_CREDIT_ACCT_NO_1, FEE_CRNCY_2, FEE_AMT_2, FEE_DEBIT_ACCT_NO_2, FEE_CREDIT_ACCT_NO_2," +
                    " FEE_CRNCY_3, FEE_AMT_3, FEE_DEBIT_ACCT_NO_3, FEE_CREDIT_ACCT_NO_3, FEE_CRNCY_4, FEE_AMT_4, FEE_DEBIT_ACCT_NO_4, FEE_CREDIT_ACCT_NO_4," +
                    " FEE_CRNCY_5, FEE_AMT_5, FEE_DEBIT_ACCT_NO_5, FEE_CREDIT_ACCT_NO_5) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


            switch (serviceType){
                case AIRTIME:
                    insert = insertQueryForAirtimeService;
                    break;
                case NIP_INFLOW:
                case UP_INFLOW:
                    insert = insertQueryNIP_UP_InflowService;
                    break;
                case NIP_OUTFLOW:
                case UP_OUTFLOW:
                    insert = insertQueryNIP_UP_OutflowService;
                    break;
                default: break;
            }

            selectStatement = conn.prepareStatement(query);
            insertStatement = conn.prepareStatement(insert);

            LOG.info("Select query for the service table: {}", query);
            LOG.info("Insert query for the module table: {}", insert);

//            try (PreparedStatement selectStatement = conn.prepareStatement(query);
//                 PreparedStatement insertStatement = conn.prepareStatement(insert)) {
//                LOG.info("Select query for the service table: {}", query);
//                LOG.info("Insert query for the module table: {}", insert);

            for (String ref : transactionReferences) {
                selectStatement.setString(1, ref);
                //ResultSet rs = selectStatement.executeQuery();
                rs = selectStatement.executeQuery();

                // If a matching record is found, extract data from the result set
                if (rs.next()) {

                    String tranDate, tranAmt, tranNarration, credit_accountNo, debit_accountNo, topUpRef, solId, trandId, requestDate = "", clientId;
                    String FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1;
                    String FEECRNCY2, FEEAMT2, DRACCTNO2, CRACCTNO2;
                    String FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3;
                    String FEECRNCY4, FEEAMT4, DRACCTNO4, CRACCTNO4;
                    String FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5;

                    String contraAcctNo1 = System.getProperty("CONTRA_ACCOUNT_AIRTIME/DATA");
                    String contraAcctNo2 = System.getProperty("CONTRA_ACCOUNT_NIP_INFLOW");
                    String contraAcctNo3 = System.getProperty("CONTRA_ACCOUNT_UP_INFLOW");
                    String contraAcctNo4 = System.getProperty("CONTRA_ACCOUNT_NIP_OUTFLOW");
                    String contraAcctNo5 = System.getProperty("CONTRA_ACCOUNT_UP_OUTFLOW");

                    if(serviceType.equals(ServiceType.AIRTIME)){
                        tranDate = rs.getString(queryColumns.get(0));
                        tranAmt = rs.getString(queryColumns.get(1));
                        tranNarration = rs.getString(queryColumns.get(2));
                        debit_accountNo = rs.getString(queryColumns.get(3));
                        topUpRef = rs.getString(queryColumns.get(4));
                        if (topUpRef != null && topUpRef.length() > 12)
                            topUpRef = topUpRef.substring(topUpRef.length() - 12);

                        solId = rs.getString(queryColumns.get(5));
                        clientId = rs.getString(queryColumns.get(6));

                        if (contraAcctNo1 != null && solId != null) {
                            contraAcctNo1 = contraAcctNo1.replace("----", solId);
                        }

                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, tranDate);
                        insertStatement.setString(3, tranAmt);
                        insertStatement.setString(4, tranNarration);
                        insertStatement.setString(5, debit_accountNo);
                        insertStatement.setString(6, contraAcctNo1); // credit_acct_no
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, topUpRef);
                        insertStatement.setString(9, solId);
                        insertStatement.setString(10, clientId);
                        insertStatement.setString(11, "AIRTIME");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "Y");

                        insertStatement.addBatch();
                        totalValidated++;

                    } else if (serviceType.equals(ServiceType.NIP_INFLOW)) {
                        tranDate = rs.getString(queryColumns.get(0));
                        tranAmt = rs.getString(queryColumns.get(1));
                        tranNarration = rs.getString(queryColumns.get(2));
                        credit_accountNo = rs.getString(queryColumns.get(3));
                        trandId = rs.getString(queryColumns.get(4));
                        trandId = (trandId == null) ? " " : trandId;
                        solId = rs.getString(queryColumns.get(5));
                        solId = (solId == null) ? " " : solId;

                        if (contraAcctNo2 != null && !solId.equals(" ")) {
                            contraAcctNo2 = contraAcctNo2.replace("----", solId);
                        }

                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, tranDate);
                        insertStatement.setString(3, tranAmt);
                        insertStatement.setString(4, tranNarration);
                        insertStatement.setString(5, contraAcctNo2); // debit_acct_no
                        insertStatement.setString(6, credit_accountNo);
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, trandId);
                        insertStatement.setString(9, solId);
                        insertStatement.setString(10, "NIPINFLOW");
                        insertStatement.setString(11, "NIPINFLOW");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "Y");
                        insertStatement.setString(14, "NGN");
                        insertStatement.setString(15, "0");
                        insertStatement.setString(16, " ");
                        insertStatement.setString(17, " ");
                        insertStatement.setString(18, "NGN");
                        insertStatement.setString(19, "0");
                        insertStatement.setString(20, " ");
                        insertStatement.setString(21, " ");
                        insertStatement.setString(22, "NGN");
                        insertStatement.setString(23, "0");
                        insertStatement.setString(24, " ");
                        insertStatement.setString(25, " ");
                        insertStatement.setString(26, "NGN");
                        insertStatement.setString(27, "0");
                        insertStatement.setString(28, " ");
                        insertStatement.setString(29, " ");
                        insertStatement.setString(30, "NGN");
                        insertStatement.setString(31, "0");
                        insertStatement.setString(32, " ");
                        insertStatement.setString(33, " ");


                        insertStatement.addBatch();
                        totalValidated++;

                    } else if(serviceType.equals(ServiceType.UP_INFLOW)){
                        tranDate = rs.getString(queryColumns.get(0));
                        tranAmt = rs.getString(queryColumns.get(1));
                        tranNarration = rs.getString(queryColumns.get(2));
                        credit_accountNo = rs.getString(queryColumns.get(3));
                        trandId = rs.getString(queryColumns.get(4));
                        trandId = (trandId == null) ? " " : trandId;
                        solId = rs.getString(queryColumns.get(5));
                        solId = (solId == null) ? " " : solId;

                        if (contraAcctNo3 != null && !solId.equals(" ")) {
                            contraAcctNo3 = contraAcctNo3.replace("----", solId);
                        }

                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, tranDate);
                        insertStatement.setString(3, tranAmt);
                        insertStatement.setString(4, tranNarration);
                        insertStatement.setString(5, contraAcctNo3); // debit_acct_no
                        insertStatement.setString(6, credit_accountNo);
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, trandId);
                        insertStatement.setString(9, solId);
                        insertStatement.setString(10, "UPINFLOW");
                        insertStatement.setString(11, "UPINFLOW");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "Y");
                        insertStatement.setString(14, "NGN");
                        insertStatement.setString(15, "0");
                        insertStatement.setString(16, " ");
                        insertStatement.setString(17, " ");
                        insertStatement.setString(18, "NGN");
                        insertStatement.setString(19, "0");
                        insertStatement.setString(20, " ");
                        insertStatement.setString(21, " ");
                        insertStatement.setString(22, "NGN");
                        insertStatement.setString(23, "0");
                        insertStatement.setString(24, " ");
                        insertStatement.setString(25, " ");
                        insertStatement.setString(26, "NGN");
                        insertStatement.setString(27, "0");
                        insertStatement.setString(28, " ");
                        insertStatement.setString(29, " ");
                        insertStatement.setString(30, "NGN");
                        insertStatement.setString(31, "0");
                        insertStatement.setString(32, " ");
                        insertStatement.setString(33, " ");

                        insertStatement.addBatch();
                        totalValidated++;

                    } else if (serviceType.equals(ServiceType.NIP_OUTFLOW) || serviceType.equals(ServiceType.UP_OUTFLOW)) {

                        tranDate = rs.getString(queryColumns.get(0));
                        tranAmt = rs.getString(queryColumns.get(1));
                        tranNarration = rs.getString(queryColumns.get(2));
                        debit_accountNo = rs.getString(queryColumns.get(3));

                        FEECRNCY1 = rs.getString(queryColumns.get(4));
                        FEECRNCY1 = (FEECRNCY1 == null) ? "NGN" : FEECRNCY1;

                        FEEAMT1 = rs.getString(queryColumns.get(5));

                        DRACCTNO1 = rs.getString(queryColumns.get(6));
                        DRACCTNO1 = (DRACCTNO1 == null) ? " " : DRACCTNO1;

                        CRACCTNO1 = rs.getString(queryColumns.get(7));
                        CRACCTNO1 = (CRACCTNO1 == null) ? " " : CRACCTNO1;

                        FEECRNCY2 = rs.getString(queryColumns.get(8));
                        FEECRNCY2 = (FEECRNCY2 == null) ? "NGN" : FEECRNCY2;

                        FEEAMT2 = rs.getString(queryColumns.get(9));

                        DRACCTNO2 = rs.getString(queryColumns.get(10));
                        DRACCTNO2 = (DRACCTNO2 == null) ? " " : DRACCTNO2;

                        CRACCTNO2 = rs.getString(queryColumns.get(11));
                        CRACCTNO2 = (CRACCTNO2 == null) ? " " : CRACCTNO2;

                        FEECRNCY3 = rs.getString(queryColumns.get(12));
                        FEECRNCY3 = (FEECRNCY3 == null) ? "NGN" : FEECRNCY3;

                        FEEAMT3 = rs.getString(queryColumns.get(13));

                        DRACCTNO3 = rs.getString(queryColumns.get(14));
                        DRACCTNO3 = (DRACCTNO3 == null) ? " " : DRACCTNO3;

                        CRACCTNO3 = rs.getString(queryColumns.get(15));
                        CRACCTNO3 = (CRACCTNO3 == null) ? " " : CRACCTNO3;

                        FEECRNCY4 = rs.getString(queryColumns.get(16));
                        FEECRNCY4 = (FEECRNCY4 == null) ? "NGN" : FEECRNCY4;

                        FEEAMT4 = rs.getString(queryColumns.get(17));

                        DRACCTNO4 = rs.getString(queryColumns.get(18));
                        DRACCTNO4 = (DRACCTNO4 == null) ? " " : DRACCTNO4;

                        CRACCTNO4 = rs.getString(queryColumns.get(19));
                        CRACCTNO4 = (CRACCTNO4 == null) ? " " : CRACCTNO4;

                        FEECRNCY5 = rs.getString(queryColumns.get(20));
                        FEECRNCY5 = (FEECRNCY5 == null) ? "NGN" : FEECRNCY5;

                        FEEAMT5 = rs.getString(queryColumns.get(21));

                        DRACCTNO5 = rs.getString(queryColumns.get(22));
                        DRACCTNO5 = (DRACCTNO5 == null) ? " " : DRACCTNO5;

                        CRACCTNO5 = rs.getString(queryColumns.get(23));
                        CRACCTNO5 = (CRACCTNO5 == null) ? " " : CRACCTNO5;

                        trandId = rs.getString(queryColumns.get(24));
                        solId = rs.getString(queryColumns.get(25));
                        clientId = rs.getString(queryColumns.get(26));


                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, tranDate);
                        insertStatement.setString(3, tranAmt);
                        insertStatement.setString(4, tranNarration);
                        if (serviceType.equals(ServiceType.NIP_OUTFLOW) && contraAcctNo4 != null && solId != null) {
                            contraAcctNo4 = contraAcctNo4.replace("----", solId);
                            insertStatement.setString(5, debit_accountNo);
                            insertStatement.setString(6, contraAcctNo4);
                        } else {
                            assert solId != null;
                            contraAcctNo5 = contraAcctNo5.replace("----", solId);
                            insertStatement.setString(5, debit_accountNo);
                            insertStatement.setString(6, contraAcctNo5);
                        }
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, trandId);
                        insertStatement.setString(9, solId);
                        insertStatement.setString(10, clientId);
                        insertStatement.setString(11, "IBTOUTFLOW");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "Y");
                        insertStatement.setString(14, FEECRNCY1);
                        insertStatement.setString(15, FEEAMT1);
                        insertStatement.setString(16, DRACCTNO1);
                        insertStatement.setString(17, CRACCTNO1);
                        insertStatement.setString(18, FEECRNCY2);
                        insertStatement.setString(19, FEEAMT2);
                        insertStatement.setString(20, DRACCTNO2);
                        insertStatement.setString(21, CRACCTNO2);
                        insertStatement.setString(22, FEECRNCY3);
                        insertStatement.setString(23, FEEAMT3);
                        insertStatement.setString(24, DRACCTNO3);
                        insertStatement.setString(25, CRACCTNO3);
                        insertStatement.setString(26, FEECRNCY4);
                        insertStatement.setString(27, FEEAMT4);
                        insertStatement.setString(28, DRACCTNO4);
                        insertStatement.setString(29, CRACCTNO4);
                        insertStatement.setString(30, FEECRNCY5);
                        insertStatement.setString(31, FEEAMT5);
                        insertStatement.setString(32, DRACCTNO5);
                        insertStatement.setString(33, CRACCTNO5);

                        insertStatement.addBatch();
                        totalValidated++;

                    }else {
                        break;
                    }

                } else {
                    if(serviceType.equals(ServiceType.AIRTIME)){
                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, " ");
                        insertStatement.setString(3, " ");
                        insertStatement.setString(4, " ");
                        insertStatement.setString(5, " ");
                        insertStatement.setString(6, " ");
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, " ");
                        insertStatement.setString(9, " ");
                        insertStatement.setString(10, " ");
                        insertStatement.setString(11, "AIRTIME");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "N");
                    }else {
                        // If no record is found, insert null values for missing data
                        insertStatement.setString(1, ref);
                        insertStatement.setString(2, " ");
                        insertStatement.setString(3, " ");
                        insertStatement.setString(4, " ");
                        insertStatement.setString(5, " ");
                        insertStatement.setString(6, " ");
                        insertStatement.setString(7, documentId);
                        insertStatement.setString(8, " ");
                        insertStatement.setString(9, " ");
                        insertStatement.setString(10, " ");
                        insertStatement.setString(11, " ");
                        insertStatement.setString(12, "NGN");
                        insertStatement.setString(13, "N");
                        insertStatement.setString(14, "NGN");
                        insertStatement.setString(15, "0");
                        insertStatement.setString(16, " ");
                        insertStatement.setString(17, " ");
                        insertStatement.setString(18, "NGN");
                        insertStatement.setString(19, "0");
                        insertStatement.setString(20, " ");
                        insertStatement.setString(21, " ");
                        insertStatement.setString(22, "NGN");
                        insertStatement.setString(23, "0");
                        insertStatement.setString(24, " ");
                        insertStatement.setString(25, " ");
                        insertStatement.setString(26, "NGN");
                        insertStatement.setString(27, "0");
                        insertStatement.setString(28, " ");
                        insertStatement.setString(29, " ");
                        insertStatement.setString(30, "NGN");
                        insertStatement.setString(31, "0");
                        insertStatement.setString(32, " ");
                        insertStatement.setString(33, " ");
                        insertStatement.addBatch();
                    }
                }

                batchCount++;

                // Execute batch when reaching the batch size
                if (batchCount % batchSize == 0) {
                    insertStatement.executeBatch();
                    conn.commit();  // Commit after each batch
                    batchCount = 0;  // Reset batch count
                }
            }

            // Execute any remaining statements in the batch
            if (batchCount > 0) {
                insertStatement.executeBatch();
                conn.commit();  // Commit the remaining batch
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.error("Error during transaction rollback", rollbackEx);
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (selectStatement != null) selectStatement.close();
                if (insertStatement != null) insertStatement.close();
                if (conn != null) conn.close();
            } catch (SQLException closeEx) {
                LOG.error("Error closing resources", closeEx);
            }
        }

        return totalValidated;
    }


    private String getServiceTable(ServiceType serviceType) {
        switch (serviceType) {
            case AIRTIME:
                return AppConstants.DbTables.AIRTIME_TABLE;
            case NIP_OUTFLOW:
                return AppConstants.DbTables.NIP_OUTFLOW_TRANSACTION;
            case NIP_INFLOW:
                return AppConstants.DbTables.NIP_INFLOW_TRANSACTION;
            case UP_INFLOW:
                return AppConstants.DbTables.UP_INFLOW_TRANSACTION;
            case UP_OUTFLOW:
                return AppConstants.DbTables.UP_OUTFLOW_TRANSACTION;
            default:
                throw new IllegalArgumentException("Invalid service type");
        }
    }

    private String getReferenceColumn(ServiceType serviceType) {
        return serviceType == ServiceType.AIRTIME ? "TOPUP_REF_ID" : "SESSIONID";
    }

    private void saveToModuleTable(ModuleName moduleName, String ref, String tranDate, BigDecimal tranAmt,
                                   String tranNarration, String accountNo, String creditAcctNo, String documentId, Connection conn) {
        String moduleTable = getModuleTable(moduleName);
        String insertQuery = "INSERT INTO " + moduleTable + " (tran_ref, tran_date, tran_amt, tran_narration, account_no, credit_acct_no, document_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setString(1, ref);
            pstmt.setDate(2, tranDate != null ? parseDate(tranDate) : null);
            pstmt.setBigDecimal(3, tranAmt);
            pstmt.setString(4, tranNarration);
            pstmt.setString(5, accountNo);
            pstmt.setString(6, creditAcctNo);
            pstmt.setString(7, documentId);
            pstmt.executeUpdate();
        } catch (SQLException | ParseException e) {
            LOG.error(e.getMessage()); // Log error
        }
    }

    private Date parseDate(String tranDate) throws ParseException {
        if (tranDate == null || tranDate.isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        java.util.Date parsedDate = sdf.parse(tranDate);
        return new Date(parsedDate.getTime());
    }

    private String getModuleTable(ModuleName moduleName) {
        switch (moduleName) {
            case STAND_REV:
                return AppConstants.DbTables.STANDARD_REVERSAL;
            case EXCEP_REV:
                return AppConstants.DbTables.EXCEPTIONAL_REVERSAL;
            case FUNDS_RECOUP:
                return AppConstants.DbTables.FUNDS_RECOUP;
            case UNSETTLED_TRXN:
                return AppConstants.DbTables.UNSETTLED_TRANSACTION;
            default:
                throw new IllegalArgumentException("Invalid module name");
        }
    }


    public String getQuery(ServiceType serviceType, String serviceTable, String referenceColumn){
        switch (serviceType){
            case AIRTIME:
                return "SELECT " + referenceColumn + ", ENTRYDATE, TXNAMT, NARRATION, ACCTNO, TOPUP_REF_ID, SOLID, CHANNELID " +
                        "FROM " + serviceTable + " WHERE " + referenceColumn + " = ?";
            case NIP_INFLOW:
            case UP_INFLOW:
                return "SELECT " + referenceColumn + ", REQUESTDATE, AMOUNT, NARRATION, BENEFICIARYACCOUNTNUMBER, TRANID, ACCT_SOL " +
                        "FROM " + serviceTable + " WHERE " + referenceColumn + " = ? AND TRANTYPE = 'FTSingleCreditRequest'";
            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return "SELECT " + referenceColumn + ", REQUESTDATE, AMOUNT, NARRATION, ORIGINATORACCOUNTNUMBER, FEECRNCY1, FEEAMT1, DRACCTNO1, CRACCTNO1, FEECRNCY2, FEEAMT2, DRACCTNO2, CRACCTNO2, FEECRNCY3, FEEAMT3, DRACCTNO3, CRACCTNO3, FEECRNCY4, FEEAMT4, DRACCTNO4, CRACCTNO4, FEECRNCY5, FEEAMT5, DRACCTNO5, CRACCTNO5, TRANID, ACCT_SOL, CLIENTNAME " +
                        "FROM " + serviceTable + " WHERE " + referenceColumn + " = ? AND TRANTYPE = 'FTSingleCreditRequest'";
            default:
                throw new IllegalArgumentException("Invalid service type");
        }
    }

    private List<String> getQueryColumns(ServiceType serviceType) {
        switch (serviceType){
            case AIRTIME:
                return new ArrayList<>(Arrays.asList("ENTRYDATE", "TXNAMT", "NARRATION", "ACCTNO", "TOPUP_REF_ID", "SOLID", "CHANNELID"));
            case NIP_INFLOW:
            case UP_INFLOW:
                return new ArrayList<>(Arrays.asList("REQUESTDATE", "AMOUNT", "NARRATION", "BENEFICIARYACCOUNTNUMBER", "TRANID", "ACCT_SOL"));
            case NIP_OUTFLOW:
            case UP_OUTFLOW:
                return new ArrayList<>(Arrays.asList("REQUESTDATE", "AMOUNT", "NARRATION", "ORIGINATORACCOUNTNUMBER", "FEECRNCY1", "FEEAMT1", "DRACCTNO1", "CRACCTNO1", "FEECRNCY2", "FEEAMT2", "DRACCTNO2", "CRACCTNO2", "FEECRNCY3", "FEEAMT3", "DRACCTNO3", "CRACCTNO3", "FEECRNCY4", "FEEAMT4", "DRACCTNO4", "CRACCTNO4", "FEECRNCY5", "FEEAMT5", "DRACCTNO5", "CRACCTNO5", "TRANID", "ACCT_SOL", "CLIENTNAME"));
            default:
                throw new IllegalArgumentException("Invalid service type");
        }
    }

    public void saveFileUploadMetadata(String fileName, String documentId, ModuleName moduleName, ServiceType serviceType, int totalUnapprovedTran, int totalValidated, int totalFailed, String currentUser) {

        String query = "INSERT INTO " + AppConstants.DbTables.FILE_REQUEST_TABLE + " (document_name, document_type, document_id, module_name, service_type, upload_date, uploaded_by, total_unapproved_tran, total_unapproved_validated, total_unapproved_failed) " +
                "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, ?, ?, ?, ?)";

        LOG.info("Insert query for the File upload MC table: {}", query);

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = ConnectionUtil.getConnection();
            pstmt = conn.prepareStatement(query);
            conn.setAutoCommit(false);
            pstmt.setString(1, fileName);
            pstmt.setString(2, "Excel");
            pstmt.setString(3, documentId);
            pstmt.setString(4, moduleName.name());
            pstmt.setString(5, serviceType.name());
            pstmt.setString(6, currentUser);
            pstmt.setInt(7, totalUnapprovedTran);
            pstmt.setInt(8, totalValidated);
            pstmt.setInt(9, totalFailed);
            pstmt.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    public void updateFileUploadMetadata(String documentId, int totalValidated, int totalFailed) {
        String query = "UPDATE "+ AppConstants.DbTables.FILE_REQUEST_TABLE + " SET total_unapproved_validated = ?, total_unapproved_failed = ? WHERE document_id = ?";

        LOG.info("Update query for the File upload MC table: {}", query);

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = ConnectionUtil.getConnection();
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, totalValidated);
            pstmt.setInt(2, totalFailed);
            pstmt.setString(3, documentId);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    public void batchSaveSettlementReport(List<SettlementReportRecord> settlementRecords) {
        String sql = "INSERT INTO " + AppConstants.DbTables.SETTLEMENT_REPORT + " (S_N, CHANNEL, SESSION_ID, TRANSACTION_TYPE, RESPONSE, AMOUNT, "
                + "TRANSACTION_TIME, ORIGINATOR_INSTITUTION, ORIGINATOR_BILLER, DESTINATION_INSTITUTION, DESTINATION_ACCOUNT_NAME, "
                + "DESTINATION_ACCOUNT_NO, NARRATION, PAYMENT_REFERENCE, LAST_12_DIGITS_OF_SESSION_ID, DOCUMENT_ID, SERVICETYPE, UPLOADEDBY) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LOG.info("Insert query for the SETTLEMENT_REPORT table: {}", sql);

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = ConnectionUtil.getConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(sql);


            for (SettlementReportRecord record : settlementRecords) {
                preparedStatement.setString(1, record.getSn());
                preparedStatement.setString(2, record.getChannel());
                preparedStatement.setString(3, record.getSessionId());
                preparedStatement.setString(4, record.getTransactionType());
                preparedStatement.setString(5, record.getResponse());
                preparedStatement.setDouble(6, record.getAmount());
                preparedStatement.setString(7, record.getTransactionTime());
                preparedStatement.setString(8, record.getOriginatorInstitution());
                preparedStatement.setString(9, record.getOriginatorBiller());
                preparedStatement.setString(10, record.getDestinationInstitution());
                preparedStatement.setString(11, record.getDestinationAccountName());
                preparedStatement.setString(12, record.getDestinationAccountNo());
                preparedStatement.setString(13, record.getNarration());
                preparedStatement.setString(14, record.getPaymentReference());
                preparedStatement.setString(15, record.getLast12DigitsOfSessionId());
                preparedStatement.setString(16, record.getDocumentId());
                preparedStatement.setString(17, record.getServiceType());
                preparedStatement.setString(18, record.getUploadedBy());

                preparedStatement.addBatch();  // Add record to batch
            }

            preparedStatement.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeResources(connection, preparedStatement, null);
        }
    }

    public JsonObject getSettlementStatusEnquiryWithAccountNo(String acctNo, String startDate, String endDate){
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();

        String query = "SELECT S_N, DOCUMENT_ID, TRAN_STATUS, CHANNEL, SESSION_ID, TRANSACTION_TYPE, RESPONSE, AMOUNT, TRANSACTION_TIME, " +
                "ORIGINATOR_INSTITUTION, ORIGINATOR_BILLER, DESTINATION_INSTITUTION, DESTINATION_ACCOUNT_NAME, " +
                "DESTINATION_ACCOUNT_NO, NARRATION, PAYMENT_REFERENCE, SERVICETYPE " +
                "FROM ESBUSER.SETTLEMENT_REPORT " +
                "WHERE DESTINATION_ACCOUNT_NO = ? " +
                "AND TO_DATE(SUBSTR(TRANSACTION_TIME, 2), 'YYYY-MM-DD HH24:MI:SS') BETWEEN TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') " +
                "AND TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') " +
                "FETCH FIRST 1 ROW ONLY";


        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        try{
            connection = ConnectionUtil.getConnection();
            preparedStatement = connection.prepareStatement(query);


            preparedStatement.setString(1, acctNo);
            preparedStatement.setString(2, startDate);
            preparedStatement.setString(3, endDate);

            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                JsonObjectBuilder recordBuilder = Json.createObjectBuilder();
                recordBuilder
                        .add("sno", rs.getString("S_N") != null ? rs.getString("S_N") : "")
                        .add("document_id", rs.getString("DOCUMENT_ID") != null ? rs.getString("DOCUMENT_ID") : "")
                        .add("tran_status", rs.getString("TRAN_STATUS") != null ? rs.getString("TRAN_STATUS") : "")
                        .add("channel", rs.getString("CHANNEL") != null ? rs.getString("CHANNEL") : "")
                        .add("session_id", rs.getString("SESSION_ID") != null ? rs.getString("SESSION_ID") : "")
                        .add("tran_type", rs.getString("TRANSACTION_TYPE") != null ? rs.getString("TRANSACTION_TYPE") : "")
                        .add("response", rs.getString("RESPONSE") != null ? rs.getString("RESPONSE") : "")
                        .add("amount", rs.getBigDecimal("AMOUNT") != null ? rs.getBigDecimal("AMOUNT").toString() : "")
                        .add("tran_time", rs.getString("TRANSACTION_TIME") != null ? rs.getString("TRANSACTION_TIME") : "")
                        .add("org_inst", rs.getString("ORIGINATOR_INSTITUTION") != null ? rs.getString("ORIGINATOR_INSTITUTION") : "")
                        .add("biller", rs.getString("ORIGINATOR_BILLER") != null ? rs.getString("ORIGINATOR_BILLER") : "")
                        .add("dest_inst", rs.getString("DESTINATION_INSTITUTION") != null ? rs.getString("DESTINATION_INSTITUTION") : "")
                        .add("dest_acct_name", rs.getString("DESTINATION_ACCOUNT_NAME") != null ? rs.getString("DESTINATION_ACCOUNT_NAME") : "")
                        .add("dest_acct_no", rs.getString("DESTINATION_ACCOUNT_NO") != null ? rs.getString("DESTINATION_ACCOUNT_NO") : "")
                        .add("narration", rs.getString("NARRATION") != null ? rs.getString("NARRATION") : "")
                        .add("pay_ref", rs.getString("PAYMENT_REFERENCE") != null ? rs.getString("PAYMENT_REFERENCE") : "")
                        .add("service_type", rs.getString("SERVICETYPE") != null ? rs.getString("SERVICETYPE") : "");

                dataBuilder.add(recordBuilder.build());
            }


            return Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Success")
                    .add("data", dataBuilder)
                    .build();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "An error occurred while processing your request.")
                    .build();
        } finally {
            closeResources(connection, preparedStatement, rs);
        }
    }

    public JsonObject getSettlementStatusEnquiryWithSessionId(String sessionId, String startDate, String endDate){
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();

        String query = "SELECT S_N, DOCUMENT_ID, TRAN_STATUS, CHANNEL, SESSION_ID, TRANSACTION_TYPE, RESPONSE, AMOUNT, TRANSACTION_TIME, " +
                "ORIGINATOR_INSTITUTION, ORIGINATOR_BILLER, DESTINATION_INSTITUTION, DESTINATION_ACCOUNT_NAME, " +
                "DESTINATION_ACCOUNT_NO, NARRATION, PAYMENT_REFERENCE, SERVICETYPE " +
                "FROM ESBUSER.SETTLEMENT_REPORT " +
                "WHERE SESSION_ID = ? " +
                "AND TO_DATE(SUBSTR(TRANSACTION_TIME, 2), 'YYYY-MM-DD HH24:MI:SS') BETWEEN TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') " +
                "AND TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') " +
                "FETCH FIRST 1 ROW ONLY";

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;


        try{
            connection = ConnectionUtil.getConnection();
            preparedStatement = connection.prepareStatement(query);


            preparedStatement.setString(1, sessionId);
            preparedStatement.setString(2, startDate);
            preparedStatement.setString(3, endDate);

            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                JsonObjectBuilder recordBuilder = Json.createObjectBuilder();
                recordBuilder
                        .add("sno", rs.getString("S_N") != null ? rs.getString("S_N") : "")
                        .add("document_id", rs.getString("DOCUMENT_ID") != null ? rs.getString("DOCUMENT_ID") : "")
                        .add("tran_status", rs.getString("TRAN_STATUS") != null ? rs.getString("TRAN_STATUS") : "")
                        .add("channel", rs.getString("CHANNEL") != null ? rs.getString("CHANNEL") : "")
                        .add("session_id", rs.getString("SESSION_ID") != null ? rs.getString("SESSION_ID") : "")
                        .add("tran_type", rs.getString("TRANSACTION_TYPE") != null ? rs.getString("TRANSACTION_TYPE") : "")
                        .add("response", rs.getString("RESPONSE") != null ? rs.getString("RESPONSE") : "")
                        .add("amount", rs.getBigDecimal("AMOUNT") != null ? rs.getBigDecimal("AMOUNT").toString() : "")
                        .add("tran_time", rs.getString("TRANSACTION_TIME") != null ? rs.getString("TRANSACTION_TIME") : "")
                        .add("org_inst", rs.getString("ORIGINATOR_INSTITUTION") != null ? rs.getString("ORIGINATOR_INSTITUTION") : "")
                        .add("biller", rs.getString("ORIGINATOR_BILLER") != null ? rs.getString("ORIGINATOR_BILLER") : "")
                        .add("dest_inst", rs.getString("DESTINATION_INSTITUTION") != null ? rs.getString("DESTINATION_INSTITUTION") : "")
                        .add("dest_acct_name", rs.getString("DESTINATION_ACCOUNT_NAME") != null ? rs.getString("DESTINATION_ACCOUNT_NAME") : "")
                        .add("dest_acct_no", rs.getString("DESTINATION_ACCOUNT_NO") != null ? rs.getString("DESTINATION_ACCOUNT_NO") : "")
                        .add("narration", rs.getString("NARRATION") != null ? rs.getString("NARRATION") : "")
                        .add("pay_ref", rs.getString("PAYMENT_REFERENCE") != null ? rs.getString("PAYMENT_REFERENCE") : "")
                        .add("service_type", rs.getString("SERVICETYPE") != null ? rs.getString("SERVICETYPE") : "");

                dataBuilder.add(recordBuilder.build());
            }

            return Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Success")
                    .add("data", dataBuilder)
                    .build();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "An error occurred while processing your request.")
                    .build();
        } finally {
            closeResources(connection, preparedStatement, rs);
        }
    }

    public JsonObject getPaymentSummariesByDateRange(String module, String startDate, String endDate, String batchId, int size, int page) {
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        String mainQuery;
        String countQuery;
        if(module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_SUMMARY))){
            mainQuery = "SELECT * FROM (" +
                    "    SELECT BATCHID, TOTALAMOUNT, ITEMCOUNT, DEBIT_ACCOUNT_NO, DEBIT_BANKCODE, NARRATION, " +
                    "           CTRYCODE, CRNCYCODE, ENTRY_DATE, VALIDATN_RSP_CODE, SUBMIT_STATUS_CODE, " +
                    "           ROW_NUMBER() OVER (ORDER BY ENTRY_DATE DESC) AS row_num " +
                    "    FROM ESBUSER.interbankbulkpaymentsummary " +
                    "    WHERE ENTRY_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";

            countQuery = "SELECT COUNT(1) AS total_rows " +
                    "FROM ESBUSER.interbankbulkpaymentsummary " +
                    "WHERE ENTRY_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";
        }else {
            mainQuery = "SELECT * FROM (" +
                    "    SELECT BATCHID, TOTALAMOUNT, ITEMCOUNT, DEBIT_ACCOUNT_NO, DEBIT_BANKCODE, NARRATION, " +
                    "           CTRYCODE, CRNCYCODE, ENTRY_DATE, VALIDATN_RSP_CODE, " +
                    "           ROW_NUMBER() OVER (ORDER BY ENTRY_DATE DESC) AS row_num " +
                    "    FROM ESBUSER.intrabankbulkpaymentsummary " +
                    "    WHERE ENTRY_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";

            countQuery = "SELECT COUNT(1) AS total_rows " +
                    "FROM ESBUSER.intrabankbulkpaymentsummary " +
                    "WHERE ENTRY_DATE BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";
        }


        // Add batchId condition if provided
        if (batchId != null && !batchId.isEmpty()) {
            mainQuery += " AND BATCHID = ? ";
            countQuery += " AND BATCHID = ? ";
        }

        mainQuery += ") WHERE row_num BETWEEN ? AND ?";

        Connection connection = null;
        PreparedStatement mainStmt = null;
        PreparedStatement countStmt = null;
        ResultSet rs = null;
        ResultSet countRs = null;

        try {
            connection = ConnectionUtil.getConnection();
            mainStmt = connection.prepareStatement(mainQuery);
            countStmt = connection.prepareStatement(countQuery);

            // Set startDate and endDate parameters
            mainStmt.setString(1, startDate);
            mainStmt.setString(2, endDate);
            countStmt.setString(1, startDate);
            countStmt.setString(2, endDate);

            int parameterIndex = 3;

            // Set batchId parameter if it was provided
            if (batchId != null && !batchId.isEmpty()) {
                mainStmt.setString(parameterIndex, batchId);
                countStmt.setString(3, batchId);
                parameterIndex++;
            }

            mainStmt.setInt(parameterIndex++, startRow);
            mainStmt.setInt(parameterIndex, endRow);

            int totalRows = 0;
            countRs = countStmt.executeQuery();
            if (countRs.next()) {
                totalRows = countRs.getInt("total_rows");
            }


            rs = mainStmt.executeQuery();
            boolean recordsFound = false;
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                recordsFound = true;
                JsonObjectBuilder recordBuilder = Json.createObjectBuilder();

                recordBuilder
                        .add("batchid", rs.getString("BATCHID"))
                        .add("totalamount", rs.getBigDecimal("TOTALAMOUNT").toString())
                        .add("itemcount", rs.getInt("ITEMCOUNT"))
                        .add("debit_account_no", rs.getString("DEBIT_ACCOUNT_NO"))
                        .add("debit_bankcode", rs.getString("DEBIT_BANKCODE"))
                        .add("narration", rs.getString("NARRATION") != null ? rs.getString("NARRATION") : "")
                        .add("ctrycode", rs.getString("CTRYCODE"))
                        .add("crncycode", rs.getString("CRNCYCODE"))
                        .add("entry_date", rs.getString("ENTRY_DATE"))
                        .add("validatn_rsp_code", rs.getString("VALIDATN_RSP_CODE") != null ? rs.getString("VALIDATN_RSP_CODE") : "");

                if (module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_SUMMARY))) {
                    recordBuilder
                            .add("submit_status_code", rs.getString("SUBMIT_STATUS_CODE") != null ? rs.getString("SUBMIT_STATUS_CODE") : "");
                }

                dataBuilder.add(recordBuilder.build());
            }

            // Build and return the final JSON object
            return Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", recordsFound ? "Success" : "No records found")
                    .add("total_rows", totalRows)
                    .add("data", dataBuilder)
                    .build();

        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "An error occurred while processing your request.")
                    .build();
        } finally {
            try {
                if (rs != null) rs.close();
                if (countRs != null) countRs.close();
                if (mainStmt != null) mainStmt.close();
                if (countStmt != null) countStmt.close();
                if (connection != null) connection.close();
            } catch (SQLException closeEx) {
                LOG.error("Error closing resources", closeEx);
            }
        }
    }

    public JsonObject getPaymentDetailsByBatchId(String module, String batchId, int size, int page) {
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();

        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        String mainQuery;
        String countQuery;

        if(module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_SUMMARY))){
            mainQuery = "SELECT * FROM (" +
                    "    SELECT batchid, credt_bankcode, amount, transactionid, narration, entry_date, " +
                    "           credt_account_no, " +
                    "           CASE tsq_rsp_code " +
                    "               WHEN '09' THEN 'Pending' " +
                    "               WHEN '25' THEN 'Pending' " +
                    "               WHEN '96' THEN 'Pending' " +
                    "               WHEN '97' THEN 'Pending' " +
                    "               WHEN '00' THEN 'Success' " +
                    "               ELSE 'Failed' " +
                    "           END AS tsq_status_code, " +
                    "           CASE debit_resp_code " +
                    "               WHEN '09' THEN 'Pending' " +
                    "               WHEN '25' THEN 'Pending' " +
                    "               WHEN '96' THEN 'Pending' " +
                    "               WHEN '97' THEN 'Pending' " +
                    "               WHEN '00' THEN 'Success' " +
                    "               ELSE 'Failed' " +
                    "           END AS debit_resp_code, " +
                    "           session_id, debit_session_id, tsq_timestamp, " +
                    "           ROW_NUMBER() OVER (ORDER BY entry_date DESC) AS row_num " +
                    "    FROM esbuser.interbankbulkpaymentdetail " +
                    "    WHERE batchid = ? " +
                    ") WHERE row_num BETWEEN ? AND ?";

            countQuery = "SELECT COUNT(1) AS total_rows " +
                    "FROM esbuser.interbankbulkpaymentdetail " +
                    "WHERE batchid = ?";
        }else {
            mainQuery = "SELECT * FROM (" +
                    "    SELECT batchid, credt_bankcode, amount, transactionid, narration, entry_date, " +
                    "           credt_account_no, " +
                    "           CASE tsq_rsp_code " +
                    "               WHEN '09' THEN 'Pending' " +
                    "               WHEN '25' THEN 'Pending' " +
                    "               WHEN '96' THEN 'Pending' " +
                    "               WHEN '97' THEN 'Pending' " +
                    "               WHEN '00' THEN 'Success' " +
                    "               ELSE 'Failed' " +
                    "           END AS tsq_status_code, " +
                    "           ROW_NUMBER() OVER (ORDER BY entry_date DESC) AS row_num " +
                    "    FROM esbuser.intrabankbulkpaymentdetail " +
                    "    WHERE batchid = ? " +
                    ") WHERE row_num BETWEEN ? AND ?";

            countQuery = "SELECT COUNT(1) AS total_rows " +
                    "FROM esbuser.intrabankbulkpaymentdetail " +
                    "WHERE batchid = ?";
        }

        Connection connection = null;
        PreparedStatement mainStmt = null;
        PreparedStatement countStmt = null;
        ResultSet countRs = null;
        ResultSet rs = null;

        try{
            connection = ConnectionUtil.getConnection();
            mainStmt = connection.prepareStatement(mainQuery);
            countStmt = connection.prepareStatement(countQuery);

            mainStmt.setString(1, batchId);
            mainStmt.setInt(2, startRow);
            mainStmt.setInt(3, endRow);

            countStmt.setString(1, batchId);

            int totalRows = 0;
            countRs = countStmt.executeQuery();
            if (countRs.next()) {
                totalRows = countRs.getInt("total_rows");
            }
            rs = mainStmt.executeQuery();
            boolean recordsFound = false;

            while (rs.next()) {
                recordsFound = true;
                JsonObjectBuilder recordBuilder = Json.createObjectBuilder();

                recordBuilder
                        .add("batchid", rs.getString("batchid"))
                        .add("credt_bankcode", rs.getString("credt_bankcode"))
                        .add("amount", rs.getBigDecimal("amount").toString())
                        .add("transactionid", rs.getString("transactionid"))
                        .add("narration", rs.getString("narration") != null ? rs.getString("narration") : "")
                        .add("entry_date", rs.getString("entry_date"))
                        .add("credt_account_no", rs.getString("credt_account_no"))
                        .add("tsq_status_code", rs.getString("tsq_status_code"));


                if (module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_SUMMARY))) {
                    recordBuilder
                            .add("debit_resp_code", rs.getString("debit_resp_code"))
                            .add("session_id", rs.getString("session_id") != null ? rs.getString("session_id") : "")
                            .add("debit_session_id", rs.getString("debit_session_id") != null ? rs.getString("debit_session_id") : "")
                            .add("tsq_timestamp", rs.getString("tsq_timestamp") != null ? rs.getString("tsq_timestamp") : "");
                }

                dataBuilder.add(recordBuilder.build());
            }

            // Build and return the final JSON object
            return Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", recordsFound ? "Success" : "No records found")
                    .add("total_rows", totalRows)
                    .add("data", dataBuilder)
                    .build();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "An error occurred while processing your request.")
                    .build();
        } finally {
            try {
                if (rs != null) rs.close();
                if (countRs != null) countRs.close();
                if (mainStmt != null) mainStmt.close();
                if (countStmt != null) countStmt.close();
                if (connection != null) connection.close();
            } catch (SQLException closeEx) {
                LOG.error("Error closing resources", closeEx);
            }
        }
    }


    public JsonObject getBatchStatisticsByBatchIdAndGroupedByStatus(String module, String batchId) {
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();

        String query;
        if(module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_SUMMARY))){
            query = "SELECT batchid, COUNT(1) AS transaction_count, SUM(amount) AS total_amount, " +
                    "CASE tsq_rsp_code " +
                    "    WHEN '09' THEN 'Pending' " +
                    "    WHEN '25' THEN 'Pending' " +
                    "    WHEN '96' THEN 'Pending' " +
                    "    WHEN '97' THEN 'Pending' " +
                    "    WHEN '00' THEN 'Success' " +
                    "    ELSE 'Failed' " +
                    "END AS tsq_status_code " +
                    "FROM esbuser.interbankbulkpaymentdetail " +
                    "WHERE batchid = ? " +
                    "GROUP BY batchid, tsq_rsp_code";
        }else {
            query = "SELECT batchid, COUNT(1) AS transaction_count, SUM(amount) AS total_amount, " +
                    "CASE tsq_rsp_code " +
                    "    WHEN '09' THEN 'Pending' " +
                    "    WHEN '25' THEN 'Pending' " +
                    "    WHEN '96' THEN 'Pending' " +
                    "    WHEN '97' THEN 'Pending' " +
                    "    WHEN '00' THEN 'Success' " +
                    "    ELSE 'Failed' " +
                    "END AS tsq_status_code " +
                    "FROM esbuser.intrabankbulkpaymentdetail " +
                    "WHERE batchid = ? " +
                    "GROUP BY batchid, tsq_rsp_code";
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        try{
            connection = ConnectionUtil.getConnection();
            preparedStatement = connection.prepareStatement(query);

            // Set the batchId parameter
            preparedStatement.setString(1, batchId);

            rs = preparedStatement.executeQuery();
            boolean recordsFound = false;

            while (rs.next()) {
                recordsFound = true;
                JsonObjectBuilder recordBuilder = Json.createObjectBuilder();

                recordBuilder
                        .add("batchid", rs.getString("batchid"))
                        .add("transaction_count", rs.getInt("transaction_count"))
                        .add("total_amount", rs.getBigDecimal("total_amount").toString())
                        .add("tsq_status_code", rs.getString("tsq_status_code"));

                dataBuilder.add(recordBuilder.build());
            }

            // Build and return the final JSON response
            return Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", recordsFound ? "Statistics retrieved successfully" : "No records found for the given batch ID")
                    .add("data", dataBuilder)
                    .build();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "An error occurred while processing your request.")
                    .build();
        } finally {
            closeResources(connection, preparedStatement, rs);
        }
    }

    private void closeResources(Connection conn, PreparedStatement preparedStatement, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.error("Error closing ResultSet: {}", e.getMessage());
            }
        }
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                LOG.error("Error closing PreparedStatement: {}", e.getMessage());
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.error("Error closing Connection: {}", e.getMessage());
            }
        }
    }
}
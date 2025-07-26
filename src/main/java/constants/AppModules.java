package constants;

import constants.AppConstants.DbTables;

import java.util.ArrayList;
import java.util.List;

public enum AppModules {


    FUNDS_RECOUP("funds_recoup", "Funds recoup", DbTables.FUNDS_RECOUP, getFundsRecoupTableColumns(), "funds-recoup"),
    STANDARD_REVERSAL("stand_rev", "Standard reversal", DbTables.STANDARD_REVERSAL, getStandardRevColumns(), "standard-reversals"),
    EXCEPTIONAL_REVERSAL("excep_rev", "Exceptional reversal", DbTables.EXCEPTIONAL_REVERSAL, getExceptionalRevColumns(), "exceptional-reversals"),
    UNSETTLED_TRANSACTION("unsettled_trxn", "Unsettled transaction", DbTables.UNSETTLED_TRANSACTION, getgetUnsettledTransactionColumns(), "unsettled-transactions"),
    SETTLEMENT_REPORT("settlement_report", "Settlement report", DbTables.SETTLEMENT_REPORT, getgetUnsettledTransactionColumns(), "settlement-report"),
    FAILED_TRANSACTION("failed-transactions");

    private final String code;
    private final String description;

    private final String tableName;

    private final List<String> tableColumns;
    private final String fileNameFormat;

    AppModules(String fileNameFormat) {
        this.code = "";
        this.description = "";
        this.tableName = "";
        this.tableColumns = new ArrayList<>();
        this.fileNameFormat = fileNameFormat;
    }

    AppModules(String code, String description, String tableName, List<String> tableColumns, String fileNameFormat) {
        this.code = code;
        this.description = description;
        this.tableName = tableName;
        this.tableColumns = tableColumns;
        this.fileNameFormat = fileNameFormat;

    }

    private static List<String> getFundsRecoupTableColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("serial_no");
        columns.add("tran_ref");
        columns.add("tran_date");
        columns.add("tran_amt");
        columns.add("document_id");
        columns.add("tran_narration");
        columns.add("account_no");
        columns.add("credit_acct_no");

        return columns;
    }

    private static List<String> getStandardRevColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("serial_no");
        columns.add("tran_ref");
        columns.add("tran_date");
        columns.add("tran_amt");
        columns.add("document_id");
        columns.add("tran_narration");
        columns.add("account_no");
        columns.add("credit_acct_no");

        return columns;
    }

    private static List<String> getExceptionalRevColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("serial_no");
        columns.add("tran_ref");
        columns.add("tran_date");
        columns.add("tran_amt");
        columns.add("document_id");
        columns.add("tran_narration");
        columns.add("account_no");
        columns.add("credit_acct_no");

        return columns;
    }


    private static List<String> getgetUnsettledTransactionColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("serial_no");
        columns.add("tran_ref");
        columns.add("tran_date");
        columns.add("tran_amt");
        columns.add("document_id");
        columns.add("tran_narration");
        columns.add("account_no");
        columns.add("credit_acct_no");

        return columns;
    }


    public String getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }


    public String getTableName() {
        return tableName;
    }


    public List<String> getTableColumns() {
        return tableColumns;
    }

    public String getFileNameFormat() {
        return fileNameFormat;
    }
}

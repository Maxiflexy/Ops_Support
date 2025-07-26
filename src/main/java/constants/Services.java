package constants;

import constants.AppConstants.DbTables;

import static constants.AppConstants.ServiceQueries.*;

public enum Services {

//    /Airtime date: entry_date,
    // UP_INFLOW Inflow date: requestdate,
// UP_OUTFLOW: requestdate,
//NIP_INFLOW: requestdate,
//NIP OUTFLOW:
    AIRTIME("AIRTIME", "AIRTIME TRANSACTIONS", DbTables.AIRTIME_TABLE, "entrydate", "txn_status", "debit_reversal_rsp_flg", "debit_rsp_flg", "txnamt", "TOPUP_REF_ID", AIRTIME_FAILED, AIRTIME_PENDING, AIRTIME_SUCCESS, AIRTIME_ACCOUNT_DEBIT_SUCCESS, AIRTIME_ACCOUNT_DEBIT_FAILED, AIRTIME_REVERSAL_SUCCESS, AIRTIME_REVERSAL_FAILED, "airt", "", ""),
    NIP_INFLOW("NIP_INFLOW", "NIP INFLOW TRANSACTIONS", DbTables.NIP_INFLOW_TRANSACTION, "requestdate", "txn_status", "rev_status", "acct_debit_status", "amount", "sessionid", NIP_INFLOW_FAILED, NIP_INFLOW_PENDING, NIP_INFLOW_SUCCESS, "", "", "", "", "nip-inf", "TARGETACCOUNTNUMBER", ""),
    NIP_OUTFLOW("NIP_OUTFLOW", "NIP OUTFLOW TRANSACTIONS", DbTables.NIP_OUTFLOW_TRANSACTION, "requestdate", "txn_status","rev_status", "acct_debit_status", "amount", "sessionid", UP_OUTFLOW_FAILED, UP_OUTFLOW_PENDING, UP_OUTFLOW_SUCCESS, UP_OUTFLOW_ACCOUNT_DEBIT_SUCCESS, UP_OUTFLOW_ACCOUNT_DEBIT_FAILED, UP_OUTFLOW_REVERSAL_SUCCESS, UP_OUTFLOW_REVERSAL_FAILED, "nip-out", "ORIGINATORACCOUNTNUMBER", UP_OUTFLOW_DEBIT_SUCCESS_FAILED_TXN),
    UP_INFLOW("UP_INFLOW", "UP INFLOW TRANSACTIONS", DbTables.UP_INFLOW_TRANSACTION, "requestdate", "txn_status", "rev_status", "acct_debit_status", "amount", "sessionid", UP_INFLOW_FAILED, UP_INFLOW_PENDING, UP_INFLOW_SUCCESS, "", "", "", "", "up-inf", "", ""),
    UP_OUTFLOW("UP_OUTFLOW", "UP OUTFLOW TRANSACTIONS", DbTables.UP_OUTFLOW_TRANSACTION, "requestdate", "txn_status", "rev_status", "acct_debit_status", "amount", "sessionid", UP_OUTFLOW_FAILED, UP_OUTFLOW_PENDING, UP_OUTFLOW_SUCCESS, UP_OUTFLOW_ACCOUNT_DEBIT_SUCCESS, UP_OUTFLOW_ACCOUNT_DEBIT_FAILED, UP_OUTFLOW_REVERSAL_SUCCESS, UP_OUTFLOW_REVERSAL_FAILED, "up-out", "", UP_OUTFLOW_DEBIT_SUCCESS_FAILED_TXN);

    public  String serviceName;
    public  String serviceDescription;
    public  String serviceTable;

    public String tranDate;
    public String tranAmount;
    public String tranRef;
    public String tranStatus;
    public String reversalStatus;
    public String debitStatus;

    private String failedTransactionStatusQuery;
    private String pendingTransactionStatusQuery;
    private String successTransactionStatusQuery;
    private String accountDebitSuccessQuery;
    private String accountDebitFailureQuery;
    private String reversalSuccessQuery;
    private String reversalFailureQuery;
    private String short_code;
    private String accountNumber;
    private final String debitQueryWithFailedTxn;


    Services(String serviceName, String serviceDescription, String serviceTable, String tranDate, String tranStatus, String reversalStatus, String debitStatus, String tranAmnt, String tranRef, String failedTransactionStatusQuery, String pendingTransactionStatusQuery, String successTransactionStatusQuery, String accountDebitSuccessQuery, String accountDebitFailureQuery, String reversalSuccessQuery, String reversalFailureQuery, String short_code, String accountNumber, String debitQueryWithFailedTxn) {
        this.serviceName = serviceName;
        this.serviceDescription = serviceDescription;
        this.serviceTable = serviceTable;
        this.tranDate = tranDate;
        this.tranStatus = tranStatus;
        this.reversalStatus = reversalStatus;
        this.debitStatus = debitStatus;
        this.tranAmount = tranAmnt;
        this.tranRef = tranRef;
        this.failedTransactionStatusQuery = failedTransactionStatusQuery;
        this.pendingTransactionStatusQuery = pendingTransactionStatusQuery;
        this.successTransactionStatusQuery = successTransactionStatusQuery;
        this.accountDebitSuccessQuery = accountDebitSuccessQuery;
        this.accountDebitFailureQuery = accountDebitFailureQuery;
        this.reversalSuccessQuery = reversalSuccessQuery;
        this.reversalFailureQuery = reversalFailureQuery;
        this.short_code = short_code;
        this.accountNumber = accountNumber;

        this.debitQueryWithFailedTxn = debitQueryWithFailedTxn;
    }


    public  String getServiceName() {
        return serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public String getServiceTable() {
        return serviceTable;
    }

    public String getTranDate() {
        return tranDate;
    }

    public String getTranAmount() {
        return tranAmount;
    }

    public String getTranRef() {
        return tranRef;
    }

    public String getTranStatus() {
        return tranStatus;
    }

    public String getReversalStatus() {
        return reversalStatus;
    }

    public String getDebitStatus() {
        return debitStatus;
    }

    public String getFailedTransactionStatusQuery() {
        return failedTransactionStatusQuery;
    }

    public String getPendingTransactionStatusQuery() {
        return pendingTransactionStatusQuery;
    }

    public String getSuccessTransactionStatusQuery() {
        return successTransactionStatusQuery;
    }

    public String getAccountDebitSuccessQuery() {
        return accountDebitSuccessQuery;
    }

    public String getAccountDebitFailureQuery() {
        return accountDebitFailureQuery;
    }

    public String getReversalSuccessQuery() {
        return reversalSuccessQuery;
    }

    public String getReversalFailureQuery() {
        return reversalFailureQuery;
    }

    public String getShort_code() {
        return short_code;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getDebitQueryWithFailedTxn() {
        return debitQueryWithFailedTxn;
    }
}

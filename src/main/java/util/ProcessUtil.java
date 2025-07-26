package util;

public enum ProcessUtil {

    ENTRUST("validatetoken", "ENTRUST"),
    REVERSAL_REQUEST("validatetoken", "UPLOAD_FILE_REQUEST"),
    REVERSAL_APPROVED("validatetoken", "UPLOAD_FILE_APPROVED"),
    FUNDS_RECOUP_REQUEST("validatetoken", "UPLOAD_FILE_REQUEST"),
    FAILED_TRNASACTION_REQUEST("validatetoken", "FAILED_TRANSACTION_REPORT"),
    FUNDS_RECOUP_REQUEST_APPROVED("validatetoken", "UPLOAD_FILE_APPROVED"),
    SETTLEMENT_REQUEST("validatetoken", "UPLOAD_FILE_REQUEST"),
    SETTLEMENT_APPROVED("validatetoken", "UPLOAD_FILE_APPROVED"),
    USER_CREATION("validatetoken", "USER_CREATION"),
    FILE_UPLOAD_APPROVED("validatetoken", "UPLOAD_FILE_APPROVED");


    public String URI = null;
    public String MESSAGE_TYPE = null;

    ProcessUtil(String URI, String MESSAGE_TYPE) {

        this.URI = URI;
        this.MESSAGE_TYPE = MESSAGE_TYPE;

    }

}

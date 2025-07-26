package util;

public class SettlementReportRecord {

    private String sn;
    private String channel;
    private String sessionId;
    private String transactionType;
    private String response;
    private double amount;
    private String transactionTime;
    private String originatorInstitution;
    private String originatorBiller;
    private String destinationInstitution;
    private String destinationAccountName;
    private String destinationAccountNo;
    private String narration;
    private String paymentReference;
    private String last12DigitsOfSessionId;
    private String documentId;
    private String serviceType;
    private String uploadedBy;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDestinationAccountName() {
        return destinationAccountName;
    }

    public void setDestinationAccountName(String destinationAccountName) {
        this.destinationAccountName = destinationAccountName;
    }

    public String getDestinationAccountNo() {
        return destinationAccountNo;
    }

    public void setDestinationAccountNo(String destinationAccountNo) {
        this.destinationAccountNo = destinationAccountNo;
    }

    public String getDestinationInstitution() {
        return destinationInstitution;
    }

    public void setDestinationInstitution(String destinationInstitution) {
        this.destinationInstitution = destinationInstitution;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getLast12DigitsOfSessionId() {
        return last12DigitsOfSessionId;
    }

    public void setLast12DigitsOfSessionId(String last12DigitsOfSessionId) {
        this.last12DigitsOfSessionId = last12DigitsOfSessionId;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getOriginatorBiller() {
        return originatorBiller;
    }

    public void setOriginatorBiller(String originatorBiller) {
        this.originatorBiller = originatorBiller;
    }

    public String getOriginatorInstitution() {
        return originatorInstitution;
    }

    public void setOriginatorInstitution(String originatorInstitution) {
        this.originatorInstitution = originatorInstitution;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}

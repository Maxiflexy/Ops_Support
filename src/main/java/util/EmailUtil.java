package util;

import constants.AppModules;
import constants.ServiceType;
import constants.Services;
import messaging.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import persistence.DBHelper;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class EmailUtil {

    static final Logger LOG = LogManager.getLogger(EmailUtil.class);


    public static final int TRAN_LIMIT = 1000;

    public static boolean sendEmail(Email email) {

        String emailHost = System.getProperty("email-host");
        String password = System.getProperty("email-password");
        String user = System.getProperty("email-from");
        String port = System.getProperty("email-port");

        Properties props = new Properties();
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.port", port);
        props.put("mail.transport.protocol","smtp");

        Session session = Session.getDefaultInstance(props);
        session.setDebug(true);

        //Compose the message
        try {

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.getTo()));
            if (email.getCc() != null && email.getCc().length != 0) {
                message.addRecipients(Message.RecipientType.CC, email.getCc());
            }
            if (!email.isHasAttachment()) {
                if (email.getContentType() != null && !email.getContentType().isEmpty()) {
                    message.setContent(email.getContent(), email.getContentType());
                } else {
                    message.setText(email.getContent());
                }
                message.setSubject(email.getTitle());
            } else {
                Message mimeMessahe = new MimeMessage(session);
                mimeMessahe.setFrom(new InternetAddress(user));
                mimeMessahe.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getTo()));
                mimeMessahe.setSubject("Transaction Data Excel File");
                BodyPart messageBodyPart = new MimeBodyPart();
                String htmlContent = email.getContent();
                messageBodyPart.setContent(htmlContent, email.getContentType());
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                for (String s : email.getFiles()) {
                    addAttachment(multipart, s);
                }

                mimeMessahe.setContent(multipart);

                // Send the email
                Transport.send(mimeMessahe);

                System.out.println("Email sent successfully with the attached Excel file and HTML body.");

            }

            //send the message
            Transport.send(message);

            System.out.println("message sent successfully...");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void sendAsyncEmailNotification(Email email) {

        try {

            EmailNotification executeNotification = new EmailNotification(email);
            executeNotification.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void sendEmailAttachment(List<BaseBean> transactions, BaseBean requestBean) throws IOException, ExecutionException, InterruptedException {
        int transactionSize = Math.min(transactions.size(), TRAN_LIMIT);
        int totalBatches = (int)Math.ceil((transactions.size() * 1.0) / transactionSize);
        TransactionHolder holder = new TransactionHolder(new HashMap<>());
        List<String> filePaths = new ArrayList<>();
        List<Future<String>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(5, totalBatches));
        Email email = new Email();


        for (int i = 1; i <= totalBatches; i++) {
            List<BaseBean> batchedTransactions;
            if (i == totalBatches) {
                batchedTransactions = new ArrayList<>(transactions.subList((i - 1) * transactionSize, transactions.size()));
            } else {
                batchedTransactions = new ArrayList<>(transactions.subList((i - 1) * transactionSize, i * transactionSize));
            }
            holder.setTransactionWithBatch(i, batchedTransactions);
            GenerateFile fileGenerator = new GenerateFile(requestBean, holder, i);
            futures.add(executor.submit(fileGenerator));
        }

        for (Future<String> future : futures) {
            if (future.get() != null) {
                filePaths.add(future.get());
            }
        }

        email.setTo(fetchCurrentUserEmailAddress(requestBean));
        email.setHasAttachment(true);
        email.setTitle(requestBean.getString("Failed Transaction report"));
        email.setContent(NotificationService.generateFailedTransactionReport(requestBean));
        email.setContentType("text/html");
        email.setFiles(filePaths);
        EmailUtil.sendAsyncEmailNotification(email);

    }



    private static String fetchCurrentUserEmailAddress(BaseBean requestBean) {
        BaseBean userBean = getUserDetails(requestBean);
        return userBean.getString("email");
    }

    private static BaseBean getUserDetails(BaseBean requestBean) {
        BaseBean userBean = new BaseBean();
        userBean.setString("username", requestBean.getString("username"));
        DBHelper.fetchUserDetailsWithUsername(userBean);
        return userBean;
    }

    private static void addAttachment(Multipart multipart, String filename) throws MessagingException {
        DataSource source = new FileDataSource(filename);
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);
    }

    private static class TransactionHolder {
        private Map<Integer, List<BaseBean>> batchedTransactions = new HashMap<>();

        public TransactionHolder(Map<Integer, List<BaseBean>> batchedTransactions) {
            this.batchedTransactions = batchedTransactions;
        }

        public Map<Integer, List<BaseBean>> getBatchedTransactions() {
            return batchedTransactions;
        }


        public void setTransactionWithBatch(Integer batchNo, List<BaseBean> transactions) {
            batchedTransactions.put(batchNo, transactions);
        }

        public List<BaseBean> getTransaction(Integer batchNo) {
            return batchedTransactions.get(batchNo);
        }
    }

    private static class GenerateFile implements Callable<String> {

        private final int batchNo;
        private final List<BaseBean> transactions;
        private final BaseBean requestBean;

        public GenerateFile(BaseBean requestBean, TransactionHolder holder, int batchNo) {
            this.requestBean = requestBean;
            this.batchNo = batchNo;
            this.transactions = holder.getTransaction(batchNo);
        }

        @Override
        public String call() throws Exception {
            String moduleName = requestBean.getString("module_name");
            LOG.info("Module name: {}", moduleName);
            AtomicReference<String> fileName = new AtomicReference<>("");
            AppModules modules = CustomUtil.fetchApplicationModuleInfo(moduleName);
            Services services = CustomUtil.fetchServiceInfo(requestBean.getString("service_type"));
            if (modules.equals(AppModules.FAILED_TRANSACTION)) {
                fileName.set(modules.getFileNameFormat().concat("-").concat(services.getShort_code()).concat("-").concat(CustomUtil.generateDocumentId()).concat("-" + batchNo) + ".xlsx");
                generateExcelFile(transactions, fileName.get());
            } else if (modules.equals(AppModules.SETTLEMENT_REPORT)) {
                fileName.set(modules.getFileNameFormat().concat("-").concat(services.getShort_code()).concat("-").concat(requestBean.getString("document_id")).concat("-" + batchNo) + ".xlsx");
                generateSettlementReportExcelSheet(transactions, fileName.get());
            } else {
                fileName.set(modules.getFileNameFormat().concat("-").concat(services.getShort_code()).concat("-").concat(requestBean.getString("document_id")).concat("-" + batchNo) + ".xlsx");
                generateFailedTransactionExcelSheet(transactions, fileName.get());
            }
            return fileName.get();
        }

        private static void generateSettlementReportExcelSheet(List<BaseBean> transactions, String filePath) throws IOException {
            // Create an Excel workbook and sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Transaction Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"sno", "channel", "Session ID", "Transaction type", "response", "amount", "transaction time", "originator institution", "destination institution", "biller", "destination account name", "destination account number", "narration", "payment reference", "document ID", "service type", "status", "processing date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Stream over the JSON array and write to Excel
            int rowIndex = 1;
            for (BaseBean transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(transaction.getString("sno"));
                row.createCell(1).setCellValue(transaction.getString("channel"));
                row.createCell(2).setCellValue(transaction.getString("session_id"));
                row.createCell(3).setCellValue(transaction.getString("tran_type"));
                row.createCell(4).setCellValue(transaction.getString("response"));
                row.createCell(5).setCellValue(transaction.getString("amount"));
                row.createCell(6).setCellValue(transaction.getString("tran_time"));
                row.createCell(7).setCellValue(transaction.getString("org_inst"));
                row.createCell(8).setCellValue(transaction.getString("dest_inst"));
                row.createCell(9).setCellValue(transaction.getString("biller"));
                row.createCell(10).setCellValue(transaction.getString("dest_acct_name"));
                row.createCell(11).setCellValue(transaction.getString("dest_acct_no"));
                row.createCell(12).setCellValue(transaction.getString("narration"));
                row.createCell(13).setCellValue(transaction.getString("pay_ref"));
                row.createCell(14).setCellValue(transaction.getString("document_id"));
                row.createCell(15).setCellValue(transaction.getString("service_type"));
                row.createCell(16).setCellValue(transaction.getString("status"));
                row.createCell(17).setCellValue(transaction.getString("proc_date"));
            }


            // Write the Excel file to disk
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            workbook.close();
            System.out.println("Excel file '" + filePath + "' created successfully.");

        }

        private static void generateFailedTransactionExcelSheet(List<BaseBean> transactions, String filePath) throws IOException {
            // Create an Excel workbook and sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Transaction Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"SNO", "TRANSACTION REFERENCE", "TRANSACTION DATE", "TRANSACTION AMOUNT", "BATCH ID", "TRANSACTION NARRATION", "ACCOUNT NO", "CREDIT ACCOUNT NO", "VALIDATION STATUS", "RESPONSE_CODE", "RESPONSE_DATE", "RESPONSE_FLAG", "RESPONSE_DESC"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.autoSizeColumn(i);
            }

            // Stream over the JSON array and write to Excel
            int rowIndex = 1;
            for (BaseBean transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(transaction.getString("sno"));
                row.createCell(1).setCellValue(transaction.getString("tran_ref"));
                row.createCell(2).setCellValue(transaction.getString("tran_date"));
                row.createCell(3).setCellValue(transaction.getString("tran_amt"));
                row.createCell(4).setCellValue(transaction.getString("document_id"));
                row.createCell(5).setCellValue(transaction.getString("tran_narration"));
                row.createCell(6).setCellValue(transaction.getString("acct_no"));
                row.createCell(7).setCellValue(transaction.getString("credit_acct_no"));
                row.createCell(8).setCellValue(transaction.getString("isValidated"));
                row.createCell(9).setCellValue(transaction.getString("response_code"));
                row.createCell(10).setCellValue(transaction.getString("response_date"));
                row.createCell(11).setCellValue(transaction.getString("response_flag"));
                row.createCell(12).setCellValue(transaction.getString("response_desc"));

            }

//             Adjust columns to fit the content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

            // Write the Excel file to disk
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            workbook.close();
            System.out.println("Excel file '" + filePath + "' created successfully.");
        }

        private static void generateExcelFile(List<BaseBean> transactions, String filePath) throws IOException {

            // Create an Excel workbook and sheet
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Transaction Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Transaction Reference", "Transaction Date", "Transaction Amount"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Stream over the JSON array and write to Excel
            int rowIndex = 1;
            for (BaseBean transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(transaction.getString("tran_ref"));
                row.createCell(1).setCellValue(transaction.getString("tran_date"));
                row.createCell(2).setCellValue(transaction.getString("tran_amnt"));
            }

            // Adjust columns to fit the content
//        for (int i = 0; i < headers.length; i++) {
//            sheet.autoSizeColumn(i);
//        }

            // Write the Excel file to disk
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            workbook.close();
            System.out.println("Excel file '" + filePath + "' created successfully.");
        }

    }



}

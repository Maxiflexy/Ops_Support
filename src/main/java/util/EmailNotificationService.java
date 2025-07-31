package util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import constants.AppConstants;
import constants.ModuleName;
import org.apache.commons.io.IOUtils;
import persistence.ConnectionUtil;

public class EmailNotificationService {

    private static final String EMAIL_TEMPLATE_PATH = AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH;
    private static final Logger logger = Logger.getLogger(EmailNotificationService.class.getName());

    private EmailNotificationService() {}

    private static class EmailNotificationServiceHolder {
        private static final EmailNotificationService instance = new EmailNotificationService();
    }

    public static EmailNotificationService getInstance() {
        return EmailNotificationServiceHolder.instance;
    }

    public void sendUploadNotification(String documentId, String moduleName, int total, int success, int fail, String currentUser) throws Exception {
        // Step 1: Load the email template
        String emailTemplate = loadEmailTemplate();

        // Step 2: Get the initiator's email from the database
        String initiatorEmail = getInitiatorEmail(currentUser);
        if (initiatorEmail == null) {
            throw new Exception("Initiator email not found for user: " + currentUser);
        }

        // Step 3: Get the list of approval emails
        List<String> approvedEmails = getApproverEmails();
        if (approvedEmails.isEmpty()) {
            throw new Exception("No approves found with ROLE_ID = 3");
        }

        // Step 4: Prepare the email content
        String filledTemplate = fillTemplate(emailTemplate, currentUser, moduleName, documentId, total, success, fail);

        // Step 5: Send the email to the initiator with the approved emails in CC
        sendEmailWithCC(initiatorEmail, approvedEmails, filledTemplate);
    }

    public void sendUploadNotification(String documentId, ModuleName moduleName, String currentUser) throws Exception {
        String emailTemplate = loadEmailTemplate(moduleName);

        String initiatorEmail = getInitiatorEmail(currentUser);
        if (initiatorEmail == null) {
            throw new Exception("Initiator email not found for user: " + currentUser);
        }

        List<String> approvedEmails = getApproverEmails();
        if (approvedEmails.isEmpty()) {
            throw new Exception("No approves found with ROLE_ID = 3");
        }

        String filledTemplate = fillTemplate(emailTemplate, currentUser, String.valueOf(moduleName), documentId);

        sendEmailWithCC(initiatorEmail, approvedEmails, filledTemplate);
    }


    public void sendUploadNotification(String documentId, String currentUserEmail, String currentUser) throws Exception {
        String emailTemplate = loadEmailTemplateForStatus();

        if (currentUserEmail == null) {
            throw new Exception("Initiator email not found for user: " + currentUser);
        }

        String filledTemplate = fillTemplate(emailTemplate, currentUser, documentId);

        sendEmail(currentUserEmail, filledTemplate);
    }

    private void sendEmail(String recipientEmail, String filledTemplate) {

        try {
            String emailHost = System.getProperty("email-host");
            String password = System.getProperty("email-password");
            String username = System.getProperty("email-from");
            String port = System.getProperty("email-port");

            Properties props = new Properties();
            props.put("mail.smtp.host", emailHost);
            props.put("mail.smtp.port", port);
            props.put("mail.transport.protocol","smtp");

            Session session = Session.getDefaultInstance(props);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            message.setSubject("Document Upload Notification");
            message.setContent(filledTemplate, "text/html");

            Transport.send(message);

            logger.info("Email sent successfully to " + recipientEmail);

        } catch (Exception e) {
            logger.severe("Failed to send email to " + recipientEmail + ": " + e.getMessage());
        }
    }


    private void sendEmailWithCC(String recipientEmail, List<String> approvedEmails, String filledTemplate) {

        try {
            String emailHost = System.getProperty("email-host");
            String password = System.getProperty("email-password");
            String username = System.getProperty("email-from");
            String port = System.getProperty("email-port");

            Properties props = new Properties();
            props.put("mail.smtp.host", emailHost);
            props.put("mail.smtp.port", port);
            props.put("mail.transport.protocol","smtp");

            Session session = Session.getDefaultInstance(props);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            if (!approvedEmails.isEmpty()) {
                InternetAddress[] ccAddresses = new InternetAddress[approvedEmails.size()];
                for (int i = 0; i < approvedEmails.size(); i++) {
                    ccAddresses[i] = new InternetAddress(approvedEmails.get(i));
                }
                message.setRecipients(Message.RecipientType.CC, ccAddresses);
            }

            message.setSubject("File Upload Notification");
            message.setContent(filledTemplate, "text/html");

            Transport.send(message);

            logger.info("Email sent successfully to " + recipientEmail + " with CC to " + approvedEmails);

        } catch (Exception e) {
            logger.severe("Failed to send email to " + recipientEmail + ": " + e.getMessage());
        }
    }

    private String loadEmailTemplate() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Template file not found: " + AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String loadEmailTemplateForStatus() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH_STATUS_ENQUIRY)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Template file not found: " + AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private String loadEmailTemplate(ModuleName moduleName) throws Exception {
        if(moduleName == ModuleName.SETTLEMENT_REPORT)
            try (InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH_SETTLEMENT_REPORT)) {
                if (inputStream == null) {
                    throw new FileNotFoundException(
                            "Template file not found: " + AppConstants.EmailTemplates.EMAIL_TEMPLATE_PATH_SETTLEMENT_REPORT);
                }
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        return "";
    }

    private String getInitiatorEmail(String username) throws Exception {
        String query = "SELECT EMAIL FROM ESBUSER.BCK_USERS WHERE USERNAME = ?";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("EMAIL");
            }
        }
        return null;
    }

    private List<String> getApproverEmails() throws Exception {
        List<String> emails = new ArrayList<>();
        String query = "SELECT EMAIL FROM ESBUSER.BCK_USERS WHERE ROLE_ID = 3";
        try (Connection conn = ConnectionUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                emails.add(rs.getString("EMAIL"));
            }
        }
        return emails;
    }

    private String fillTemplate(String template, String initiator, String moduleName, String documentId, int total, int success, int fail) {
        return template
                .replace("${initiator}", initiator)
                .replace("${module_name}", moduleName)
                .replace("${document_id}", documentId)
                .replace("${total}", String.valueOf(total))
                .replace("${succ}", String.valueOf(success))
                .replace("${fail}", String.valueOf(fail));
    }

    private String fillTemplate(String template, String initiator, String moduleName, String documentId) {
        return template
                .replace("${initiator}", initiator)
                .replace("${module_name}", moduleName)
                .replace("${document_id}", documentId);
    }

    private String fillTemplate(String template, String currentUser, String documentId) {
        return template
                .replace("${document_id}", documentId)
                .replace("${uploader_name}", currentUser);
    }

}

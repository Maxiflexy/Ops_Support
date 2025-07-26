package util;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Email {

    public Email() {
        this.cc = new ArrayList<>();
    }

    private String from;
    private String to;
    private String title;
    private final List<InternetAddress> cc;
    private String fileName;



    private String content;
    private String contentType;
    private boolean hasAttachment;

    private List<String> files;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public InternetAddress[] getCc() {
        InternetAddress[] addresses = new InternetAddress[this.cc.size()];
        for (int i = 0; i < this.cc.size(); i++) {
            addresses[i] = this.cc.get(i);
        }
        return addresses;
    }

    public void setCc(List<String> cc) throws AddressException {
        for (String address : cc) {
            this.cc.add(new InternetAddress(address));
        }
    }

    public void setCc(String cc) throws AddressException {
        this.cc.add(new InternetAddress(cc));
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

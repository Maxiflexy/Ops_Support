package messaging;

import util.BaseBean;
import util.MessageBodyUtil;
import util.MessageBuilder;
import util.ProcessUtil;

public class NotificationService {

    public static String generateApprovedOrCanceledFundsRecoupNotification(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.FUNDS_RECOUP_REQUEST.MESSAGE_TYPE));
        msgBuilder.replace("${username}", requestBean.getString("username"));
        msgBuilder.replace("${token}", requestBean.getString("token"));
        return msgBuilder.getMainStr();
    }

    public static String generateApprovedOrCancelledEmailNotification(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.FILE_UPLOAD_APPROVED.MESSAGE_TYPE));
        msgBuilder.replace("${initiator}", requestBean.getString("initiator"));
        msgBuilder.replace("${document_id}", requestBean.getString("document_id"));
        msgBuilder.replace("${approval_status}", requestBean.getString("status").equals("true") ? "approved" : "cancelled");
        msgBuilder.replace("${module_name}", requestBean.getString("module_name"));
        msgBuilder.replace("${approval_message}", requestBean.getString("message"));
        return msgBuilder.getMainStr();
    }

    public static String generateFailedTransactionReport(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.FAILED_TRNASACTION_REQUEST.MESSAGE_TYPE));
        msgBuilder.replace("${initiator}", requestBean.getString("username"));
        msgBuilder.replace("${module_name}", requestBean.getString("module_name"));
        return msgBuilder.getMainStr();
    }


    public static String generateUserCreationNotificationTemplate(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.USER_CREATION.MESSAGE_TYPE));
        msgBuilder.replace("${firstName}", requestBean.getString("firstName"));
        msgBuilder.replace("${lastName}", requestBean.getString("lastName"));
        msgBuilder.replace("${url}", requestBean.getString("token"));
        return msgBuilder.getMainStr();
    }
}

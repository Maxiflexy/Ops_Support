package messaging;

import util.BaseBean;
import util.MessageBodyUtil;
import util.MessageBuilder;
import util.ProcessUtil;

public class TokenService {

    public static String generateEntrustRequest(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.ENTRUST.MESSAGE_TYPE));
        msgBuilder.replace("${username}", requestBean.getString("username"));
        msgBuilder.replace("${token}", requestBean.getString("token"));
        return msgBuilder.getMainStr();
    }




}

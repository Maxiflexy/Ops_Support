package messaging;

import util.BaseBean;

public interface CreateRequestMessage {

    String createRequestMessage(BaseBean requestBean, BaseBean configBean, String request);
}

package messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;

public abstract class CommonDB extends RequestValidator{

    final static private Logger LOG = LogManager.getLogger(Common.class);

    final static protected String INST_ID = "UBA";

    protected abstract String validateRequestBody(String request, BaseBean requestBean);

    protected abstract void processResponse(BaseBean requestBean);

    protected abstract String createReply(BaseBean requestBean, Boolean procErr);


    public boolean performDatabaseOperation() {
        return false;
    }

}


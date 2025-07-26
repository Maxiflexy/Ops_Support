package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExcelFileThread extends Thread {

    static final Logger LOG = LogManager.getLogger(ExcelFileThread.class);

    private final List<BaseBean> transactions;
    private final BaseBean requestBean;

    public ExcelFileThread(List<BaseBean> transactions, BaseBean requestBean) {
        this.transactions = transactions;
        this.requestBean = requestBean;
    }
    @Override
    public void run() {
        try {
            new EmailUtil().sendEmailAttachment(transactions, requestBean);
        } catch (IOException e) {
            LOG.error(e);
        } catch (ExecutionException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}

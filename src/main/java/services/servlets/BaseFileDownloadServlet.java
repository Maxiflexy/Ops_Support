package services.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.executors.DownloadExecutor;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.ResponseUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public abstract class BaseFileDownloadServlet extends HttpServlet {
    public final static Logger LOG = LogManager.getLogger(CustomBaseServlet.class);

    protected static final String APPLICATION_JSON = "application/json";
    protected static final String UTF_8 = "UTF-8";

    private String contentType=null;
    private DownloadExecutor executor=null;

    protected DownloadExecutor getExecutor() {
        return executor;
    }
    protected void setExecutor(DownloadExecutor executor) {
        this.executor = executor;
    }
    final public String getContentType() {
        return contentType;
    }
    final public String getBody(HttpServletRequest request) {


        StringBuffer jb = new StringBuffer();
        String line = null;

        contentType=request.getContentType()==null?"":request.getContentType().trim();

        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            LOG.error(e.getMessage(),e);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage(),e);
        }

        LOG.debug(jb.toString());
        return jb.toString();

    }

    public final String getAppId(HttpServletRequest request) {
        String id = "";
        try {
            id = request.getParameter("appl_id");

        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage(), ex);
        }
        return id == null ? "" : id;
    }

    final public String getInvalidFormatResponse() {
        BaseBean baseBean = new BaseBean();
        baseBean.setString("status_type", ResponseUtil.RJCT);
        baseBean.setString("status_code", ResponseUtil.INVALID_MSG_FORMAT);
        return ResponseUtil.createDefaultResponse(baseBean);

    }

    @Override
    protected abstract void  doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException;

}

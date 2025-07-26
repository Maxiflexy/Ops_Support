package services.servlets;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.executors.Executor;
import util.BaseBean;
import util.ResponseUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public abstract  class BaseServlet extends HttpServlet{
	
	public final static Logger LOG = LogManager.getLogger(BaseServlet.class);
	
    protected static final String APPLICATION_JSON = "application/json";
    protected static final String UTF_8 = "UTF-8";

	private String contentType=null;
	private Executor executor=null;
	
	protected Executor getExecutor() {
		return executor;
	}
	protected void setExecutor(Executor executor) {
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

    @Override
    protected abstract void  doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException;

	@Override
	protected abstract void doPut(HttpServletRequest req, HttpServletResponse resp) throws  IOException;
}

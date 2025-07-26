package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageBuilder {

	private StringBuilder mainStr=null;
	
	static final private Logger LOG = LogManager.getLogger(MessageBuilder.class);
	
	
	public MessageBuilder(String templ) {
		this.mainStr=new StringBuilder(templ);
	}
	
	
	final public String getMainStr() {
		if(mainStr==null) {
			return null;
		}else {
		    return mainStr.toString();
		}
		
	}	

	
	public final void replace(String searchString, String replacement) {
		
		if(mainStr==null)return;
		
		int len=0;
		try {
			len = searchString.length();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOG.error("",e);
		}	
		int pos=-1;
		try {
			pos = this.mainStr.indexOf(searchString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOG.error("",e);
			
		}
		
		while(pos>-1) {
			try {
				this.mainStr.replace(pos, pos+len, replacement);
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				pos=-1;
				LOG.error("",e);
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				pos=-1;
				LOG.error("",e);
				break;
				
			}
			try {
				pos=this.mainStr.indexOf(searchString);
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				pos=-1;
				LOG.error("",e);
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				pos=-1;
				LOG.error("",e);
				break;
				
			}
			
		}

	}
	
	
}

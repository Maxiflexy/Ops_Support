package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;

public class MessageBodyUtil implements Serializable{

	private static final long serialVersionUID = 2853574260655637080L;

	HashMap<String, String> tplMap = new HashMap<>();


	private static MessageBodyUtil instance = null;
	final private static Logger LOG = LogManager.getLogger(MessageBodyUtil.class);

	public static  MessageBodyUtil getInstance() {
        if (instance == null) {
        	synchronized(MessageBodyUtil.class) {
        	        instance = new MessageBodyUtil();
        	}
        }
        return instance;

	}

	private MessageBodyUtil() {}
	private String getTemplate(String type) {
		String template = "";
		if (tplMap.containsKey(type)) {
			template = tplMap.get(type);
		} else {
			template = loadAny(type);
			if (template != null && !template.isEmpty())
				tplMap.put(type, template);
		}
		return template != null ? template : "";

	}



	private synchronized String loadAny(String type) {

		InputStream in = null;
		BufferedReader reader = null;
		String tmp=null;

		try {

			in = MessageBodyUtil.class.getClassLoader().getResourceAsStream(new String(type+".tpl"));
			reader = new BufferedReader(new InputStreamReader(in));
	        StringBuilder out = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            out.append(line);
	        }

	        tmp= out.toString();



		} catch (RuntimeException e) {
			e.printStackTrace();

		} catch (Exception exp) {

			System.out.println("Oops! Error occurred while loading "+type);
			exp.printStackTrace();

		}finally {
	       if(reader!=null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       }
	       if(in!=null) {
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       }

		}
		return tmp;
	}

	public static String getBodyTempl(String type) {
		MessageBodyUtil util = MessageBodyUtil.getInstance();
		return util.getTemplate(type);
	}

}

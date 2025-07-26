package util;

import javax.json.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JsonUtil {

	
	public final static JsonObjectBuilder toBuilder(JsonObject jo) {
	    JsonObjectBuilder job = Json.createObjectBuilder();

	    for (Entry<String, JsonValue> entry : jo.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
	    }

	    return job;
	}
    
    public final static String toStr(JsonObject jsonObj) {
    	
    	
    	StringWriter stringWriter = new StringWriter();
    	
        JsonWriter writer = Json.createWriter(stringWriter);
        
        try {
			writer.writeObject(jsonObj);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        try {
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return stringWriter.getBuffer().toString();
        
    }

	public final static String toStr(JsonArray jsonArray) {


		StringWriter stringWriter = new StringWriter();

		JsonWriter writer = Json.createWriter(stringWriter);

		try {
			writer.writeArray(jsonArray);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return stringWriter.getBuffer().toString();

	}


    public final static JsonObject toJsonObject(String  request) {

    	JsonObject jsonObj = null;
    	JsonReader jsonReader=null;
    	//JsonObject jobj = null;
		try {
			jsonReader = Json.createReader(new StringReader(request));
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			jsonObj = jsonReader.readObject();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsonObj;


    }


	public final static JsonArray toJsonArray(String  request) {

		JsonArray jsonObj = null;
		JsonReader jsonReader=null;
		//JsonObject jobj = null;
		try {
			jsonReader = Json.createReader(new StringReader(request));
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			jsonObj = jsonReader.readArray();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsonObj;


	}
 
    
    
	public static final String getJsonObjValue(JsonObject jobj, String name) {
         
         return jobj.get(name)==null?" ":jobj.getString(name);

	}

	public static final String getJsonObjValue2(JsonObject jobj, String name) {
        JsonValue jv = jobj.get(name);
        String value = null;
        
        if (jv!=null)  {  
	        if(jv.getValueType().equals(JsonValue.ValueType.NUMBER)) {
	        	value=((JsonNumber)jv).toString();
	        }else if(jv.getValueType().equals(JsonValue.ValueType.STRING)) {
	        	value=((JsonString)jv).getString();
	        }else if(jv.getValueType().equals(JsonValue.ValueType.NULL)) {
	        	value= " ";
	        }else if(jv.getValueType().equals(JsonValue.ValueType.FALSE)) {
	        	value= "false";
	        }else if(jv.getValueType().equals(JsonValue.ValueType.TRUE)) {
	        	value= "true";
	        }else {
	        	
	        }
	    } else {
	    	value= " ";
	    }
        if(value==null || "".equals(value)) {
        	value= " ";
        }
	    return value;

	}

	public static String convertBaseBeanListToJsonString(List<BaseBean> request) {
		JsonArrayBuilder jArrBuilder = Json.createArrayBuilder();
		for (BaseBean bean : request) {
			jArrBuilder.add(convertBeanToJsonObject(bean));
		}
		return JsonUtil.toStr(jArrBuilder.build());
	}

	public static String convertBaseBeanListToJsonString(List<BaseBean> request, String[] customNodes) {
		JsonArrayBuilder jArrBuilder = Json.createArrayBuilder();
		for (BaseBean bean : request) {
			jArrBuilder.add(convertBeanToJsonObject(bean, customNodes));
		}
		return JsonUtil.toStr(jArrBuilder.build());
	}

	public static JsonObject convertBeanToJsonObject(BaseBean bean, String[] customNodes) {
		JsonObjectBuilder jobj = Json.createObjectBuilder();
		Map<String, JsonObjectBuilder> builderMap = new HashMap<>();
        for (String customNode : customNodes) {
            builderMap.put(customNode, Json.createObjectBuilder());
        }
		for (Entry<String, String> entry : bean.entrySet()) {
			String key = entry.getKey().split("-")[0];
			String keyValue = "";
			try {
				keyValue = entry.getKey().split("-")[1];
			} catch (Exception e) {

			}
			if (builderMap.containsKey(key) && !keyValue.isEmpty()) {
				builderMap.get(key).add(keyValue, entry.getValue());
				continue;
			}

			jobj.add(entry.getKey(), entry.getValue());
		}
		for (String customNode : customNodes) {
			jobj.add(customNode, builderMap.get(customNode));
		}
//		bean.forEach(jobj::add);
		return jobj.build();
	}

	public static JsonObject convertBeanToJsonObject(BaseBean bean) {
		JsonObjectBuilder jobj = Json.createObjectBuilder();
		bean.forEach(jobj::add);
		return jobj.build();
	}

	public static JsonObjectBuilder addObject(JsonObjectBuilder objectBuilder, String parameterName, String parameterValue) {
		try {
			objectBuilder.add(parameterName, parameterValue);
		} catch (Exception ex) {
//			ex.printStackTrace();
		}
		return objectBuilder;
	}
	
}

package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.*;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class JsonServiceConfig implements Serializable{


	private Map<String, BaseBean> serviceConfig = null;

	final private void setServiceConfig(Map<String, BaseBean> serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	final private Map<String, BaseBean> getServiceConfig() {
		return serviceConfig;
	}

	final static Logger LOG = LogManager.getLogger(JsonServiceConfig.class);

	private static JsonServiceConfig instance = null;

	final public static JsonServiceConfig getInstance() {

		if (instance == null) {

			synchronized(JsonServiceConfig.class) {

				if (instance == null) {
					instance= new JsonServiceConfig();
					instance.setServiceConfig(new HashMap<String,BaseBean>());
				}
			}
		}

		return instance;

	}

	private JsonServiceConfig() {}

	final public BaseBean  getProperty(String inst_id) {

		if (getServiceConfig()!=null) {

			if(getServiceConfig().isEmpty()) {
				synchronized(JsonServiceConfig.class) {
					LOG.info("serviceConfig cache is empty");
					getServiceConfig().clear();
					setServiceConfig(new HashMap<String,BaseBean>());
					loadCfg();
				}

			}

		}else {

			LOG.info("serviceConfig cache is null");
			synchronized(JsonServiceConfig.class) {
				setServiceConfig(new HashMap<String,BaseBean>());
				loadCfg();
			}

		}

		LOG.debug(inst_id);

		return getServiceConfig().get(inst_id);
	}



	final private void loadCfg() {
		try {

			LOG.info("loading startup.cfg");
			InputStream inputStream = null;


			try {

				inputStream = JsonServiceConfig.class.getClassLoader().getResourceAsStream("startup.cfg");

			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}
			JsonReader jsonReader = null;

			try {
				jsonReader = Json.createReader(inputStream);
			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}
			JsonObject jobj = null;

			try {
				jobj = jsonReader.readObject();
			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}

			JsonArray jarry = null;
			try {
				jarry = (JsonArray)jobj.getJsonArray("config");
			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}

			BaseBean baseBean = new BaseBean();

			BaseBean dataBean = null;

			baseBean.setString("cfg_home",System.getProperty("app.conf.home").replace("\\", "/"));


			loadProperty(jobj,baseBean,"fi_service_port");
			loadProperty(jobj,baseBean,"ad_base_uri");
			loadProperty(jobj,baseBean,"private_key");
			loadProperty(jobj,baseBean,"public_key");

			loadProperty(jobj,baseBean,"read_timeout");
			loadProperty(jobj,baseBean,"conn_request_timeout");
			loadProperty(jobj,baseBean,"conn_timeout");

			loadProperty(jobj,baseBean,"cls_http_clnt");
			loadProperty(jobj,baseBean,"cert_per_inst");
			loadProperty(jobj,baseBean,"sec_url");

			loadProperty(jobj,baseBean,"entrust_uri");
			loadProperty(jobj,baseBean,"entrust_path");
			loadProperty(jobj,baseBean,"entrust_port");
			loadProperty(jobj,baseBean,"entrust_name");
			loadProperty(jobj,baseBean,"entrust_word");
			loadProperty(jobj,baseBean,"cmp");

			loadProperty(jobj,baseBean,"ibm_scheme");
			loadProperty(jobj,baseBean,"ibm_host");
			loadProperty(jobj,baseBean,"ibm_port");
			loadProperty(jobj,baseBean,"ibm_path");




			for(JsonValue jval : jarry) {

				JsonObject IdObj =(JsonObject)jval;

				try {

					if (IdObj !=null) {

						dataBean = new BaseBean();

						fillBean(dataBean,baseBean);

						getConfig(IdObj, dataBean);

						getServiceConfig().put(dataBean.getString("inst_id"), dataBean);

					}else {

						LOG.info("Config object is null");

					}

				} catch (RuntimeException e) {

					LOG.error("",e);

				} catch (Exception e) {

					LOG.error("",e);

				}

			}

		} catch (Exception e) {

			LOG.error("",e);

		}


	}


	final private void loadProperty(JsonObject jsonObj,BaseBean baseBean, String prop) {


		try {

			baseBean.setString(prop, JsonUtil.getJsonObjValue2(jsonObj,prop));

		} catch (RuntimeException e) {

			LOG.error("",e);

		} catch (Exception e) {

			LOG.error("",e);

		}


	}

	final private void getConfig(JsonObject jsonObj,BaseBean baseBean) {


		for (String kname : jsonObj.keySet()) {

			try {

				baseBean.setString(kname, JsonUtil.getJsonObjValue2(jsonObj, kname));

			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}
		}

	}

	final private void fillBean(BaseBean destBean,BaseBean sourceBean) {


		for (String kname : sourceBean.keySet()) {

			try {

				destBean.setString(kname, sourceBean.getString(kname));

			} catch (RuntimeException e) {
				LOG.error("",e);
			} catch (Exception e) {
				LOG.error("",e);
			}
		}



	}

}
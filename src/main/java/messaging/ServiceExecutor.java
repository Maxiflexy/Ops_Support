package messaging;

import constants.Services;
import services.executors.Executor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class ServiceExecutor implements Executor {

    @Override
    public String execute(String request) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Services service : Services.values()) {
            JsonObjectBuilder jobj = Json.createObjectBuilder();
            jobj.add("service_name", service.getServiceDescription());
            jobj.add("service_code", service.getServiceName());
            builder.add(jobj);
        }
        String services = JsonUtil.toStr(builder.build());
        BaseBean requestBean = new BaseBean();
        requestBean.setString("data", services);
        return createReply(requestBean, false);
    }
    
    private String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (!procErr) {
            builder.add("status", ResponseUtil.SUCCESS);
            builder.add("message", "Success");
            builder.add("data", JsonUtil.toJsonArray(requestBean.getString("data")));
        }
        return builder.build().toString();
    }
}

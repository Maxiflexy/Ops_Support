package persistence;

import util.BaseBean;

import javax.json.JsonObject;

public interface FetchRequest {

    boolean fetchDocumentDetails(BaseBean requestBean, JsonObject request);
}

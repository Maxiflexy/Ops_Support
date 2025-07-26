package persistence;

import util.BaseBean;

import javax.json.JsonObject;
import java.sql.Connection;

public interface FileUploadOps extends FetchRequest {
    boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn);

}

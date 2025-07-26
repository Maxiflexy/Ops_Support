package messaging.fileUtils;

import messaging.PersistData;
import util.BaseBean;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface FileValidator extends PersistData {

    boolean validateFileBodyWithReader(InputStream body, BaseBean validationBean);

    Map<String, Object> fetchData();

    default boolean validateFile(Part requestFile, BaseBean validationBean, Reader reader) {
        try {
            return validateFileBodyWithReader(requestFile.getInputStream(), validationBean);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}

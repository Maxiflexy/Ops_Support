package messaging.fileUtils;

import util.BaseBean;

import java.io.InputStream;
import java.util.List;

public interface Reader {
    boolean readFile(InputStream inputStream, String[] inputColumns, String[] inputHeaders, BaseBean fileBean, List<BaseBean> tables);

    default boolean validateHeader(String[] inputHeaders, String[] fileHeaders) {
        for (int i = 0; i < inputHeaders.length; i++) {
            if (!inputHeaders[i].equals(fileHeaders[i]))
                return false;
        }
        return true;
    }

    RowValidator getRowValidator();

    void setRowValidator(RowValidator rowValidator);

}

package messaging.fileUtils;

import util.BaseBean;

public abstract class RowValidator {

     protected abstract   boolean validateRow(BaseBean row, BaseBean requestBean);
}

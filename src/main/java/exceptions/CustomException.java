package exceptions;

import util.BaseBean;

public class CustomException extends RuntimeException {

    private final String message;
    private final int statusCode;
    private final String responseCode;

    private final BaseBean baseBean;

    public CustomException(BaseBean exceptionBean) {
        baseBean = exceptionBean;
        this.message = exceptionBean.getString("message").isEmpty() ? "An error occurred" : exceptionBean.getString("message");
        this.statusCode = Integer.parseInt(exceptionBean.getString("statusCode").isEmpty() ? "500" : exceptionBean.getString("statusCode"));
        this.responseCode = exceptionBean.getString("responseCode");
    }

    @Override
    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseCode() {
        return responseCode.isEmpty() ? "00" : responseCode;
    }

    public BaseBean getBaseBean() {
        return this.baseBean;
    }
}
package services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import services.servlets.BaseServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ErrorServlet extends BaseServlet {

    // Method to handle GET method request.
    final static Logger LOG = LogManager.getLogger(ErrorServlet.class);
    protected static final String APPLICATION_JSON = "application/json";

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws  IOException {

        System.out.println("Executing error handler");
        Throwable throwable = (Throwable)
                request.getAttribute("javax.servlet.error.exception");
        Integer statusCode = (Integer)
                request.getAttribute("javax.servlet.error.status_code");
        String servletName = (String)
                request.getAttribute("javax.servlet.error.servlet_name");

        if (servletName == null) {
            servletName = "Unknown";
        }
        String requestUri = (String)
                request.getAttribute("javax.servlet.error.request_uri");

        if (requestUri == null) {
            requestUri = "Unknown";
        }
        response.setCharacterEncoding("UTF-8");

        response.setContentType(APPLICATION_JSON);

        PrintWriter out = response.getWriter();
        String msg = null;
        String code = null;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\" : ");
        sb.append("{");
        sb.append("\"message\"");
        sb.append(":");

        if (throwable == null && statusCode == null) {
            msg = "\"Internal server error\"";
            code = "\"501\"";
            statusCode = 501;
        } else if (statusCode != null) {
            //Integer obj= new Integer(404);
            if (statusCode.equals(404)) {
                msg = "\"Resource not available\"";
            } else if (statusCode.equals(401)) {
                msg = "\"Unauthorized\"";
            } else if (statusCode.equals(403)) {
                msg = "\"Forbidden\"";
            } else {
                if (throwable != null) {
                    msg = "\"" + throwable.getMessage() + "\"";
                } else {
                    msg = "\"Error processing request\"";
                }
            }
            code = "\"" + statusCode.toString() + "\"";
        } else {
            if (throwable != null) {
                msg = "\"" + throwable.getMessage() + "\"";
            } else {
                msg = "\"Error processing request\"";

            }
            code = "\"501\"";
        }

        sb.append(msg);
        sb.append(",");
        sb.append("\"code\"");
        sb.append(":");
        sb.append(code);
        sb.append(",");
        sb.append("\"type\"");
        sb.append(":");
        sb.append("\"RJCT\"");
        sb.append("}");
        sb.append("}");
        response.setStatus(statusCode);
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(UTF_8);
        out.print(sb);


    }

    // Method to handle POST method request.
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        doGet(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

}

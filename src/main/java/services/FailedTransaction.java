package services;

import messaging.FailedTransactionExecutor;
import messaging.UserService;
import services.servlets.BaseServlet;
import services.servlets.CustomBaseServlet;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.createDefaultResponse;

public class FailedTransaction extends CustomBaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
//            requestStr = servletRequest.getParameter("status");
            out = servletResponse.getWriter();
            String respStr = null;
            String username = (String) servletRequest.getAttribute("username");
            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "service_type", servletRequest.getParameter("service_type"));
            addObject(builder, "start_date", servletRequest.getParameter("start_date"));
            addObject(builder, "end_date", servletRequest.getParameter("end_date"));
            addObject(builder, "transaction_status", servletRequest.getParameter("transaction_status"));
            addObject(builder, "query_status", servletRequest.getParameter("query_status"));
            addObject(builder, "reversal_status", servletRequest.getParameter("reversal_status"));
            addObject(builder, "account_debit_status", servletRequest.getParameter("account_debit_status"));
            addObject(builder, "page", servletRequest.getParameter("page"));
            addObject(builder, "size", servletRequest.getParameter("size"));
            addObject(builder, "report", servletRequest.getParameter("report"));

            servletResponse.setStatus(Integer.parseInt(ResponseUtil.HTTP_OK_STATUS));
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new FailedTransactionExecutor());
            respStr = getExecutor().execute(builder.build().toString(), username, "");
            out.print(respStr);
        }catch (IllegalArgumentException e){
            assert out != null;
            out.print(createDefaultResponse("400", 400, e.getMessage()));
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            assert out != null;
            LOG.error(e.getMessage(), e);
            out.print(createDefaultResponse("500", 500, e.getMessage()));
            servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        try {
            out.flush();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}

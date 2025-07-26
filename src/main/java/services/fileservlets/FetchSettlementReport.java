package services.fileservlets;

import persistence.TransactionService;
import services.servlets.FileServlet;

import javax.json.Json;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.json.JsonObject;
import java.io.PrintWriter;

public class FetchSettlementReport extends FileServlet {

    private final TransactionService transactionService = TransactionService.getInstance();

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

        String module = servletRequest.getParameter("module");
        String sessionId = servletRequest.getParameter("session_id") != null ? servletRequest.getParameter("session_id") : "";
        String acctNo = servletRequest.getParameter("acct_no") != null ? servletRequest.getParameter("acct_no") : "";
        String startDate = servletRequest.getParameter("start_date");
        String endDate = servletRequest.getParameter("end_date");

        servletResponse.setContentType("application/json");
        servletResponse.setCharacterEncoding("UTF-8");

        if (!"settlement_report".equals(module)) {
            JsonObject errorResponse = Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "Invalid module specified")
                    .build();

            try (PrintWriter out = servletResponse.getWriter()) {
                out.print(errorResponse.toString());
                servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return;
        }

        JsonObject result;

        if(!sessionId.isEmpty())
            result = transactionService.getSettlementStatusEnquiryWithSessionId(sessionId, startDate, endDate);
        else
            result = transactionService.getSettlementStatusEnquiryWithAccountNo(acctNo, startDate, endDate);

        try (PrintWriter out = servletResponse.getWriter()) {
            out.print(result.toString());
            servletResponse.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {}

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {}
}

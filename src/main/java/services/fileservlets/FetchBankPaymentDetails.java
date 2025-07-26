package services.fileservlets;

import constants.ModuleName;
import persistence.TransactionService;
import services.servlets.FileServlet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FetchBankPaymentDetails extends FileServlet {

    private final TransactionService transactionService = TransactionService.getInstance();

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String module = servletRequest.getParameter("module");
        String batchId = servletRequest.getParameter("batch_id") != null ? servletRequest.getParameter("batch_id") : "";

        int size = 10;
        int page = 1;
        try {
            if (servletRequest.getParameter("size") != null) {
                int requestedSize = Integer.parseInt(servletRequest.getParameter("size"));
                if (requestedSize <= 10) {
                    size = requestedSize;
                }
            }
            if (servletRequest.getParameter("page") != null) {
                page = Integer.parseInt(servletRequest.getParameter("page"));
            }
        } catch (NumberFormatException e) {
            // Handle invalid size or page values gracefully
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = Json.createObjectBuilder()
                    .add("status", "99")
                    .add("message", "Invalid size or page parameter")
                    .build();
            try (PrintWriter out = servletResponse.getWriter()) {
                out.print(errorResponse.toString());
            }
            return;
        }

        servletResponse.setContentType("application/json");
        servletResponse.setCharacterEncoding("UTF-8");

        if (!module.equalsIgnoreCase(String.valueOf(ModuleName.INTER_BANK_BULK_PAYMENT_DETAILS)) &&
                !module.equalsIgnoreCase(String.valueOf(ModuleName.INTRA_BANK_BULK_PAYMENT_DETAILS))) {
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

        JsonObject result = transactionService.getPaymentDetailsByBatchId(module, batchId, size, page);

        try (PrintWriter out = servletResponse.getWriter()) {
            out.print(result.toString());
            servletResponse.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}


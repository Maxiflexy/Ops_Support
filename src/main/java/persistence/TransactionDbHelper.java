package persistence;

import constants.AppModules;
import constants.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDbHelper {

    final static Logger LOG = LogManager.getLogger(TransactionDbHelper.class);

    public static boolean fetchFailedTransactions(BaseBean requestBean, boolean sendNotification) {

        Services service = CustomUtil.fetchServiceInfo(requestBean.getString("service_type"));
        if (requestBean.getString("size").equals("")) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").equals("")) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));

        String query = "SELECT "
                .concat(service.getTranDate())
                .concat(", ")
                .concat(service.getTranAmount())
                .concat(", ")
                .concat(service.getTranRef())
                .concat(" FROM ")
                .concat(service.getServiceTable())
                .concat(" where ")
                .concat(service.tranDate)
                .concat(" between to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");


        query = getTransactionFilterQuery(requestBean, service, query);

        if (!sendNotification) {
            query = query.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");
        }
        LOG.info("Query is: {}", query);
        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        List<BaseBean> transactions = new ArrayList<>();
        try {

            int kk = 0;

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));

            try {
                int sizeCount = 0;
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sizeCount++;
                    BaseBean resultBean = new BaseBean();
                    resultBean.setString("tran_ref", rs.getString(service.tranRef));
                    resultBean.setString("tran_amnt", rs.getString(service.tranAmount));
                    resultBean.setString("tran_date", rs.getString(service.tranDate));
                    transactions.add(resultBean);
                }
                requestBean.setString("size_count", String.valueOf(sizeCount));
                success = true;
                requestBean.setString("data", JsonUtil.convertBaseBeanListToJsonString(transactions));
                fetchTransactionCount(requestBean);
                if (sendNotification) {
                    ExcelFileThread fileThread = new ExcelFileThread(transactions, requestBean);
                    fileThread.start();
                }
            } catch (SQLException e) {

                LOG.error("", e);

            }


        } catch (Exception e) {

            LOG.error("", e);

        } finally {

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("", e);
                }
                ps = null;
            }
            ConnectionUtil.closeConnection(cnn);
        }


        return success;
    }

    public static boolean fetchTransactionCount(BaseBean requestBean) {
        Services service = CustomUtil.fetchServiceInfo(requestBean.getString("service_type"));

        String query = "SELECT count(*) as tran_count"
                .concat(" FROM ")
                .concat(service.getServiceTable())
                .concat(" where ")
                .concat(service.tranDate)
                .concat(" between to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS') and to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");

        query = getTransactionFilterQuery(requestBean, service, query);

        LOG.info("Query is: {}", query);
        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;
        try {

            int kk = 0;

            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("start_date"));
            ps.setString(++kk, requestBean.getString("end_date"));


            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total", rs.getString("tran_count"));
                    success = true;

                }

            } catch (Exception e) {

                LOG.error("", e);

            }


        } catch (SQLException e) {

            LOG.error("", e);

        } finally {

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    LOG.error("", e);
                }
                ps = null;
            }
            ConnectionUtil.closeConnection(cnn);
        }

        return success;
    }

    private static String getTransactionFilterQuery(BaseBean requestBean, Services service, String query) {
        if (requestBean.containsKey("transaction_status")) {
            if (requestBean.getString("transaction_status").equalsIgnoreCase("success")) {
                query = query.concat(" AND ")
                        .concat(service.getSuccessTransactionStatusQuery());
            } else if (requestBean.getString("transaction_status").equalsIgnoreCase("failed")) {
                query = query.concat(" AND ")
                        .concat(service.getFailedTransactionStatusQuery());
            } else if (requestBean.getString("transaction_status").equalsIgnoreCase("pending")) {
                query = query.concat(" AND ")
                        .concat(service.getPendingTransactionStatusQuery());
            } else {
                throw new IllegalArgumentException("Invalid transaction status: " + requestBean.getString("transaction_status"));
            }
        }

        if (requestBean.containsKey("account_debit_status")) {
            if (requestBean.getString("account_debit_status").equalsIgnoreCase("success") && requestBean.getString("transaction_status").equalsIgnoreCase("failed") && !service.getDebitQueryWithFailedTxn().isEmpty()) {
                query = query.concat(" AND ")
                        .concat(service.getDebitQueryWithFailedTxn());
            } else if (requestBean.getString("account_debit_status").equalsIgnoreCase("success") && !service.getAccountDebitSuccessQuery().isEmpty()) {
                query = query.concat(" AND ")
                        .concat(service.getAccountDebitSuccessQuery());
            }else if (requestBean.getString("account_debit_status").equalsIgnoreCase("failed") && !service.getAccountDebitFailureQuery().isEmpty()) {
                query = query.concat(" AND ")
                        .concat(service.getAccountDebitFailureQuery());
            } else {
                throw new IllegalArgumentException("Invalid Account debit status: " + requestBean.getString("account_debit_status"));
            }
        }

        if (requestBean.containsKey("reversal_status")) {
            if (requestBean.getString("reversal_status").equalsIgnoreCase("success") && !service.getReversalSuccessQuery().isEmpty()) {
                query = query.concat(" AND ")
                        .concat(service.getReversalSuccessQuery());
            } else if (requestBean.getString("reversal_status").equalsIgnoreCase("failed") && !service.getReversalFailureQuery().isEmpty()) {
                query = query.concat(" AND ")
                        .concat(service.getReversalFailureQuery());
            } else {
                throw new IllegalArgumentException("Invalid reversal status: " + requestBean.getString("reversal_status"));
            }
        }
        return query;
    }


}

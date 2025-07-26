package persistence;

import constants.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.*;

import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.SETTLEMENT_REPORT;


public class SettlementReportDbHelper implements FileUploadOps {

    final static Logger LOG = LogManager.getLogger(SettlementReportDbHelper.class);

    @Override
    public boolean fetchDocumentDetails(BaseBean requestBean, JsonObject request) {
        boolean sendNotification = requestBean.containsKey("report") && requestBean.getString("report").equalsIgnoreCase("true");

        if (requestBean.getString("size").equals("")) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").equals("")) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));

        String query = "SELECT * FROM ".concat(SETTLEMENT_REPORT).concat(" f where f.document_id = ? ");
        if (requestBean.containsKey("tran_status")) {
            query = query.concat(" and f.tran_status = ? ");
        }
        if (!sendNotification && requestBean.containsKey("paginated") && requestBean.getString("paginated").equals("true")) {
            query = query.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");
        }

        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", query);

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("document_id"));
            if (requestBean.containsKey("tran_status")) {
                ps.setString(++kk, requestBean.getString("tran_status"));
            }
            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> contents = new ArrayList<>();
                while (rs.next()) {

                    BaseBean documentBean = new BaseBean();
                    try {
                        documentBean.setString("sno", rs.getString("s_n"));
                        documentBean.setString("channel", rs.getString("channel"));
                        documentBean.setString("session_id", rs.getString("session_id"));
                        documentBean.setString("tran_type", rs.getString("transaction_type"));
                        documentBean.setString("response", rs.getString("response"));
                        documentBean.setString("amount", rs.getString("amount"));
                        documentBean.setString("tran_time", rs.getString("transaction_time"));
                        documentBean.setString("org_inst", rs.getString("originator_institution"));
                        documentBean.setString("dest_inst", rs.getString("destination_institution"));
                        documentBean.setString("biller", rs.getString("originator_biller"));
                        documentBean.setString("dest_acct_name", rs.getString("destination_account_name"));
                        documentBean.setString("dest_acct_no", rs.getString("destination_account_no"));
                        documentBean.setString("narration", rs.getString("narration"));
                        documentBean.setString("pay_ref", rs.getString("payment_reference"));
                        documentBean.setString("document_id", rs.getString("document_id"));
                        documentBean.setString("service_type", rs.getString("servicetype"));
                        if (requestBean.getString("action").equals("approved")) {
                            documentBean.setString("status", rs.getString("tran_status"));
                            documentBean.setString("proc_date", rs.getString("resp_date"));
                        }
                        contents.add(documentBean);
                    } catch (Exception ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(contents));
                fetchTotalRecordCount(requestBean, request);
                if (sendNotification) {
                    ExcelFileThread fileThread = new ExcelFileThread(contents, requestBean);
                    fileThread.start();
                }
                success = true;
            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();
            }

        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
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

    private void fetchTotalRecordCount(BaseBean requestBean, JsonObject request) {
        String fetchByStatusQuery = "SELECT count(*) as file_count FROM "
                .concat(SETTLEMENT_REPORT)
                .concat(" f where f.document_id = ? ");


        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching  files count {}", fetchByStatusQuery);

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchByStatusQuery);
            ps.setString(++kk, requestBean.getString("document_id"));
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requestBean.setString("total_count", rs.getString("file_count"));
                }
                success = true;
            } catch (Exception e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();

            }


        } catch (SQLException e) {
            requestBean.setString("message", e.getMessage());
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
    }

    @Override
    public boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn) {
        return false;
    }

    public static BaseBean fetchTransactionDetails(BaseBean requestBean) {

        Services services = CustomUtil.fetchServiceInfo(requestBean.getString("service_type"));

        String query = "SELECT f.*, g.".concat(services.getAccountNumber()).concat(" as accountNo FROM ")
                .concat(SETTLEMENT_REPORT)
                .concat(" f, ")
                .concat(services.serviceTable)
                .concat(" g")
                .concat(" where f.s_n = ? and f.document_id = ? and g.")
                .concat(services.tranRef)
                .concat(" = f.session_id");

        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching transaction {}", query);
        BaseBean documentBean = new BaseBean();

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("sno"));
            ps.setString(++kk, requestBean.getString("document_id"));

            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {

                    documentBean.setString("sno", rs.getString("s_n"));
                    documentBean.setString("channel", rs.getString("channel"));
                    documentBean.setString("session_id", rs.getString("session_id"));
                    documentBean.setString("tran_type", rs.getString("transaction_type"));
                    documentBean.setString("response", rs.getString("response"));
                    documentBean.setString("amount", rs.getString("amount"));
                    documentBean.setString("tran_time", rs.getString("transaction_time"));
                    documentBean.setString("org_inst", rs.getString("originator_institution"));
                    documentBean.setString("dest_inst", rs.getString("destination_institution"));
                    documentBean.setString("biller", rs.getString("originator_biller"));
                    documentBean.setString("dest_acct_name", rs.getString("destination_account_name"));
                    documentBean.setString("dest_acct_no", rs.getString("destination_account_no"));
                    documentBean.setString("narration", rs.getString("narration"));
                    documentBean.setString("pay_ref", rs.getString("payment_reference"));
                    documentBean.setString("document_id", rs.getString("document_id"));
                    documentBean.setString("service_type", rs.getString("servicetype"));
                    documentBean.setString("status", rs.getString("tran_status"));
                    documentBean.setString("proc_date", rs.getString("resp_date"));
                    documentBean.setString("accountNo", rs.getString("accountNo"));
                }

            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
                e.printStackTrace();
            }

        } catch (Exception e) {
            requestBean.setString("message", e.getMessage());
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
        return documentBean;

    }
}

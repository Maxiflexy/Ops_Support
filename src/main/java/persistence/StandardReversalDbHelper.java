package persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.EmailUtil;
import util.ExcelFileThread;
import util.JsonUtil;

import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static constants.AppConstants.DbTables.RESPONSE_CODE_CONFIG;
import static constants.AppConstants.DbTables.STANDARD_REVERSAL;

public class StandardReversalDbHelper implements FileUploadOps {

    final static Logger LOG = LogManager.getLogger(StandardReversalDbHelper.class);

    @Override
    public boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn) {
        return false;
    }

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

        String query = "SELECT * FROM "
                .concat(STANDARD_REVERSAL)
                .concat(" f LEFT JOIN ")
                .concat(RESPONSE_CODE_CONFIG)
                .concat( " r on f.C24RESPCODE = r.err_code where f.document_id = ? AND f.tran_amt is not null AND f.validation_status is not null");

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
                        documentBean.setString("sno", rs.getString("serial_no"));
                        documentBean.setString("tran_ref", rs.getString("tran_ref"));
                        documentBean.setString("tran_date", rs.getString("tran_date_time"));
                        documentBean.setString("tran_amt", rs.getString("tran_amt"));
                        documentBean.setString("document_id", rs.getString("document_id"));
                        documentBean.setString("tran_narration", rs.getString("narration"));
                        documentBean.setString("acct_no", rs.getString("debit_acct_no"));
                        documentBean.setString("credit_acct_no", rs.getString("credit_acct_no"));
                        documentBean.setString("isValidated", String.valueOf(!rs.getString("validation_status").equals("N")));
                        if (requestBean.getString("action").equals("approved")) {
                            documentBean.setString("response_code", rs.getString("c24respcode"));
                            documentBean.setString("response_date", rs.getString("c24respdate"));
                            documentBean.setString("response_flag", rs.getString("c24respflag"));
                            documentBean.setString("response_desc", rs.getString("err_desc"));
                        }
                        contents.add(documentBean);
                    } catch (Exception e) {
                        LOG.error(e);
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
                .concat(STANDARD_REVERSAL)
                .concat(" f where f.document_id = ? AND f.tran_amt is not null AND f.validation_status is not null");


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
}

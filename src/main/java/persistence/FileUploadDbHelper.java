package persistence;

import exceptions.CustomException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static constants.AppConstants.DbTables.FILE_REQUEST_TABLE;
import static util.CustomUtil.fetchApplicationModuleInfo;

public class FileUploadDbHelper {
    final static Logger LOG = LogManager.getLogger(FileUploadDbHelper.class);

    public static boolean fetchUploadedFiles(BaseBean requestBean, JsonObject request) {

        if (requestBean.getString("size").equals("")) {
            requestBean.setString("size", "10");
        }
        if (requestBean.getString("page").equals("")) {
            requestBean.setString("page", "1");
        }

        String limit = requestBean.getString("size");
        String offset = String.valueOf((Integer.parseInt(requestBean.getString("page")) - 1) * Integer.parseInt(limit));


        String fetchByStatusQuery = "SELECT * FROM "
                .concat(FILE_REQUEST_TABLE)
                .concat(" f where f.status ");

        fetchByStatusQuery = applyQueryFilter(requestBean, fetchByStatusQuery);
        fetchByStatusQuery = fetchByStatusQuery.concat(" order by f.upload_date DESC");
        fetchByStatusQuery = fetchByStatusQuery.concat(" OFFSET ").concat(offset).concat(" ROWS FETCH NEXT ").concat(limit).concat(" ROWS ONLY");


        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", fetchByStatusQuery);

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchByStatusQuery);
            if (requestBean.containsKey("module_name")) {
                ps.setString(++kk, requestBean.getString("module_name").toUpperCase());
            }
            if (requestBean.containsKey("document_id")) {
                ps.setString(++kk, requestBean.getString("document_id"));
            }
            if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").isEmpty()) {
                ps.setString(++kk, requestBean.getString("service_type"));
            }
            if (requestBean.containsKey("start_date")) {
                ps.setString(++kk, requestBean.getString("start_date"));
            }

            if (requestBean.containsKey("end_date")) {
                ps.setString(++kk, requestBean.getString("end_date"));
            }

            try {
                ResultSet rs = ps.executeQuery();
                List<BaseBean> files = new ArrayList<>();
                while (rs.next()) {
                    BaseBean fileBean = new BaseBean();
                    fileBean.setString("document_name", rs.getString("module_name"));
                    fileBean.setString("document_type", rs.getString("document_type"));
                    fileBean.setString("document_id", rs.getString("document_id"));
                    fileBean.setString("module_name", rs.getString("module_name"));
                    fileBean.setString("service_type", rs.getString("service_type"));
                    fileBean.setString("upload_date", rs.getString("upload_date"));
                    fileBean.setString("uploaded_by", rs.getString("uploaded_by"));
                    fileBean.setString("record_count-total", rs.getString("TOTAL_UNAPPROVED_TRAN"));
                    fileBean.setString("record_count-success", rs.getString("TOTAL_UNAPPROVED_VALIDATED"));
                    fileBean.setString("record_count-failure", rs.getString("TOTAL_UNAPPROVED_FAILED"));
                    fileBean.setString("isValidated", String.valueOf(!rs.getString("validation_status").equals("N")));
                    if (requestBean.getString("action").equals("cancelled") || requestBean.getString("action").equals("approved")) {
                        fileBean.setString("approved_by", rs.getString("approved_by"));
                        fileBean.setString("approval_date", rs.getString("approval_date"));
                        fileBean.setString("approval_comment", rs.getString("approval_comment"));
                    }
                    files.add(fileBean);
                }
                requestBean.setString("jsonBean", JsonUtil.convertBaseBeanListToJsonString(files, new String[]{"record_count"}));
                fetchTotalFileUploadCount(requestBean, request);
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
        return success;
    }

    private static String applyQueryFilter(BaseBean requestBean, String fetchByStatusQuery) {
        if (requestBean.getString("action").equals("unapproved")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" is null");
        } else if (requestBean.getString("action").equals("cancelled")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" = 'N'");
        } else if (requestBean.getString("action").equals("approved")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" = 'Y'");
        } else {
            throw new IllegalArgumentException("Unsupported action: " + requestBean.getString("action"));
        }
        if (requestBean.containsKey("module_name")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.module_name = ?");
        }
        if (requestBean.containsKey("document_id")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.document_id = ?");
        }

        if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").isEmpty()) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.service_type = ?");
        }

        if (requestBean.containsKey("start_date")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.upload_date >= to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        }

        if (requestBean.containsKey("end_date")) {
            fetchByStatusQuery = fetchByStatusQuery.concat(" and f.upload_date <= to_date(?, 'YYYY-MM-DD\"T\"HH24:MI:SS')");
        }
        return fetchByStatusQuery;
    }

    public static boolean fetchTotalFileUploadCount(BaseBean requestBean, JsonObject request) {
        String fetchByStatusQuery = "SELECT count(*) as file_count FROM "
                .concat(FILE_REQUEST_TABLE)
                .concat(" f where f.status ");

        fetchByStatusQuery = applyQueryFilter(requestBean, fetchByStatusQuery);


        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;


        LOG.info("Fetching uploaded files {}", fetchByStatusQuery);

        try {
            int kk = 0;
            ps = cnn.prepareStatement(fetchByStatusQuery);
            if (requestBean.containsKey("module_name")) {
                ps.setString(++kk, requestBean.getString("module_name").toUpperCase());
            }
            if (requestBean.containsKey("document_id")) {
                ps.setString(++kk, requestBean.getString("document_id"));
            }
            if (requestBean.containsKey("service_type") && !requestBean.getString("service_type").isEmpty()) {
                ps.setString(++kk, requestBean.getString("service_type"));
            }
            if (requestBean.containsKey("start_date")) {
                ps.setString(++kk, requestBean.getString("start_date"));
            }

            if (requestBean.containsKey("end_date")) {
                ps.setString(++kk, requestBean.getString("end_date"));
            }
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

        }
        return success;
    }

    public static boolean approveFileUpload(BaseBean requestBean, JsonObject request, String moduleName, String action) {
//        update esbuser.file_upload_mc c set c.status = 'Y', c.approved_by = 'michael',c.approval_date = sysdate, c.approval_comment = 'Done' where c.document_id='a366918c-cb00-4085-96d7-491f74096258' and c.status is null;
        String query = "update "
                .concat(FILE_REQUEST_TABLE)
                .concat(" c set c.status = ?, c.approved_by = ?, c.approval_date = sysdate, c.approval_comment = ? where c.document_id=? and c.module_name = ? and c.status is null");
        PreparedStatement ps = null;
        Connection cnn = ConnectionUtil.getConnection();
        boolean success = false;
        try {
            cnn.setAutoCommit(false);
            boolean approvalStatus = Boolean.parseBoolean(requestBean.getString("status"));
            LOG.info("Executing file approval query: {}", query);
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, approvalStatus ? "Y" : "N");
            ps.setString(++kk, requestBean.getString("email"));
            ps.setString(++kk, requestBean.getString("message"));
            ps.setString(++kk, requestBean.getString("document_id"));
            ps.setString(++kk, request.getString("module_name"));

            try {

                if (ps.executeUpdate() > 0) {
                    LOG.info("writing to file upload maker checker: ");
                    success = true;
                    cnn.commit();
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to file upload maker checker");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }

            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

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

    public static boolean approveUpload(BaseBean requestBean, JsonObject requestObject) {
        boolean success = false;
        JsonObject request = getFileRequest(requestBean, requestObject);
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Cannot find uploaded file with id: " + requestBean.getString("document_id"));
        }
        String moduleName = request.getString("module_name");
        String action = request.getString("service_type");

        requestBean.setString("initiator", request.getString("uploaded_by"));
        requestBean.setString("module_name", moduleName);
        success = approveFileUpload(requestBean, request, moduleName, action);
        return success;

    }

    private static JsonObject getFileRequest(BaseBean requestBean, JsonObject requestObject) {
        BaseBean fileBean = new BaseBean();
        fileBean.setString("action", requestBean.getString("action"));
        fileBean.setString("document_id", requestBean.getString("document_id"));
        boolean fetchFile = fetchUploadedFiles(fileBean, requestObject);

        if (!fetchFile || fileBean.getString("jsonBean").isEmpty() || JsonUtil.toJsonArray(fileBean.getString("jsonBean")).isEmpty()) {
            requestBean.setString("message","Cannot find uploaded file with id: " + requestBean.getString("document_id"));
            throw new IllegalArgumentException("Cannot find uploaded file with id: " + requestBean.getString("document_id"));
        }

        return JsonUtil.toJsonArray(fileBean.getString("jsonBean")).isEmpty() ? Json.createObjectBuilder().build() : (JsonObject) JsonUtil.toJsonArray(fileBean.getString("jsonBean")).get(0);
    }

    private static FileUploadOps getApprovedUploadedFileHelper(BaseBean requestBean, String moduleName) {
        FileUploadOps fileUploadHelper;
        try {
            switch (fetchApplicationModuleInfo(moduleName.toUpperCase())) {
                case FUNDS_RECOUP:
                    fileUploadHelper = new FundsRecoupDbHelper();
                    break;
                case EXCEPTIONAL_REVERSAL:
                    fileUploadHelper = new ExceptionalReversalDbHelper();
                    break;
                case UNSETTLED_TRANSACTION:
                    fileUploadHelper = new UnsettledTransactionDbHelper();
                    break;
                case STANDARD_REVERSAL:
                    fileUploadHelper = new StandardReversalDbHelper();
                    break;
                case SETTLEMENT_REPORT:
                    fileUploadHelper = new SettlementReportDbHelper();
                    break;
                default:
                    requestBean.setString("message", "Invalid module name");
                    throw new CustomException(requestBean);
            }
        } catch (IllegalArgumentException e) {
            requestBean.setString("message", e.getMessage());
            throw new CustomException(requestBean);
        }
        return fileUploadHelper;
    }

    public static boolean fetchFileContent(BaseBean requestBean, JsonObject requestObject) {
        String moduleName;
        if (!requestBean.containsKey("module_name")) {
            JsonObject fileDet = getFileRequest(requestBean, requestObject);
            moduleName = fileDet.getString("module_name");
            requestBean.setString("module_name", moduleName);
            requestBean.setString("service_type", fileDet.getString("service_type"));
        } else {
            moduleName = requestBean.getString("module_name");
        }

        FileUploadOps fetchHelper = getApprovedUploadedFileHelper(requestBean, moduleName);
        return fetchHelper.fetchDocumentDetails(requestBean, requestObject);
    }

    public static boolean saveNewRecord(Map<String, Object> tables, BaseBean requestBean) {
        Connection cnn = ConnectionUtil.getConnection();
        boolean success = false;
        PreparedStatement ps = null;

        try {
            cnn.setAutoCommit(false);

            for (String key : tables.keySet()) {
                String query = "INSERT INTO "
                        .concat(FILE_REQUEST_TABLE)
                        .concat(" (document_name,document_type,document_id,upload_date,uploaded_by,module_name, key_name, key_value, action) VALUES (?, ?,?,sysdate,?,?,?,?,?)");
                LOG.info("Executing file upload query: {}", query);
                BaseBean fileBean = (BaseBean) ((Map<String, Object>) tables.get(key)).get("file");
                List<BaseBean> tableBeans = (List<BaseBean>) ((Map<String, Object>) tables.get(key)).get("table");
                int kk = 0;
                ps = cnn.prepareStatement(query);
                ps.setString(++kk, requestBean.getString("fileName"));
                ps.setString(++kk, requestBean.getString("extension"));
                ps.setString(++kk, requestBean.getString("document-id"));
                ps.setString(++kk, requestBean.getString("email"));
                ps.setString(++kk, requestBean.getString("module_name").toLowerCase());
                ps.setString(++kk, requestBean.getString("key_name"));
                ps.setString(++kk, requestBean.getString("key_value"));
                ps.setString(++kk, requestBean.getString("action"));
                requestBean.setString("document-id", fileBean.getString("document-id"));

                try {

                    if (ps.executeUpdate() > 0) {
                        LOG.info("writing to file details maker checker: {}", requestBean.getString("module_name"));
                        FileUploadOps ops = getApprovedUploadedFileHelper(requestBean, requestBean.getString("module_name"));
                        for (BaseBean row : tableBeans) {
                            success = ops.createModuleRequest(requestBean, row, cnn);
                            if (!success) {
                                cnn.rollback();
                                break;
                            }
                        }
                    } else {
                        //check if app has been verified
                        LOG.info("unable to write to fee config maker checker");
                        cnn.rollback();
                        LOG.info("done with rollback");
                        break;

                    }

                } catch (Exception e) {
                    requestBean.setString("message", e.getMessage());
                    LOG.error("", e);
                    cnn.rollback();

                }
            }


            LOG.info("Saving transaction in db {}", success);

            if (success) {
                cnn.commit();
            } else {
                cnn.rollback();
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
        return success;
    }

    public static boolean saveESBResponse(BaseBean requestBean) {

        String query = "UPDATE "
                .concat(FILE_REQUEST_TABLE)
                .concat(" f set f.resp_flg = ?, f.resp_status = ?, f.resp_date = sysdate, f.appr_count = appr_count + 1 where f.document_id = ?");

        PreparedStatement ps = null;
        Connection cnn = ConnectionUtil.getConnection();
        boolean success = false;
        try {
            cnn.setAutoCommit(false);
            LOG.info("Executing esb response update query: {}", query);
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("resp_flag"));
            ps.setString(++kk, requestBean.getString("ibm_status"));
            ps.setString(++kk, requestBean.getString("ibm_document_id"));

            try {

                if (ps.executeUpdate() > 0) {
                    cnn.commit();
                    success = true;
                } else {
                    //check if app has been verified
                    LOG.info("unable to write to file upload maker checker");
                    cnn.rollback();
                    LOG.info("done with rollback");
                }

            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);

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

    public static boolean fetchUploadSummaryWithDocumentId(BaseBean requestBean) {
        String query = "SELECT module_name, total_unapproved_validated, document_id, service_type from "
                .concat(FILE_REQUEST_TABLE)
                .concat(" where document_id = ?");

        LOG.info("Fetching uploaded files {}", query);
        boolean success = false;
        Connection cnn = ConnectionUtil.getConnection();
        PreparedStatement ps = null;

        try {
            int kk = 0;
            ps = cnn.prepareStatement(query);
            ps.setString(++kk, requestBean.getString("document_id"));
            BaseBean summaryBean = new BaseBean();
            try {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    summaryBean.put("moduleName", rs.getString("module_name"), false);
                    summaryBean.put("count", rs.getString("total_unapproved_validated"), false);
                    summaryBean.put("documentId", rs.getString("document_id"), false);
                    summaryBean.put("serviceType", rs.getString("service_type"), false);

                    requestBean.setString("data", String.valueOf(JsonUtil.convertBeanToJsonObject(summaryBean)));
                    success = true;
                }
            } catch (SQLException e) {
                requestBean.setString("message", e.getMessage());
                LOG.error("", e);
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
}

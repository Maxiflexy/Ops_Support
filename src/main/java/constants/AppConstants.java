package constants;

public class AppConstants {

    public static class DbTables {
        public static final String TABLE_SPACE = "esbuser";
        public static final String PERMISSION_TABLE = TABLE_SPACE.concat(".bck_permissions");
        public static final String TOKEN = TABLE_SPACE.concat(".token");
        public static final String USERS_TABLE = TABLE_SPACE.concat(".bck_users");
        public static final String ROLE_PERMISSION = TABLE_SPACE.concat(".bck_role_permission");
        public static final String PERMISSION_RESOURCE = TABLE_SPACE.concat(".switch_permission_resources");
        public static final String SWITCH_RESOURCE = TABLE_SPACE.concat(".switch_resources");
        public static final String ROLES = TABLE_SPACE.concat(".bck_roles");
        public static final String SUBSCRIPTIONS = TABLE_SPACE.concat(".bck_subscriptions");

        public static final String PERF_STAT_TABLE = TABLE_SPACE.concat(".perf_stat");
        public static final String SWITCH_list_TABLE = TABLE_SPACE.concat(".switch_list");
        public static final String UNAPPROVED_SWITCH_LIST = TABLE_SPACE.concat(".switch_list_mc");

        public static final String APPROVED_ENDPOINTS = TABLE_SPACE.concat(".endpoint");
        public static final String UNAPPROVED_ENDPOINT = TABLE_SPACE.concat(".endpoint_list_mc");
        public static final String MESSAGE_TABLE = TABLE_SPACE.concat(".BCK_MESSAGES");

        public static final String UP_INFLOW_TRANSACTION = TABLE_SPACE.concat(".ibt_in_flw ");
        public static final String UP_OUTFLOW_TRANSACTION = TABLE_SPACE.concat(".ibt_out_flw");
        //public static final String NIP_OUTFLOW_TRANSACTION = TABLE_SPACE.concat(".nip_out_flw_2_v3");
        public static final String NIP_OUTFLOW_TRANSACTION = TABLE_SPACE.concat(".ibt_out_flw");
        public static final String NIP_INFLOW_TRANSACTION = TABLE_SPACE.concat(".nip_in_flw_v2");

        public static final String SERVICE_ID_TABLE = TABLE_SPACE.concat(".nip_svc_usr ");
        public static final String NXT_ACTION_TABLE = TABLE_SPACE.concat(".nxt_action ");

        public static final String FILE_REQUEST_TABLE = TABLE_SPACE.concat(".SUPPORT_FILE_UPLOAD_MC");
        public static final String FEE_CONFIG_MC = TABLE_SPACE.concat(".FEES_CONFIG_MC");
        public static final String FEE_CONFIG = TABLE_SPACE.concat(".FEES_CONFIG");
        public static final String CONTRA_ACCOUNT_TABLE = TABLE_SPACE.concat(".CONTRA_ACC_CONFIG");
        public static final String CONTRA_ACCOUNT_MC_TABLE = TABLE_SPACE.concat(".CONTRA_ACC_CONFIG_MC");

        public static final String INST_ROUTE_TABLE = TABLE_SPACE.concat(".IBT_FSP_FINST_MAP");
        public static final String INST_ROUTE_TABLE_MC = TABLE_SPACE.concat(".IBT_FSP_FINST_MAP_MC");
        public static final String RESPONSE_CODE = TABLE_SPACE.concat(".NIP_OUTFLW2_SERV_CTRL_V3");
        public static final String RESPONSE_CODE_MC = TABLE_SPACE.concat(".NEXT_ACTION_MC");
        public static final String INST_LIST_TABLE = TABLE_SPACE.concat(".IBT_FINST_LIST");
        public static final String INST_LIST_TABLE_MC = TABLE_SPACE.concat(".IBT_FINST_LIST_MC");
        public static final String SERVICEID_TABLE = TABLE_SPACE.concat(".service_id");
        public static final String CHANNEL_TABLE = TABLE_SPACE.concat(".channel_apps");
        public static final String VA_SERVICE_PROVIDERS_TABLE = TABLE_SPACE.concat(".va_service_providers");
        public static final String UNAPPROVED_SERVICE_PROVIDERS = TABLE_SPACE.concat(".va_service_providers_mc");
        public static final String VIRTUAL_ACCOUNT_CONFIG = TABLE_SPACE.concat(".virtual_account_config");
        public static final String VIRTUAL_ACCOUNT_CONFIG_MC = TABLE_SPACE.concat(".virtual_account_config_mc");
        public static final String AIRTIME_TABLE = TABLE_SPACE.concat(".CHL_AIRTIMETOPUP_3");

        public static final String FILE_METADATA = TABLE_SPACE.concat(".SUPPORT_FILE_UPLOAD_MC_V2");
        public static final String STATUS_ENQUIRY_RECORD = TABLE_SPACE.concat(".STATUS_ENQUIRY_RECORD");

        public static final String STANDARD_REVERSAL = TABLE_SPACE.concat(".STANDARD_REVERSAL");
        public static final String EXCEPTIONAL_REVERSAL = TABLE_SPACE.concat(".EXCEPTIONAL_REVERSAL");
        public static final String FUNDS_RECOUP = TABLE_SPACE.concat(".FUNDS_RECOUP");
        public static final String UNSETTLED_TRANSACTION = TABLE_SPACE.concat(".UNSETTLED_TRANSACTION");
        public static final String SETTLEMENT_REPORT = TABLE_SPACE.concat(".SETTLEMENT_REPORT");
        public static final String RESPONSE_CODE_CONFIG = TABLE_SPACE.concat(".BANCS_CONNECT_RESPONSE");

    }

    public static class ServiceActions {
        public static final String UPDATE_SERVICE = "SERVICE:UPDATE";
        public static final String CREATE_SERVICE = "SERVICE:CREATE";
        public static final String APPROVE_SERVICE = "SERVICE:APPROVE";
        public static final String DELETE_SERVICE = "SERVICE:DELETE";

        public static final String APPROVED = "Y";
        public static final String CANCELED = "N";

    }

    public static class ApprovalType {
        public static final String APPROVED = "Y";
        public static final String CANCELLED = "N";
    }

    public static class ApprovalStatus {
        public static final String APPROVED = "approved";
        public static final String CANCELLED = "cancelled";
        public static final String UNAPPROVED = "unapproved";
    }

    public static class AppActions {
        public static final String CREATE = "create";
        public static final String UPDATE = "update";
        public static final String DEACTIVATE = "deactivate";
        public static final String ACTIVATE = "activate";
    }

    public static class Constants {
        public static final String APP_CODE = "SUPPORT";
        public static final String LOCAL = "infometics";
        public static final String UBA = "uba";
    }

    public static class EmailTemplates {
        public static final String EMAIL_TEMPLATE_PATH = "UPLOAD_FILE_REQUEST.tpl";
        public static final String EMAIL_TEMPLATE_PATH_SETTLEMENT_REPORT = "UPLOAD_FILE_SETTLEMENT_REPORT.tpl";
        public static final String EMAIL_TEMPLATE_PATH_STATUS_ENQUIRY = "STATUS_UPLOAD.tpl";
    }

    public static class ServiceQueries {

        public static final String NIP_INFLOW_SUCCESS = "responsecode = '00' AND tsq_2_rsp_code = '00' and C24_RSP_CODE IN ( '000','913')";
        public static final String NIP_INFLOW_PENDING = "responsecode = '00' AND tsq_2_rsp_code = '00' AND (C24_RSP_CODE NOT IN ('000','913') OR C24_RSP_FLG = 'N')";
        public static final String NIP_INFLOW_FAILED = "responsecode = '00' AND ((tsq_2_rsp_code <> '00') OR (C24_RSP_CODE NOT IN ('000','913')))";


        public static final String NIP_OUTFLOW_SUCCESS = "DEBIT_RSP_CODE IN ('000') AND (NIP_TSQ_RSP_CODE  = '00' OR TSQ_2_RSP_CODE  = '00')";
        public static final String NIP_OUTFLOW_PENDING = "DEBIT_RSP_CODE IN ('000') AND (NIP_TSQ_RSP_CODE IN ('09', '99', '25', '26', '94', '01') OR NIP_TSQ_FLG = 'N')  AND (TSQ_2_RSP_CODE IN ('09', '99', '25', '94', '01') OR TSQ_2_FLG = 'N')";
        public static final String NIP_OUTFLOW_FAILED = "DEBIT_RSP_CODE IN ('000') AND (NIP_TSQ_RSP_CODE NOT IN ('00', '25'))  AND (TSQ_2_RSP_CODE NOT IN ('00', '25') OR TSQ_2_FLG = 'N')";
        public static final String NIP_OUTFLOW_ACCOUNT_DEBIT_SUCCESS = " DEBIT_RSP_CODE in ('000', '911')";
        public static final String NIP_OUTFLOW_ACCOUNT_DEBIT_FAILED = " DEBIT_RSP_CODE not in ('000', '911')";
        public static final String NIP_OUTFLOW_REVERSAL_SUCCESS = "REVERSAL_RSP_CODE='000' and REVERSAL_FLG='Y'";
        public static final String NIP_OUTFLOW_REVERSAL_FAILED = "REVERSAL_RSP_CODE <> '000' and REVERSAL_FLG='Y'";


        public static final String UP_INFLOW_SUCCESS = "(RESPONSECODE = '00' or TSQ_2_RSP_CODE = '00') and C24_RSP_CODE = '000'";
        public static final String UP_INFLOW_PENDING = "(RESPONSECODE = '05' or TSQ_2_RSP_CODE = '05')";
        public static final String UP_INFLOW_FAILED = "(RESPONSECODE = '03' or TSQ_2_RSP_CODE = '03')";


        public static final String UP_OUTFLOW_SUCCESS = " (its_rsp_code = '00' OR its_tsq_rsp_code = '00')";
        public static final String UP_OUTFLOW_PENDING = " (its_rsp_code IN ('09', '99', '25', '26', '94', '01') OR its_rsp_flg = 'N')  AND (its_tsq_rsp_code IN ('09', '99', '25', '94', '01') OR its_tsq_flg = 'N')";
        public static final String UP_OUTFLOW_FAILED = " (its_rsp_code NOT IN ('00','25') OR its_tsq_rsp_code NOT IN ('00', '25') AND its_tsq_flg = 'N')";
        public static final String UP_OUTFLOW_ACCOUNT_DEBIT_SUCCESS = " DEBIT_RSP_CODE in ('000')";
        public static final String UP_OUTFLOW_ACCOUNT_DEBIT_FAILED = " DEBIT_RSP_CODE not in ('000', '913')";

        public static final String UP_OUTFLOW_DEBIT_SUCCESS_FAILED_TXN = " DEBIT_RSP_CODE IN ('000','911')";
        public static final String UP_OUTFLOW_REVERSAL_SUCCESS = " REVERSAL_RSP_CODE IN ('000','913') and REVERSAL_FLG='Y' ";
        public static final String UP_OUTFLOW_REVERSAL_FAILED = " REVERSAL_RSP_CODE NOT IN ('000','913') and REVERSAL_FLG='Y'";

        public static final String AIRTIME_SUCCESS = "(topup_rsp_code = 'SUC' or topup_rsp_code_2 = '00' or tsq_rsp_code = 'SUC') and DEBIT_RSP_CODE in ('000')";
        public static final String AIRTIME_PENDING = "(topup_rsp_code = 'UNKW' and tsq_rsp_code in ('UNKW', 'QER')) OR ((topup_rsp_code = 'UNKW' or topup_rsp_code is null) and tsq_rsp_code is null) and DEBIT_RSP_CODE in ('000')";
        public static final String AIRTIME_FAILED = "(topup_rsp_code <> 'SUC' or topup_rsp_code_2 <> '00' or tsq_rsp_code <> 'SUC') and DEBIT_RSP_CODE in ('000')";
        public static final String AIRTIME_ACCOUNT_DEBIT_SUCCESS = " DEBIT_RSP_CODE in ('000', '911')";
        public static final String AIRTIME_ACCOUNT_DEBIT_FAILED = " DEBIT_RSP_CODE NOT IN ('000', '911')";
        public static final String AIRTIME_REVERSAL_SUCCESS = "DEBIT_REVERSAL_RSP_CODE = '000' AND DEBIT_REVERSAL_RSP_FLG='Y'";
        public static final String AIRTIME_REVERSAL_FAILED = "DEBIT_REVERSAL_RSP_CODE <> '000' AND DEBIT_REVERSAL_RSP_FLG='Y'";



    }
}

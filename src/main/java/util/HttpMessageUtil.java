package util;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.JsonObject;
import java.io.IOException;
import java.net.URI;


//import org.apache.xerces.util.URI;

/**
 * @author Sunday Kusoro
 * @since 13/12/2021
 */
public class HttpMessageUtil {

    static final private Logger LOG = LogManager.getLogger(HttpMessageUtil.class);


    public static final void sendMessage(CloseableHttpClient httpclient, BaseBean configBean, BaseBean requestBean) {

        String resp = null;
        //BaseBean hdrMap = new BaseBean();
        String startTm = DtTm.getTm();
        try {

            LOG.info("Sending msg to AD");

            String path = requestBean.getString("path");
            URI uri;
            if (path.equals("entrust")) {
                LOG.info("Sending msg to Entrust");

                uri = new URIBuilder()
                        .setScheme("https")
                        .setHost(configBean.getString("entrust_uri"))
                        .setPort(Integer.parseInt(configBean.getString("entrust_port")))
                        .setPath("/" + configBean.getString("entrust_path"))
                        .build();

                LOG.info("Entrust path: {}", configBean.getString("entrust_path"));
                LOG.info("ENTRUST URI: {}", uri);

            } else if (path.equalsIgnoreCase("ibm_ace")) {
                LOG.info("Sending msg to IBM");

                uri = new URIBuilder()
                        .setScheme(configBean.getString("ibm_scheme"))
                        .setHost(configBean.getString("ibm_host"))
                        .setPort(Integer.parseInt(configBean.getString("ibm_port")))
                        .setPath("/" + configBean.getString("ibm_path"))
                        .build();

                LOG.info("IBM URI: {}", uri);

            } else {
                LOG.info("Sending msg to AD");

                 uri = new URIBuilder()
                        .setScheme("http")
                        .setHost(configBean.getString("ad_base_uri"))
                        .setPath("")
                        .build();
                LOG.info("AD URI: {}", uri);

            }
            HttpPost httppost = new HttpPost(uri);
            httppost.setHeader("Content-Type", "application/json");

            if (path.equals("entrust")) {
                httppost.setHeader("Authorization", requestBean.getString("auth_details"));
                httppost.setHeader("Content-Type", "application/xml");

            }
            LOG.info("Operation Support office-Channel ");
            LOG.info(configBean.getString("inst_id"));


            StringEntity entity = new StringEntity(requestBean.getString("ad_request"));
            httppost.setEntity(entity);


            httppost.setConfig(CustomUtil.getHttpRequestTimeoutConfig(configBean));

            //Execute the Get request
            CloseableHttpResponse response = null;


            String startTm3 = DtTm.getTm();

            try {

                LOG.info("calling external API");

                try {
                    response = httpclient.execute(httppost);
                    LOG.info("The response is =======" + response);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    abortCall(httppost, e);
                    checkTimeout(e, requestBean);
                    LOG.error("", e);
                } catch (Exception e) {
                    abortCall(httppost, e);
                    checkTimeout(e, requestBean);
                    LOG.error("", e);
                }
                LOG.info("Done with call");

                LOG.info("API TAT :: " + DtTm.getTmDiff(startTm3));


                if (response != null) {

                    StatusLine sl = response.getStatusLine();

                    HttpEntity respEntity = response.getEntity();

                    if (sl != null) {

                        LOG.info(sl.getStatusCode());

                        requestBean.setString("x-http-status-code", "" + sl.getStatusCode());
//                        if (path.equals("Reset") &&
//                                requestBean.getString("x-http-status-code").equals(ResponseUtil.HTTP_OK_STATUS)) {
//                            setResponseHeaders(requestBean, response);
//                        }

                    } else {

                        LOG.info("StatusLine is NULL");

                    }


                    if (respEntity != null) {

                        resp = EntityUtils.toString(respEntity);

                        CustomUtil.fillBaseBean(response.getAllHeaders(), requestBean);

                        LOG.debug(resp);

                        requestBean.setString("auth-status", resp);
                        LOG.info("API response message is =======" + resp);

                    } else {

                        LOG.info("No response from API");

                    }
                    EntityUtils.consume(respEntity);
                } else {
                    LOG.info("Response is null");
                }
            } catch (RuntimeException e) {
                LOG.error("", e);
            } catch (Exception e) {
                LOG.error("", e);
            } finally {

                releasePost(httppost, configBean);
                closeResp(response, configBean);
            }

        } catch (Exception e) {
            LOG.error("", e);

        } finally {


        }

        LOG.info("post TAT :: " + DtTm.getTmDiff(startTm));
        LOG.trace(requestBean.toString());

    }

    private static void setResponseHeaders(BaseBean requestBean, CloseableHttpResponse response) {
        for (Header header : response.getAllHeaders()) {
            LOG.info("Header Response: " + header.getName() + " : " + header.getValue());
            requestBean.setString(header.getName(), header.getValue());
        }
    }

    private final static void abortCall(HttpUriRequest httppost, Exception e) {
        if (null != httppost && !httppost.isAborted()) {
            try {
                LOG.debug("aborting request");
                httppost.abort();
                LOG.debug("done aborting..");
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                //e1.printStackTrace();
                LOG.error("error aborting..", e1);
            }
        }

    }

    private final static void checkTimeout(Exception e, BaseBean requestBean) {

        if (null != e) {

            if (e.toString().contains("timed out")) {
                requestBean.setString("x-http-status-code", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
            }

        }
    }


    private final static void releasePost(HttpPost httppost, BaseBean configBean) {
        if ("false".equalsIgnoreCase(configBean.getString("rls_conn"))) {
        } else {

            LOG.debug("releasing conn");
            try {
                httppost.releaseConnection();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                LOG.debug("", e);
            }

            LOG.debug("conn released");

        }

    }


    private final static void closeResp(CloseableHttpResponse response, BaseBean configBean) {
        if ("false".equalsIgnoreCase(configBean.getString("cls_conn"))) {
        } else {
            LOG.debug("closing resp conn");
            HttpClientUtils.closeQuietly(response);
            LOG.debug("done closing conn");

        }

    }

    private final static void releaseGet(HttpGet httpget, BaseBean configBean) {
        if ("false".equalsIgnoreCase(configBean.getString("rls_conn"))) {
        } else {

            LOG.debug("releasing conn");
            try {
                httpget.releaseConnection();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                LOG.debug("", e);
            }

            LOG.debug("conn released");

        }

    }


    public static final void callSecS(CloseableHttpClient httpclient, RequestConfig config, BaseBean configBean, BaseBean requestBean) {

        String resp = null;
        //String startTm = DtTm.getTm();
        try {

            LOG.info("Sending msg to SS");


            String url = configBean.getString("sec_url");

            String cbs_msg = null;


            cbs_msg = requestBean.getString("json_message");


            LOG.info(url);
            HttpPost httppost = new HttpPost(url);

            httppost.setHeader("Content-Type", "application/json");


            LOG.info("encrypting cbs-bound message");


            StringEntity entity = new StringEntity(cbs_msg);

            httppost.setEntity(entity);

            httppost.setConfig(config);


            //Execute GET Message request

            CloseableHttpResponse response = null;
            LOG.info("Content type: " + httppost.getFirstHeader("Content-Type").getValue());
            try {
                LOG.info("post msg");
                LOG.info(httppost.toString());

                String startTm3 = DtTm.getTm();

                try {

                    response = httpclient.execute(httppost);

                } catch (RuntimeException e) {

                    abortCall(httppost, e);

                    if (e.toString().contains("timed out")) {

                        requestBean.setString("sec-http-status-code", ResponseUtil.READ_TIMEOUT);

                    }

                    LOG.error("", e);

                } catch (Exception e) {

                    abortCall(httppost, e);

                    if (e.toString().contains("timed out")) {

                        requestBean.setString("sec-http-status-code", ResponseUtil.READ_TIMEOUT);

                    }

                    LOG.error("", e);
                }


                LOG.debug("waited for:: " + DtTm.getTmDiff(startTm3));


                //LOG.info("Done with executione");

                if (response != null) {

                    HttpEntity respEntity = response.getEntity();

                    StatusLine sl = response.getStatusLine();

                    if (sl != null) {

                        LOG.info("" + sl.getStatusCode());

                        requestBean.setString("sec-http-status-code", "" + sl.getStatusCode());

                    } else {

                        LOG.info("statusline is null");

                    }


                    if (respEntity != null) {

                        String cipherMsg = EntityUtils.toString(respEntity);


                        LOG.info("Cipher Resp Msg");
                        LOG.info(cipherMsg);

                        String enable_auth = System.getProperty("auth.enable");
                        if (enable_auth != null && !"true".equalsIgnoreCase(enable_auth)) {
                            resp = cipherMsg;
                            System.out.println(CryptoUtils.encrypt(cipherMsg));
                        } else {
                            resp = CryptoUtils.decrypt(cipherMsg);
                            LOG.info(resp);
                        }


                        //decrypt message

                        processtSecResponse(resp, requestBean);
                        EntityUtils.consume(respEntity);
                        respEntity = null;
                    } else {

                        LOG.info("content not rcvd");

                    }


                } else {
                    LOG.info("response not rcvd");
                }

            } catch (Exception e) {

                LOG.error(e.getMessage(), e);

            } finally {

                releasePost(httppost, configBean);
                closeResp(response, configBean);


            }

        } catch (Exception e) {
            LOG.error("cbs request exception ", e);
        } finally {


        }

        /*
         * if (requestBean.getString("sec-status-type")==null) {
         *
         * requestBean.setString("sec-status-type", ResponseUtil.RJCT);
         *
         * if (requestBean.getString("sec-http-status-code")!=null) {
         *
         * requestBean.setString("sec-status-code",
         * requestBean.getString("sec-http-status-code"));
         *
         * }else {
         *
         * requestBean.setString("sec-status-code",
         * ResponseUtil.GENERIC_PROCESSING_ERROR);
         *
         * }
         *
         * }
         *
         * LOG.debug(requestBean.getString("sec-status-type"));
         * LOG.debug(requestBean.getString("sec-status-code"));
         * LOG.debug(requestBean.getString("sec-http-status-code"));
         */


    }


    public static final void processtSecResponse(String respStr, BaseBean dataBean) {


        if (respStr == null) {

            LOG.info("No response from SS");
            dataBean.setString("response_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
//            dataBean.setString("status_type", ResponseUtil.CBS_STATUS_TYPE_FAILURE);
            LOG.info("Invalid response from SS; invalid Json format");

        } else {

            JsonObject jobj = JsonUtil.toJsonObject(respStr);

            if (jobj != null) {

                dataBean.setString("response_code", JsonUtil.getJsonObjValue2(jobj, "responseCode"));
                dataBean.setString("token", JsonUtil.getJsonObjValue2(jobj, "token"));

                dataBean.setString("token_type", JsonUtil.getJsonObjValue2(jobj, "tokenType"));

                dataBean.setString("expires_in", JsonUtil.getJsonObjValue2(jobj, "expiresIn"));


//                if ("000".equals(dataBean.getString("response_code"))) {
//                    dataBean.setString("status_type", ResponseUtil.CBS_STATUS_TYPE_SUCCESS);
//                    dataBean.setString("status_code", ResponseUtil.SUCCESS);
//                } else {
//                    dataBean.setString("status_type", ResponseUtil.CBS_STATUS_TYPE_FAILURE);
//                    dataBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
//                }
//            } else {
//
//                dataBean.setString("response_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
//                dataBean.setString("status_type", ResponseUtil.CBS_STATUS_TYPE_FAILURE);
//                LOG.info("Invalid response from SS; invalid Json format");
//
            }

        }

    }
}
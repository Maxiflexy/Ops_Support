package messaging;


import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import util.*;

import java.io.IOException;

public abstract class Common extends RequestValidator {

    final static private Logger LOG = LogManager.getLogger(Common.class);

    final static protected String INST_ID = "UBA";

    protected abstract String validateRequestBody(String request, BaseBean requestBean);

    protected abstract void processResponse(BaseBean requestBean);

    protected abstract String createReply(BaseBean requestBean, Boolean procErr);

    public void sendRequest(BaseBean requestBean, BaseBean configBean, String request, CreateRequestMessage requestMessage) {
        String mqRequest = requestMessage.createRequestMessage(requestBean, configBean, request);
        requestBean.setString("path", "ibm_ace");
        sendRequest(requestBean, configBean, mqRequest);
    }

    public boolean sendRequest(BaseBean requestBean, BaseBean configBean, String request) {
        CloseableHttpClient httpclient = null;
        boolean procErr = false;
        if (configBean != null) {


            LOG.debug("done loading config  ");

            if (request != null) {

                try {

                    //get http client connection
                    httpclient = getHttpClient(INST_ID, configBean);


                    if (httpclient != null) {
                        if (requestBean.getString("path").equals("entrust")) {
                            sendEntrustRequestAndProcessResponse(httpclient, requestBean, configBean, request);
                        } else if (requestBean.getString("path").equals("ibm_ace")) {
                            sendIBMRequestAndProcessResponse(httpclient, requestBean, configBean, request);
                        } else {
                            sendRequestAndProcessResponse(httpclient, requestBean, configBean, request);
                        }
                    } else {

                        LOG.info("httpclient could not be created");

                        procErr = true;

                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {

                    closeHttpClient(httpclient, configBean);

                }

            } else {

                LOG.info("error creating message");

                procErr = true;


            }

        }

        LOG.info("processing err :: " + procErr);
        return procErr;
    }

    private void sendIBMRequestAndProcessResponse(CloseableHttpClient httpclient, BaseBean requestBean, BaseBean configBean, String request) {
        requestBean.setString("ad_request", request);
        SendRequest(httpclient, requestBean, configBean);
        processResponseHeader(requestBean);
        processResponse(requestBean);
    }

    public void sendRequestAndProcessResponse(CloseableHttpClient httpclient, BaseBean requestBean, BaseBean configBean, String request) throws IOException {

        SendRequest(httpclient, requestBean, configBean);
        processResponseHeader(requestBean);
        processResponse(requestBean);
    }

    public void sendEntrustRequestAndProcessResponse(CloseableHttpClient httpclient, BaseBean requestBean, BaseBean configBean, String request) throws IOException {
        SendRequest(httpclient, requestBean, configBean);
        processResponseHeader(requestBean);
        processEntrustResponse(requestBean);

    }

    private void processEntrustResponse(BaseBean requestBean) {
        if (ResponseUtil.HTTP_OK_STATUS.equals(requestBean.getString("x-http-status-code"))) {
            Document document = XMLUtil.getXmlDoc(requestBean.getString("auth-status"));
            String isSuccessful = XMLUtil.extractXmlNodeValue(document, "/Envelope/Body/authenticateTokenResponse/return/isSuccessful");
            String message = XMLUtil.extractXmlNodeValue(document, "/Envelope/Body/authenticateTokenResponse/return/response");
            if (isSuccessful.equals("true")) {
                requestBean.setString("status", ResponseUtil.APP_SUCCESS);
                requestBean.setString("code", ResponseUtil.SUCCESS);
                requestBean.setString("message", message);
            } else {
                requestBean.setString("status", ResponseUtil.FAIL);
                requestBean.setString("code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
                requestBean.setString("message", message);
            }
        } else {
            requestBean.setString("status", ResponseUtil.FAIL);
            requestBean.setString("code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
            requestBean.setString("message", "Request unsuccessful");
        }
        LOG.info(requestBean);
    }

    private void processResponseHeader(BaseBean requestBean) {
        LOG.info("creating reply status ");

        String reqSts = requestBean.getString("x-http-status-code") == null ? "" : requestBean.getString("x-http-status-code").trim();
        switch (reqSts) {
            case ResponseUtil.HTTP_OK_STATUS:
                requestBean.setString("status_code", ResponseUtil.SUCCESS);
                requestBean.setString("status_type", ResponseUtil.APP_SUCCESS);
                break;
            case ResponseUtil.UNSUPPORTED_MEDIA_TYPE:
                requestBean.setString("status_code", ResponseUtil.UNSUPPORTED_MEDIA_TYPE);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
            case ResponseUtil.HTTP_UNAUTHORIZED_STATUS:
                requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
            case ResponseUtil.INALID_REQUEST:
                requestBean.setString("status_code", ResponseUtil.INALID_REQUEST);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
            case ResponseUtil.CHANGE_UNSUCCESSFUL:
                requestBean.setString("status_code", ResponseUtil.CHANGE_UNSUCCESSFUL);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
            case ResponseUtil.PRECONDITIONS_FAILED:
                requestBean.setString("status_code", ResponseUtil.PRECONDITIONS_FAILED);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
            case "":
                requestBean.setString("status_type",ResponseUtil.FAIL);
                requestBean.setString("status_code",ResponseUtil.RESPONSE_NOT_RECEIVED);
            default:
                requestBean.setString("status_code", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
                requestBean.setString("status_type", ResponseUtil.FAIL);
                break;
        }

        LOG.info("done creating reply status ");
    }


    protected final void closeHttpClient(CloseableHttpClient httpClient, BaseBean configBean) {

        if (httpClient != null) {

            try {
                if ("true".equalsIgnoreCase(configBean.getString("cls_http_clnt"))) {
                    httpClient.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }


    }

    protected final CloseableHttpClient getHttpClient(String senderInstId, BaseBean configBean) {


        CloseableHttpClient httpClient = null;
        String tlsCert = getTLSCertPath(senderInstId, configBean);

        String alias = configBean.getString("tls_alias");

        String tmppass = configBean.getString("tls_password");
        String storepass = configBean.getString("tls_storepass");
        String keypass = configBean.getString("tls_keypass");

        if ("".equals(tmppass.trim())) {
        } else {

            if ("".equals(storepass)) {
                storepass = tmppass;
            }

            if ("".equals(keypass)) {
                keypass = tmppass;
            }
        }


        String maxCnn = configBean.getString("max_conn");

        LOG.debug("creating connection factory");


        try {

            httpClient = CBSHttpConnection.getInstance().getConnection(tlsCert, alias, storepass, keypass, maxCnn, senderInstId);

            LOG.debug("connection succeded");

        } catch (RuntimeException e) {

            LOG.error("", e);
            LOG.debug("connection failed");

        } catch (Exception e) {

            LOG.debug("connection failed");
            LOG.error("", e);

        }

        return httpClient;
    }

    private final String getTLSCertPath(final String senderInstId, final BaseBean configBean) {

        String tlsCert = configBean.getString("cfg_home") + "/security/" + configBean.getString("tls_keystore");

        LOG.debug("tls-cert-path");
        LOG.debug(tlsCert);

        return tlsCert;
    }

    private void SendRequest(CloseableHttpClient httpclient, BaseBean requestBean, BaseBean configBean) {
        try {

            HttpMessageUtil.sendMessage(httpclient, configBean, requestBean);

        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            LOG.info("error making call");
            LOG.error("", e);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.info("error making call");
            LOG.error("", e);
        }
    }


}

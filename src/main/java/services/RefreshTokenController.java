package services;

import messaging.RefreshService;
import services.servlets.BaseServlet;
import util.CookieUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class RefreshTokenController extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new RefreshService());
                respStr = getExecutor().execute(requestStr);
                out.print(respStr);
                try {
                    Cookie tokenCookie = new Cookie("access_token", JsonUtil.toJsonObject(respStr).getString("token"));

                    // Set secure attributes
                    tokenCookie.setHttpOnly(true);                // Prevent access from JavaScript
                    tokenCookie.setSecure(true);                  // Only send over HTTPS
                    tokenCookie.setPath("/");                     // Available throughout the site
                    tokenCookie.setMaxAge(7 * 24 * 60 * 60);      // 7 days

                    // Add cookie to response
                    CookieUtil.setCookie(servletRequest, servletResponse, "access_token", tokenCookie);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(respStr);

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
        try {
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

}

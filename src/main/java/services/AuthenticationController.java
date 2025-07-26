package services;

import messaging.AuthenticationService;
import services.servlets.BaseServlet;
import util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static util.JsonUtil.addObject;

public class AuthenticationController extends BaseServlet {


    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        String requestString;
        try {
            requestString = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr;
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            JsonObjectBuilder builder = Json.createObjectBuilder();
            JsonObject jsonRequest = JsonUtil.toJsonObject(CryptoUtils.decrypt(requestString));
            if (jsonRequest != null) {
                addObject(builder, "username", jsonRequest.getString("username"));
                addObject(builder, "password", jsonRequest.getString("password"));
                addObject(builder, "token", jsonRequest.getString("token"));

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new AuthenticationService());
                respStr = getExecutor().execute(builder.build().toString());
                LOG.info("Auth Response: {}", respStr);
                try {
                    Cookie tokenCookie = new Cookie("access_token", JsonUtil.toJsonObject(respStr).getString("token"));

                    // Set secure attributes
                    tokenCookie.setHttpOnly(true);                // Prevent access from JavaScript
                    tokenCookie.setSecure(true);                  // Only send over HTTPS
                    tokenCookie.setPath("/");                     // Available throughout the site
                    tokenCookie.setMaxAge(7 * 24 * 60 * 60);      // 7 days

                    // Add cookie to response
                    servletResponse.addCookie(tokenCookie);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                out.print(CryptoUtils.encrypt(respStr));
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
                    LOG.error(e.getMessage());
                }
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(CryptoUtils.encrypt(respStr));
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage(), e);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            String respStr = getInvalidFormatResponse();
            assert out != null;
            out.print(CryptoUtils.encrypt(respStr));
        }
        try {
            out.flush();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }
}

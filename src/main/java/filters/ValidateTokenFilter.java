package filters;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.BaseBean;
import util.CookieUtil;
import util.JsonServiceConfig;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;

import static util.CustomUtil.verifyToken;


public class ValidateTokenFilter implements Filter {

    final static Logger LOG = LogManager.getLogger(ValidateTokenFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {


        HttpServletRequest httpRequest = (HttpServletRequest) request;

        boolean authorized = false;
        LOG.info("In filter class");
        BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
//        String authorizationToken = httpRequest.getHeader("Authorization");
        String authorizationToken = CookieUtil.getCookie((HttpServletRequest) request, "access_token").getValue();
        LOG.info("Cookie token: {}", authorizationToken);
        String ipAddress = httpRequest.getHeader("HTTP_X_FORWARDED_FOR");

        LOG.info("PATH IS: {}",      httpRequest.getServletPath());
        LOG.info("METHOD IS: {}",    ((HttpServletRequest) request).getMethod());

        String svciD = httpRequest.getHttpServletMapping().getServletName().toLowerCase().replace("servlet", "");

        if (ipAddress == null) {
            ipAddress = httpRequest.getRemoteAddr();
        }
        try {
            if (!authorizationToken.isEmpty()) {
               authorized = verifyToken(authorizationToken, configBean.getString("public_key"), configBean.getString("private_key"), request);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        if (!authorized) {

            LOG.info("authenticatn failed: access denied");
            request.setAttribute("auth", "FAIL");
            request.setAttribute("javax.servlet.error.status_code", 401);
            response.setContentType("application/json");
            RequestDispatcher rd = request.getRequestDispatcher("/error");
            rd.include(request, response);

        } else {
            chain.doFilter(request, response);
        }


    }


    @Override
    public void destroy() {
        Filter.super.destroy();
    }



}

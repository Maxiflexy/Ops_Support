package filters;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.DBHelper;
import util.BaseBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


public class AuthorizationFilter implements Filter {

    final static Logger LOG = LogManager.getLogger(AuthorizationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String svciD = httpRequest.getHttpServletMapping().getServletName().toLowerCase().replace("servlet", "");
        String requestPath = httpRequest.getServletPath();
        System.out.println("path:: " + httpRequest.getContextPath());
        LOG.info("SVCID:  {}", svciD);

        BaseBean requestBean = new BaseBean();
        requestBean.setString("email", (String) request.getAttribute("username"));
        requestBean.setString("role_id", (String) request.getAttribute("role_id"));
        requestBean.setString("url", requestPath);
        requestBean.setString("method", ((HttpServletRequest) request).getMethod());

        boolean hasRightPermission = DBHelper.hasRightPermission(requestBean);
        LOG.info("Has right permission: {}", hasRightPermission);

        if (hasRightPermission) {
            chain.doFilter(request, response);
        } else {
            request.setAttribute("javax.servlet.error.status_code", 403);
            response.setContentType("application/json");
            RequestDispatcher rd = request.getRequestDispatcher("/error");
            rd.include(request, response);
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}

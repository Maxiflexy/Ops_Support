package services.executors;

import javax.servlet.http.HttpServletResponse;

public interface DownloadExecutor {

    String execute(String request, String currentUser, HttpServletResponse servletResponse);

}

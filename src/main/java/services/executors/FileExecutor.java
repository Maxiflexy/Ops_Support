package services.executors;

import javax.servlet.http.Part;
import java.util.List;

public interface FileExecutor {

    String execute(String request, String currentUser, String actionId, List<Part> parts);

}

package services.executors;

public interface RequestExecutor {
    String execute(String request, String currentUser, String actionId);

}

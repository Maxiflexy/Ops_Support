package util;

public class FileProcessingResult {
    private final int totalUnapproved;
    private final int totalValidated;
    private final int totalFailed;

    public FileProcessingResult(int totalUnapproved, int totalValidated, int totalFailed) {
        this.totalUnapproved = totalUnapproved;
        this.totalValidated = totalValidated;
        this.totalFailed = totalFailed;
    }

    public int getTotalUnapproved() {
        return totalUnapproved;
    }

    public int getTotalValidated() {
        return totalValidated;
    }

    public int getTotalFailed() {
        return totalFailed;
    }
}

package pl.khuzzuk.index;

public record IndexProgress(boolean running, int processedFiles, int totalFiles, String message) {
    public static IndexProgress started() {
        return new IndexProgress(true, 0, 0, "Indeksowanie plikow...");
    }

    public static IndexProgress metadata(int processedFiles, int totalFiles) {
        return new IndexProgress(true, processedFiles, totalFiles, "Zbieranie metadanych...");
    }

    public static IndexProgress finished() {
        return new IndexProgress(false, 0, 0, "");
    }
}

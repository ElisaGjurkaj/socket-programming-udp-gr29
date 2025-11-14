package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class FileUploadSession {
    private final Path filePath;
    private final ByteArrayOutputStream fileData;
    private long bytesReceived;
    private long lastChunkTime;
    private final String clientKey;

    public FileUploadSession(Path filePath, String clientKey) {
        this.filePath = filePath;
        this.fileData = new ByteArrayOutputStream();
        this.bytesReceived = 0;
        this.lastChunkTime = System.currentTimeMillis();
        this.clientKey = clientKey;
    }

    public Path getFilePath() {
        return filePath;
    }

    public byte[] getFileBytes() {
        return fileData.toByteArray();
    }

    public synchronized void appendData(byte[] data) throws IOException {
        fileData.write(data);
        bytesReceived += data.length;
        lastChunkTime = System.currentTimeMillis();
    }

    public boolean isTimedOut() {
        return (System.currentTimeMillis() - lastChunkTime) > 300_000; // 5 minuta
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public String getClientKey() {
        return clientKey;
    }
}

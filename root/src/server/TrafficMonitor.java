package server;

import common.Utils;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficMonitor {
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final ConcurrentHashMap<String, ClientLogInfo> clientStats = new ConcurrentHashMap<>();

    public TrafficMonitor() {}

    public void recordMessageReceived(long bytes, String clientKey) {
        totalBytesReceived.addAndGet(bytes);
        totalMessages.incrementAndGet();
        updateClientStats(clientKey, bytes, true);
    }

    public void recordMessageSent(long bytes, String clientKey) {
        totalBytesSent.addAndGet(bytes);
        totalMessages.incrementAndGet();
        updateClientStats(clientKey, bytes, false);
    }

    private void updateClientStats(String clientKey, long bytes, boolean received) {
        clientStats.compute(clientKey, (k, info) -> {
            if (info == null) info = new ClientLogInfo(clientKey);
            if (received) info.addReceived(bytes);
            else info.addSent(bytes);
            return info;
        });
    }

    public String getStats(ConcurrentHashMap<String, ClientHandler> clients) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== STATISTIKA E SERVERIT ===\n");
        sb.append("Lidhje aktive: ").append(clients.size()).append("\n");
        sb.append("Mesazhe totale: ").append(totalMessages.get()).append("\n");
        sb.append("Trafik total: ").append(Utils.formatFileSize(totalBytesReceived.get() + totalBytesSent.get())).append("\n");
        sb.append("Trafik i pranuar: ").append(Utils.formatFileSize(totalBytesReceived.get())).append("\n");
        sb.append("Trafik i dërguar: ").append(Utils.formatFileSize(totalBytesSent.get())).append("\n");
        sb.append("Koha: ").append(Utils.getCurrentTimestamp()).append("\n");
        sb.append(">> Detaje për klientët:\n");

        if (clients.isEmpty()) {
            sb.append("Asnjë klient aktiv.\n");
        } else {
            for (ClientHandler client : clients.values()) {
                String clientKey = client.getClientKey();
                System.out.println("client key: " + clientKey);
                ClientLogInfo info = clientStats.get(clientKey);
                long recv = info != null ? info.getBytesReceived() : 0;
                long sent = info != null ? info.getBytesSent() : 0;
                int msgs = info != null ? info.getMessages() : 0;

                sb.append(String.format(" - %s | Pranuar: %s | Dërguar: %s | Mesazhe: %d\n",
                        clientKey,
                        Utils.formatFileSize(recv),
                        Utils.formatFileSize(sent),
                        msgs));
            }
        }
        sb.append("========================================\n");
        return sb.toString();
    }

    private static class ClientLogInfo {
        private final String clientKey;
        private long bytesReceived;
        private long bytesSent;
        private int messages;

        public ClientLogInfo(String clientKey) {
            this.clientKey = clientKey;
        }

        public synchronized void addReceived(long bytes) {
            this.bytesReceived += bytes;
            this.messages++;
        }

        public synchronized void addSent(long bytes) {
            this.bytesSent += bytes;
            this.messages++;
        }

        public long getBytesReceived() {
            return bytesReceived;
        }

        public long getBytesSent() {
            return bytesSent;
        }

        public int getMessages() {
            return messages;
        }

        @Override
        public String toString() {
            return clientKey;
        }
    }
}
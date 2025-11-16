package server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler implements Runnable {

    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final ClientHandler client;
    private final FileManager fileManager;
    private final TrafficMonitor trafficMonitor;
    private final Map<String, ClientHandler> clients;

    private static final ConcurrentHashMap<String, String> uploads = new ConcurrentHashMap<>();

    public ServerHandler(DatagramSocket socket, DatagramPacket packet, ClientHandler client,
            FileManager fileManager, TrafficMonitor trafficMonitor,
            Map<String, ClientHandler> clients) {
        this.socket = socket;
        this.packet = packet;
        this.client = client;
        this.fileManager = fileManager;
        this.trafficMonitor = trafficMonitor;
        this.clients = clients;
    }

    @Override
    public void run() {
        String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

        if (trafficMonitor != null) {
            trafficMonitor.recordMessageReceived(packet.getLength(), client.getClientKey());
        }

        try {
            if (msg.startsWith("/login "))
                handleLogin(msg);
            else if (msg.equalsIgnoreCase("/list"))
                sendResponse(fileManager.listFiles());
            else if (msg.startsWith("/read "))
                sendResponse(fileManager.readFile(msg.substring(6).trim()));
            else if (msg.startsWith("/download "))
                handleDownload(msg.substring(10).trim());
            else if (msg.startsWith("/delete "))
                handleDelete(msg.substring(8).trim());
            else if (msg.startsWith("/upload_start "))
                handleUpload(msg);
            else if (msg.startsWith("/search "))
                sendResponse(fileManager.searchFiles(msg.substring(8).trim()));
            else if (msg.startsWith("/info "))
                sendResponse(fileManager.getFileInfo(msg.substring(6).trim()));
            else if (msg.equalsIgnoreCase("STATS"))
                sendResponse(trafficMonitor.getStats(new ConcurrentHashMap<>(clients)));
            else
                sendResponse("Komandë e panjohur: " + msg);
        } catch (IOException e) {
            try {
                sendResponse("Gabim në server: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleLogin(String msg) throws IOException {
        String password = msg.substring(7).trim();
        if (password.equals(ServerConfig.ADMIN_PASSWORD)) {
            client.promoteToAdmin();
            sendResponse("Mirë se vini, Admin!");
        } else {
            sendResponse("Password gabim. Nuk keni privilegje admin.");
        }
    }

    private void handleDelete(String filename) throws IOException {
        if (!client.isAdmin()) {
            sendResponse("Vetëm admin mund të fshijë file.");
            return;
        }
        sendResponse(fileManager.deleteFile(filename));
    }

    private void handleDownload(String filename) throws IOException {
        String response = fileManager.prepareDownload(filename);
        sendResponse(response);
    }

    private void handleUpload(String msg) throws IOException {
        if (!client.isAdmin()) {
            sendResponse("Vetëm admin mund të bëjë upload.");
            return;
        }

        if (msg.length() <= 14) {
            sendResponse("Format gabim: /upload_start <filename>:<base64content>");
            return;
        }

        String[] parts = msg.substring(14).split(":", 2);
        if (parts.length < 2) {
            sendResponse("Format gabim: /upload_start <filename>:<base64content>");
            return;
        }

        String filename = parts[0].trim();
        String base64Content = parts[1].trim();

        if (filename.isEmpty() || base64Content.isEmpty()) {
            sendResponse("Gabim: mungon emri i skedarit ose përmbajtja.");
            return;
        }

        if (!isValidFilename(filename)) {
            sendResponse("Gabim: emri i skedarit është i pavlefshëm.");
            return;
        }

        Path filePath = Path.of(ServerConfig.BASE_DIR, filename);

        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            sendResponse("Gabim: nuk mund të krijohet direktoria: " + e.getMessage());
            return;
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64Content.replaceAll("\\s+", ""));
        } catch (IllegalArgumentException e) {
            sendResponse("Gabim: përmbajtje Base64 e pavlefshme.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile(), false)) {
            fos.write(data);
        } catch (IOException e) {
            sendResponse("Gabim gjatë ruajtjes së skedarit: " + e.getMessage());
            return;
        }

        String response = "File u pranuar me sukses dhe u ruajt tek: " + filePath;
        sendResponse(response);

        if (trafficMonitor != null) {
            trafficMonitor.recordMessageSent(
                    response.length(),
                    client.getAddress().getHostAddress());
        }

        System.out.println("[UPLOAD] " + " - (" + client.getAddress().getHostAddress()
                + ") → " + filename + " (" + data.length + " bytes)");
    }

    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty())
            return false;
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            return false;
        if (filename.length() > 255)
            return false;
        return filename.matches("^[a-zA-Z0-9._\\-]+$");
    }

    private void sendResponse(String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        DatagramPacket dp = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());
        socket.send(dp);

        if (trafficMonitor != null) {
            trafficMonitor.recordMessageSent(bytes.length, client.getAddress().getHostAddress());
        }
    }
}
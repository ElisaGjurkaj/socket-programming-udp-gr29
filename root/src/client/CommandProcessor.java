package client;

import server.ServerConfig;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class CommandProcessor {
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final int serverPort;
    private static final int BUFFER_SIZE = 65507;
    private boolean isAdmin = false;

    private static final String ADMIN_PASSWORD = "admin123";

    public CommandProcessor(DatagramSocket socket, InetAddress serverAddress, int serverPort) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    private void ensureDownloadsDirExists() {
        try {
            Path path = Paths.get(ServerConfig.DOWNLOADS);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Gabim gjatë krijimit të direktorisë: " + e.getMessage());
        }
    }


    public void processCommand(String command) {
        try {
            if (command.equalsIgnoreCase("STATS") ||
                    command.startsWith("/list") ||
                    command.startsWith("/read ") ||
                    command.startsWith("/search ") ||
                    command.startsWith("/info ") ||
                    command.startsWith("/download ")) {
                sendAndReceive(command);

            } else if (command.startsWith("/login ")) {
                handleLogin(command);

            } else if (command.startsWith("/upload_start ")) {
                if (!isAdmin) {
                    System.out.println("Vetëm admin mund të bëjë upload.");
                    return;
                }
                handleUpload(command);

            } else if (command.startsWith("/delete ")) {
                if (!isAdmin) {
                    System.out.println("Vetëm admin mund të fshijë file.");
                    return;
                }
                sendAndReceive(command);

            } else {
                System.out.println("Komandë e panjohur: " + command);
            }

        } catch (IOException e) {
            System.err.println("Gabim në procesimin e komandës: " + e.getMessage());
        }
    }

    private void handleLogin(String command) throws IOException {
        String[] parts = command.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Përdorimi: /login <password>");
            return;
        }

        String password = parts[1];
        sendAndReceive(command);

        if (password.equals(ADMIN_PASSWORD)) {
            isAdmin = true;
            System.out.println("Mirë se vini, Admin! Ju tani keni privilegje të plota.");
        } else {
            System.out.println("Ky password nuk ka privilegje admin.");
        }
    }

    private void handleUpload(String command) throws IOException {
        String[] parts = command.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("Përdorimi: /upload_start <file_path>");
            return;
        }

        Path filePath = Paths.get(parts[1].trim());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.out.println("Gabim: File nuk ekziston ose nuk është valid.");
            return;
        }

        String fileName = filePath.getFileName().toString();
        byte[] fileBytes = Files.readAllBytes(filePath);
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        String message = "/upload_start " + fileName + ":" + base64Content;
        sendAndReceive(message);
    }

    private void sendAndReceive(String message) throws IOException {
        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        socket.send(sendPacket);

        byte[] receiveBuffer = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        int originalTimeout = socket.getSoTimeout();
        int timeout = isAdmin ? 1000 : 3000;
        socket.setSoTimeout(timeout);

        try {
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

            if (response.startsWith("DOWNLOAD:")) {
                handleDownloadResponse(response);
            } else {
                System.out.println("\n[Server Response]\n" + response);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("VINI RE: Serveri nuk u përgjigj brenda " + timeout + " millisekondave.");
        } finally {
            socket.setSoTimeout(originalTimeout);
        }
    }

    private void handleDownloadResponse(String response) {
        String[] parts = response.split(":", 3);
        if (parts.length < 3) {
            System.out.println("Gabim: Përgjigje e pavlefshme për download");
            return;
        }

        String filename = parts[1];
        byte[] fileBytes = Base64.getDecoder().decode(parts[2]);

        ensureDownloadsDirExists();

        String outName = ServerConfig.DOWNLOADS + "/" + filename;

        try (FileOutputStream fos = new FileOutputStream(outName)) {
            fos.write(fileBytes);
            System.out.println("File u shkarkua: downloads/" + filename);
        } catch (IOException e) {
            System.out.println("Gabim në shkrimin e file: " + e.getMessage());
        }
    }
}
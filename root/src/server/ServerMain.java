package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerMain {

    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final FileManager fileManager = new FileManager(ServerConfig.BASE_DIR);
    private static final TrafficMonitor trafficMonitor = new TrafficMonitor();

    private static final int MAX_CLIENTS = 5;

    public static void main(String[] args) {
        System.out.println("Serveri po starton në portin " + ServerConfig.PORT);

        startClientCleanupTask();

        try (DatagramSocket socket = new DatagramSocket(ServerConfig.PORT)) {
            System.out.println("Serveri është gati dhe po pret klientët...");

            while (true) {
                byte[] buffer = new byte[ServerConfig.BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                if (!clients.containsKey(clientKey) && clients.size() >= MAX_CLIENTS) {
                    byte[] msg = "Serveri është plot. Provo më vonë.".getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(msg, msg.length, packet.getAddress(), packet.getPort()));
                    continue;
                }

                ClientHandler client = clients.computeIfAbsent(clientKey,
                        k -> new ClientHandler(packet.getAddress(), packet.getPort()));

                client.updateActivity();

                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

                if (msg.equalsIgnoreCase("PING")) {
                    byte[] pong = "PONG".getBytes(StandardCharsets.UTF_8);
                    DatagramPacket pongPacket = new DatagramPacket(pong, pong.length, packet.getAddress(), packet.getPort());
                    socket.send(pongPacket);
                    System.out.println("Lidhje e re: " + client.getAddress().getHostAddress());
                    trafficMonitor.recordMessageSent(pong.length, client.getClientKey());
                    continue;
                }

                new Thread(new ServerHandler(socket, packet, client, fileManager, trafficMonitor, clients)).start();
            }

        } catch (IOException e) {
            System.err.println("Gabim në server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startClientCleanupTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            clients.values().removeIf(client -> {
                if (client.isTimedOut()) {
                    System.out.println("Klienti " + client.getClientKey() + " u hoq për shkak të inaktivitetit.");
                    return true;
                }
                return false;
            });
        }, 30, 30, TimeUnit.SECONDS);
    }
}
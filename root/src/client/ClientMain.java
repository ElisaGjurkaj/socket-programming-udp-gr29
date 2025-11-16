    package client;

    import common.Role;
    import server.ServerConfig;

    import java.io.IOException;
    import java.net.*;
    import java.nio.charset.StandardCharsets;
    import java.util.Scanner;

    public class ClientMain {
        private static final String SERVER_IP = "localhost";
        private static DatagramSocket socket;
        private static InetAddress serverAddress;
        private static boolean connected = false;

        public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(3000);

                serverAddress = InetAddress.getByName(SERVER_IP);
                CommandProcessor processor = new CommandProcessor(socket, serverAddress, ServerConfig.PORT);

                printHelp();

                while (true) {
                    System.out.print(">> ");
                    String input = scanner.nextLine();
                    if (input == null) break;
                    input = input.trim();
                    if (input.isEmpty()) continue;

                    if (input.equalsIgnoreCase("/quit")) {
                        System.out.println("Po dilni nga aplikacioni...");
                        break;
                    }

                    if (input.equalsIgnoreCase("/help")) {
                        printHelp();
                        continue;
                    }

                    if (input.equalsIgnoreCase("/connect")) {
                        connectToServer();
                        if (connected) {
                            System.out.println("Tani mund të përdorni komandat e serverit.");
                        }
                        continue;
                    }

                    if (!connected) {
                        System.out.println("Ju nuk jeni të lidhur me serverin! Përdorni /connect fillimisht.");
                        continue;
                    }

                    processor.processCommand(input);
                }

            } catch (Exception e) {
                System.err.println("Gabim me socket: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) socket.close();
                scanner.close();
            }
        }

        private static void connectToServer() {
            try {
                serverAddress = InetAddress.getByName(SERVER_IP);

                System.out.println("Duke u lidhur me serverin " + SERVER_IP + ":" + ServerConfig.PORT + "...");
                if (!pingServer()) {
                    System.out.println("Serveri nuk u përgjigj. Lidhja dështoi.");
                    connected = false;
                    return;
                }

                System.out.println("U lidhët me serverin me sukses!");
                connected = true;

            } catch (IOException e) {
                System.out.println("Gabim gjatë lidhjes me serverin: " + e.getMessage());
                connected = false;
            }
        }

        private static boolean pingServer() {
            try {
                byte[] buffer = "PING".getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, ServerConfig.PORT);
                socket.send(packet);

                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                socket.receive(response);

                String msg = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                return msg.equalsIgnoreCase("PONG");
            } catch (Exception e) {
                return false;
            }
        }

        private static void printHelp() {
            System.out.println("  /connect                  - Lidhuni me serverin");
            System.out.println("  /help                     - Shfaq ndihmën");
            System.out.println("  /quit                     - Dil nga aplikacioni");
            System.out.println("\nPasi të lidheni me serverin:");
            System.out.println("  /list                     - Listo file-t në server");
            System.out.println("  /read <file>              - Lexo file nga serveri");
            System.out.println("  /download <file>          - Shkarko file nga serveri");
            System.out.println("  /search <keyword>         - Kërko file në server");
            System.out.println("  /delete <file>            - Fshijë file nga serveri");
            System.out.println("  /info <file>              - Info për file nga serveri");
            System.out.println("  /login <password>         - Login si admin");
            System.out.println("  STATS                     - Statistikat e serverit");
            System.out.println();
        }
    }

package server;

         import common.Role;
         import java.net.InetAddress;

public class ClientHandler {
    private final String clientKey;
    private final InetAddress address;
    private Role role;
    private long lastActive;

    public ClientHandler(InetAddress address, int port) {
        this.address = address;
        this.clientKey = address.getHostAddress() + ":" + port;
        this.role = Role.GUEST;
        this.lastActive = System.currentTimeMillis();
    }

    public void updateActivity() {
        this.lastActive = System.currentTimeMillis();
    }

    public boolean isTimedOut() {
        return (System.currentTimeMillis() - lastActive) > ServerConfig.TIMEOUT_MS;
    }

    public void promoteToAdmin() { this.role = Role.ADMIN; }

    public String getClientKey() { return clientKey; }
    public InetAddress getAddress() { return address; }

    public boolean isAdmin() { return role == Role.ADMIN; }
}
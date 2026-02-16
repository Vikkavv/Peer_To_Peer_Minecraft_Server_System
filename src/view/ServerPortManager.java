package view;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public class ServerPortManager {

    private final Set<Integer> usedPorts = new HashSet<>();
    private static final int DEFAULT_PORT = 25565;

    public synchronized int assignPort(int preferredPort) {
        if (isPortAvailable(preferredPort) && !usedPorts.contains(preferredPort)) {
            usedPorts.add(preferredPort);
            return preferredPort;
        }
        int port = DEFAULT_PORT;
        while (usedPorts.contains(port) || !isPortAvailable(port)) {
            port++;
            if (port > 65535) {
                throw new RuntimeException("No available ports found");
            }
        }
        usedPorts.add(port);
        return port;
    }

    public synchronized void releasePort(int port) {
        usedPorts.remove(port);
    }

    public boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

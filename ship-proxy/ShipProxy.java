import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class ShipProxy {

    private static final int SHIP_PROXY_PORT = 8080;
    private static final String OFFSHORE_PROXY_HOST = "localhost";
    private static final int OFFSHORE_PROXY_PORT = 9090;
    private static final Logger logger = Logger.getLogger(ShipProxy.class.getName());
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        setupLogger();
        logger.info("Starting Ship Proxy on port " + SHIP_PROXY_PORT);

        try (ServerSocket serverSocket = new ServerSocket(SHIP_PROXY_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start Ship Proxy: " + e.getMessage(), e);
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            logger.info("Connection accepted from " + clientSocket.getInetAddress());

            // Create a new connection to the offshore proxy for each request
            try (Socket offshoreSocket = new Socket(OFFSHORE_PROXY_HOST, OFFSHORE_PROXY_PORT)) {

                // Forward request to offshore proxy
                forwardData(clientSocket.getInputStream(), offshoreSocket.getOutputStream());
                offshoreSocket.getOutputStream().flush();

                // Forward response back to the client
                forwardData(offshoreSocket.getInputStream(), clientSocket.getOutputStream());
                clientSocket.getOutputStream().flush();

                logger.info("Request processed successfully.");
            }
        } catch (IOException e) {
            sendErrorResponse(clientSocket);
            logger.log(Level.SEVERE, "Error handling client connection: " + e.getMessage(), e);
        } finally {
            closeSocket(clientSocket);
        }
    }

    private static void forwardData(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Data forwarding error: " + e.getMessage(), e);
        }
    }

    private static void sendErrorResponse(Socket clientSocket) {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.getOutputStream().write("HTTP/1.1 502 Bad Gateway\r\n\r\n".getBytes());
                clientSocket.getOutputStream().flush();
            }
        } catch (IOException ignored) {
        }
    }

    private static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void setupLogger() {
        Logger rootLogger = Logger.getLogger("");
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }
}
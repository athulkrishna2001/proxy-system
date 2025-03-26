import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class OffshoreProxy {

    private static final int OFFSHORE_PROXY_PORT = 9090;
    private static final Logger logger = Logger.getLogger(OffshoreProxy.class.getName());
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        setupLogger();
        logger.info("Starting Offshore Proxy on port " + OFFSHORE_PROXY_PORT);

        try (ServerSocket serverSocket = new ServerSocket(OFFSHORE_PROXY_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleRequest(clientSocket));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start Offshore Proxy: " + e.getMessage(), e);
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream clientOutput = clientSocket.getOutputStream()) {

            // Read the incoming request
            String requestLine = reader.readLine();
            if (requestLine == null || !requestLine.matches("^\\w+\\s+http[s]?://.+\\s+HTTP/1\\.1$")) {
                throw new ProxyException("Invalid request format: " + requestLine);
            }

            String[] parts = requestLine.split(" ");
            String targetUrl = parts[1];
            logger.info("Fetching URL: " + targetUrl);

            // Fetch data from the target URL
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Send the status line to the client
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            String statusLine = "HTTP/1.1 " + responseCode + " " + responseMessage + "\r\n";
            clientOutput.write(statusLine.getBytes());

            // Send the headers
            for (String key : connection.getHeaderFields().keySet()) {
                if (key != null) {
                    String header = key + ": " + String.join(", ", connection.getHeaderFields().get(key)) + "\r\n";
                    clientOutput.write(header.getBytes());
                }
            }
            clientOutput.write("\r\n".getBytes());
            clientOutput.flush();

            // Send the response body
            try (InputStream websiteInput = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream()) {
                forwardData(websiteInput, clientOutput);
                logger.info("Data fetched and forwarded successfully.");
            }
        } catch (ProxyException e) {
            logger.warning("Proxy error: " + e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling connection: " + e.getMessage(), e);
        }
    }

    private static void forwardData(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
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

// Note: You'll need this exception class as well
class ProxyException extends Exception {
    public ProxyException(String message) {
        super(message);
    }
}
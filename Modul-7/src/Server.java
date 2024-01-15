import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 8888;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final ConsoleHandler consoleHandler = new ConsoleHandler();

    static {
        logger.setLevel(Level.INFO);

        // Buat formatter untuk menampilkan nama pengirim di setiap pesan log
        consoleHandler.setFormatter(new ChatFormatter());

        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
    }

    private ServerSocketChannel serverSocketChannel;
    private Map<SocketChannel, String> clientMap = new HashMap<>();
    private Set<String> usedNames = new HashSet<>();

    public Server() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));

            logger.info("Server started on port " + PORT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during server startup", e);
        }
    }

    public void start() {
        try {
            while (true) {
                SocketChannel clientChannel = serverSocketChannel.accept();
                clientChannel.configureBlocking(true);

                String clientName = getClientName(clientChannel);
                clientMap.put(clientChannel, clientName);

                broadcast(clientName + " has joined the chat");
                sendWelcomeMessage(clientChannel);

                new Thread(() -> {
                    try {
                        while (true) {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead > 0) {
                                buffer.flip();
                                CharBuffer charBuffer = CHARSET.decode(buffer);
                                String message = charBuffer.toString();
                                String formattedMessage = "[" + clientName + "] " + message;

                                logger.info(formattedMessage); // Logging tanpa pesan duplikat

                                broadcastToOthers(clientChannel, formattedMessage);
                            } else {
                                clientChannel.close();
                                clientMap.remove(clientChannel);
                                broadcast(clientName + " has left the chat");
                                break;
                            }
                        }
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error during message reception", e);
                    }
                }).start();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during server operation", e);
        }
    }

    private void broadcastToOthers(SocketChannel senderChannel, String message) throws IOException {
        for (SocketChannel channel : clientMap.keySet()) {
            if (channel != senderChannel) {
                channel.write(CHARSET.encode(message));
            }
        }
        logger.info("Broadcasted: " + message);
    }

    private String getClientName(SocketChannel clientChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        clientChannel.read(buffer);
        buffer.flip();
        String clientName = CHARSET.decode(buffer).toString().trim();

        if (usedNames.contains(clientName)) {
            int suffix = 1;
            String originalName = clientName;
            while (usedNames.contains(clientName)) {
                clientName = originalName + "_" + suffix;
                suffix++;
            }
        }

        usedNames.add(clientName);
        return clientName;
    }

    private void sendWelcomeMessage(SocketChannel clientChannel) throws IOException {
        String welcomeMessage = "Welcome to the chat, enter your messages!";
        clientChannel.write(CHARSET.encode(welcomeMessage));
    }

    private void broadcast(String message) throws IOException {
        for (SocketChannel channel : clientMap.keySet()) {
            channel.write(CHARSET.encode(message));
        }
        logger.info("Broadcasted: " + message);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}

// Buat formatter kustom untuk menampilkan nama pengirim di setiap pesan log
class ChatFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getMessage() + System.lineSeparator();
    }
}

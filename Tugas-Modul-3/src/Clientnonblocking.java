import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private SocketChannel socketChannel;
    private String clientName;

    public Client(String clientName) {
        this.clientName = clientName;
    }

    public void start() {
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            socketChannel.configureBlocking(false);

            // Send client name to the server
            socketChannel.write(CHARSET.encode(clientName));

            // Start a separate thread to handle incoming messages
            new Thread(this::receiveMessages).start();

            // Send messages from the console
            sendMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    CharBuffer charBuffer = CHARSET.decode(buffer);
                    System.out.println(charBuffer.toString());
                    buffer.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessages() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String message = scanner.nextLine();
                // Menambahkan nama client ke setiap pesan yang dikirim
                String fullMessage = clientName + ":" + message;
                socketChannel.write(CHARSET.encode(fullMessage));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        String clientName = scanner.nextLine();

        Client client = new Client(clientName);
        client.start();
    }
}

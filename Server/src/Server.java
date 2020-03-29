import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink/
 * @version 1.1.0
 */

public class Server {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        final ServerSocket server = new ServerSocket(port);
        System.out.println("Starting server on port " + port);
        while(true) {
            final Socket client = server.accept();

            System.out.println("Client connected from " + client.getInetAddress());

            Connection c = new Connection(client);

            new Thread(c).start();
        }
    }
}

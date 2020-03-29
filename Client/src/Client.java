import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink
 * @version 1.1.0
 */

public class Client {

    public static void main(String[] args) throws UnknownHostException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            Socket socket = new Socket(host, port);
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            String line = scanner.nextLine();
            while(!line.equalsIgnoreCase("exit")) {
                pw.println(line);
                pw.flush();
                line = scanner.nextLine();
            }
            scanner.close();
            pw.close();
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import javax.sound.sampled.*;
import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink/
 * @version 1.1.0
 */

public class Server {
    private boolean running;
    private int port;
    private AudioInputStream audioInputStream;
    private ServerSocket server;
    private File audioFile;
    private Clip clip;
    private Manager manager;

    private Request currentRequest;

    private int clientNumber; //stores the amount of users logged on

    /** All keys for the threads to enter synchronized blocks */
    private static final Object outputKey = new Object();
    private static final Object connectionKey = new Object();
    private static final Object clientNumberKey = new Object();
    private static final Object requestKey = new Object();
    private static final Object readyKey = new Object();

    private Thread managerThread;

    private UserList connections;

    private ArrayList<Request> requests;

    public Server(int port) {
        this.port = port;
        clientNumber = 0;
        requests = new ArrayList<Request>();
    }

    public void runServer() {

        connections = new UserList();

        manager = new Manager();

        //make a thread that manages all the threads.
        managerThread = new Thread(manager, "Manager-Thread");

        managerThread.start();

        //make the server
        try {
            this.server = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }


        //Run the server

        int counter = 1;

        try {
            while (true) {
                Socket client = server.accept(); //localhost has been called

                synchronized (outputKey) {
                    System.out.println("Client connected from " + client.getInetAddress());
                }

                Connection newConnection = new Connection(client); //make a runnable passing the client socket to the new Connection

                Thread thread = new Thread(newConnection, "Client" + counter); //make a new thread to run the connection to the client

                synchronized (connectionKey) {
                    connections.add(thread, newConnection, new User(0));
                }

                thread.start(); //start the thread
                counter++;

                synchronized (clientNumberKey) {
                    clientNumber++;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        /*
        this.running = true;
        try {
            this.server = new ServerSocket(port);
            System.out.println("Starting server on port " + this.port);

            //manage threads on one other thread
            manager = new Manager();

            int counter = 1; //keep track of how many clients have connected

            while (true) {
                Socket client = server.accept(); //localhost has been called

                System.out.println("Client connected from " + client.getInetAddress());

                Connection newConnection = new Connection(client, this); //make a runnable passing the client socket to the new Connection

                Thread thread = new Thread(newConnection, "Client" + counter); //make a new thread to run the connection to the client
                manager.addThread(thread, newConnection); //have the manager keep track of the threads
                thread.start(); //start the thread
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
         */
    }

    public void readFile(String filename) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        Socket client = server.accept();
        System.out.println("Client connected from " + client.getInetAddress());

        AudioFormat format = new AudioFormat(8000, 8, 1, true, true);
        TargetDataLine line;
                
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();

        int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
        byte[] buffer = new byte[bufferSize];

        int count = line.read(buffer, 0, bufferSize);

        String formatText = format.getEncoding() + "," + format.getSampleRate() + "," + format.getSampleSizeInBits()
                + "," + format.getChannels() + "," + format.getFrameSize() + "," + format.getFrameRate() + ","
                + format.isBigEndian();

        System.out.println(formatText);

        client.getOutputStream().write((byte)formatText.getBytes().length); //send format string length to the client
        client.getOutputStream().write(formatText.getBytes("UTF-8"));
//        for(byte by : formatText.getBytes()) {
//            System.out.println(by);
//        }
//        client.getOutputStream().write(-127); //stop signal

        boolean running = true;
        int counter = 0;

//        while(running) {
//            if(count > 0) {
//                client.getOutputStream().write(buffer);
//            }
//            count = line.read(buffer, 0, bufferSize);
//            counter++;
//            if(counter == 1000) {
//                running = false;
//            }
//        }
//
//        line.close();
//        client.getOutputStream().close();
//        client.close();

    }

    private class Connection implements Runnable {
        private User thisUser;

        private boolean ready;
        private boolean online;

        private boolean broadcasting; //if false
        private boolean listening;
        //private Connection listeningFrom;

//        private CircularArray queue;

        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;

        private Socket client;

        public Connection(Socket client) {
            this.client = client;
            online = true;
            ready = false;

            broadcasting = false;
            listening = false;
        }

        @Override
        public void run() {
            //set up the streams
            try {
                this.objectInputStream = new ObjectInputStream(client.getInputStream());
                this.objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            } catch(IOException io) {
                io.printStackTrace();
            }

            try {
                while (true) {
                    Message request = (Message) objectInputStream.readObject();

                    if(request.getType() == 1) {
                        //its text
                    }
                    else if (request.getType() == 2) {
                        //encoding data
                    }
                    else if (request.getType() == 3) {
                        //audio data
                    }
                    else if (request.getType() == 4) {
                        //user
                        this.updateUser(request);
                    }
                    else if (request.getType() == 5) {
                        //user list
                        this.sendUserList();
                    }
                    else if (request.getType() == 6) {
                        //a listen request
                        this.addRequest(request);
                    }
                    else if (request.getType() == 7) {
                        this.sendAllRequests();
                    }
                    else if (request.getType() == 8) {
                        //accept a request

                        Request audioRequest = (Request) request.getData();

                        synchronized (onCallKey) {
                            onCall = true;
//                            boolean copyOnCall = new Boolean(onCall);
                        }

                        synchronized (currentRequestKey) {
                            currentRequest = new Request(audioRequest);
                        }

                        if(audioRequest.getType() == 3) {
                            listening = true;
                            broadcasting = true;
                        } else if(audioRequest.getType() == 1 && audioRequest.getSender().equals(thisUser)) {
                            broadcasting = true;
                        } else {
                            listening = true;
                        }

                        if(audioRequest.getRecipient().equals(thisUser)) {
                            synchronized (connectionKey) {
                                for(int i = 0; i < connections.size(); i++) {
                                    if(connections.getUser(i).equals(audioRequest.getSender())) {
                                        if(connections.getConnection(i).getCurrentRequest().equals(audioRequest)) {
                                            if(connections.getConnection(i).getOnCall()) {
                                                //all good
                                                transferData();
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            synchronized (connectionKey) {
                                for(int i = 0; i < connections.size(); i++) {
                                    if(connections.getUser(i).equals(audioRequest.getRecipient())) {
                                        if(connections.getConnection(i).getCurrentRequest().equals(audioRequest)) {
                                            if(connections.getConnection(i).getOnCall()) {
                                                //all good
                                                transferData();
                                            }
                                        }
                                    }
                                }
                            }
                        }

//                        synchronized (requestKey) {
//                            for(Request r : requests) {
//                                if(r.equals(audioRequest)) {
//                                    //check to see if accepted
//                                    currentRequest = r;
//                                    if(r.isAccepted()) {
//                                        //send back a "hey ready to join call"
//                                        otherReady = true;
//                                        objectOutputStream.writeObject(new Message(9));
//                                    } else {
//                                        //send back a waiting on other dood
//                                        r.accept();
//                                        objectOutputStream.writeObject(new Message(8));
//                                    }
//                                }
//                            }
//                        }

                        //we need to tell other client that the call is prepared

                        //int broadOrList = (Integer) request.getData(); //1 for broad, 2 for list, 3 for both


//                        this.ready = true;
//
//                        if(otherReady && ready) {
//                            //join call
//                            //go to other method that deals with the circular array
//                            //in manager
//                        }
                        //TODO have a way to delete and deny requests

                        //wait till other user is ready to send back the ok signal
                    }
//                    else if (request.getType() == 9) {
//                        //user is waiting for the other user to connect
//                    }
                    else if (request.getType() == -1) {
                        //quit message
                        break;
                    }

                }

                //TODO show to console that this user has logged off in a better way than just catching an exception
            } catch (EOFException eofException) {
                synchronized (outputKey) {
                    System.out.println(thisUser.getUsername() + " has logged off");
                    this.online = false;
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }

        private boolean otherReady = false;

        private Request currentRequest;

        private void updateUser(Message request) throws IOException {
            User user = (User) request.getData();

            synchronized (connectionKey) {
                //System.out.println(connections.size);
                for(int i = 0; i < connections.size(); i++) {
                    if(connections.getUser(i).getUsername() == null) {
                        continue;
                    }
                    //System.out.println(connections.getUser(i).getUsername());
                    if(connections.getUser(i).getUsername().equals(user.getUsername())) {
                        objectOutputStream.writeObject(new Message(-2));
                        return;
                    }
                }
            }

            if(user.getId() == 0) {
                //init
                synchronized (clientNumberKey) {
                    thisUser = new User(clientNumber, user.getUsername());
                }
            } else {
                thisUser.setUsername(user.getUsername());
            }
            this.thisUser.setOnline(true);

            //update username
            //thisUser.setUsername(user.getUsername());

            synchronized (connectionKey) {
                connections.update(this, thisUser);
            }

            objectOutputStream.writeObject(new Message(4, thisUser));
        }

        private void sendUserList() throws IOException {
            ArrayList<User> users;

            synchronized (connectionKey) {
                users = connections.toArrayList();
            }

            objectOutputStream.writeObject(new Message(5, users));
        }

        private boolean onCall;

        public final Object onCallKey = new Object();

        private void addRequest(Message message) throws IOException {
            Request request = (Request) message.getData();

            synchronized (currentRequestKey) {
                this.currentRequest = new Request(request);
            }

            synchronized (onCallKey) {
                this.onCall = true;
            }

            synchronized (requestKey) {
                for(Request r : requests) {
                    if(request.equals(r)) {
                        //send this user to call as recipient
                        objectOutputStream.writeObject(new Message(6, request));
                        return;
                    }
                }

                requests.add(request);
            }

            //right here we are waiting for the other user to connect.

            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }

                boolean breakOutOfWhile = false;

                synchronized (connectionKey) {
                    for(int i = 0; i < connections.size(); i++) {
                        if(connections.getUser(i).equals(request.getRecipient())) {
                            if(connections.getConnection(i).getCurrentRequest().equals(request) && connections.getConnection(i).getOnCall()) {
                                breakOutOfWhile = true;
                            }
                        }
                    }
                }

                if (breakOutOfWhile) {
                    break;
                }
            }
            objectOutputStream.writeObject(new Message(6, request)); //unblocks current user

            //send to other method
            transferData();
        }

        public void writeMode() {
            while(true) {

            }
        }

        public void readMode() {
            
        }

        public void transferData() {
            if(listening && broadcasting) {
                //read mode
                //uh oh mode
            } else if (broadcasting) {
                //write mode
                writeMode();
            } else {
                //read mode
                readMode();
            }
        }

        public boolean getOnCall() {
            synchronized (onCallKey) {
                return onCall;
            }
        }

        public final Object currentRequestKey = new Object();

        public Request getCurrentRequest() {
            synchronized (currentRequestKey) {
                return currentRequest;
            }
        }

        private void sendAllRequests() throws IOException {
            ArrayList<Request> listToSend = new ArrayList<>();

            synchronized (requestKey) {
                for (Request request : requests) {
                    if (request.getSender().equals(thisUser) || request.getRecipient().equals(thisUser)) {
                        listToSend.add(request);
                    }
                }
            }

            objectOutputStream.writeObject(new Message(7, listToSend));
        }

        public boolean isOnline() {
            return online;
        }

        public void close() {
            try {
                objectOutputStream.close();
                objectInputStream.close();
                client.close();
            } catch (IOException ioException) {

            }
        }

        public boolean isReady() {
            return ready;
        }
    }

    private class Manager implements Runnable {
        //TODO implement Manager

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000); //check for end of connections every 1 second
                    synchronized (outputKey) {
                        System.out.println("Checking for logoffs");
                    }

                    /* check for connections that have terminated */

                    synchronized (connectionKey) {
                        if (connections.size() != 0) {
                            for (int i = 0; i < connections.size(); i++) {
                                if(!connections.getConnection(i).isOnline()) {
                                    connections.getConnection(i).close();
                                    //this connection is disconnected
                                    connections.getThread(i).join();
                                    connections.remove(i); //still need to implement this!!
                                }
                            }
                        }
                    }

                    /* check for connections that want to talk to each other */

                    synchronized (connectionKey) {
                        if(connections.size() != 0) {
                            for(int i = 0; i < connections.size(); i++) {
                                if(connections.getConnection(i).isReady()) {

                                }
                            }
                        }
                    }
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
    }


    /**
     * A linked list that keeps track of all the connections and their users.
     */
    private class UserList {
        private Node head;
        private int length;

        public UserList() {
            head = new Node();
            length = 0;
        }

        /**
         * Node class that makes up this list
         */
        private class Node {
            public Thread thread;
            public Connection connection;
            public User user;
            public Node link;

            public Node() {
                this.link = null;
                this.thread = null;
                this.connection = null;
                this.user = null;
            }

            public Node(Thread thread, Connection connection, User user) {
                this.thread = thread;
                this.connection = connection;
                this.user = user;
                this.link = null;
            }
        } /** End Node */

        public void add(Thread thread, Connection connection, User user) {
            Node lastNode = head;
            Node currentNode = head.link;

            while(currentNode != null) {
                lastNode = currentNode;
                currentNode = currentNode.link;
            }

            currentNode = new Node(thread, connection, user);
            lastNode.link = currentNode;

            this.length++;
        }

        public int size() {
            return length;
        }

        public void update(Connection connectionToUpdate, User updatedUser) {
            Node currentNode = head.link;

            try {
                while (currentNode.connection != connectionToUpdate && currentNode != null) {
                    currentNode = currentNode.link;
                }

                currentNode.user = updatedUser;
            } catch (NullPointerException nullPointerException) {
                nullPointerException.printStackTrace();
            }
        }

        public ArrayList<User> toArrayList() {
            ArrayList<User> users = new ArrayList<User>();


            Node lastNode = head;
            Node currentNode = head.link;

            while(currentNode != null) {
                users.add(currentNode.user);

                lastNode = currentNode;
                currentNode = currentNode.link;
            }

            return users;
        }

        private Node get(int index) {
            if(index > length - 1) {
                throw new ArrayIndexOutOfBoundsException();
            }

            Node currentNode = head.link;

            for(int i = 0; i < index; i++) {
                currentNode = currentNode.link;
            }

            return currentNode;
        }

        public User getUser(int index) {
            Node element = get(index);

            return element.user;
        }

        public Connection getConnection(int index) {
            Node element = get(index);

            return element.connection;
        }

        public Thread getThread(int index) {
            Node element = get(index);

            return element.thread;
        }

        public void remove(int index) {
            //TODO implement delete method
        }

    }

    public static void main(String[] args) throws Exception {
        //start server
        int port = Integer.parseInt(args[0]); //get the port number

        Server server = new Server(port); //make the server instance

        server.runServer(); //run the server
    }
}

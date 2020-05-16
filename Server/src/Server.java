import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink/
 * @version 1.2.0
 */

public class Server {
    private int port;
    private ServerSocket server;
    private ServerSocket audioServer;
    private Manager manager;

    private AudioFormat format;

    public static boolean readQueue(int id, byte[] byteToPopulate) {
        synchronized (connectionKey) {
            for(int i = 0; i < connections.size(); i++) {
                Connection connection = connections.getConnection(i);
                if(connection.getID() == id) {
                    byteToPopulate[0] = connection.readQueue();
                    return true;
                }
            }
        }

        return false;
    }

    private int clientNumber; //stores the amount of users logged on

    /** All keys for the threads to enter synchronized blocks */
    private static final Object outputKey = new Object();
    private static final Object connectionKey = new Object();
    private static final Object clientNumberKey = new Object();
    private static final Object callKey = new Object();

    private Thread managerThread;

    private static UserList connections;

    public Server(int port) {
        this.port = port;
        clientNumber = 0;
    }

    public void print(Object o) {
        synchronized (outputKey) {
            System.out.println(o);
        }
    }

    public void runServer() {

        format = new AudioFormat(44100.0f, 16, 1, true, true);

        connections = new UserList();

        manager = new Manager();

        //make a thread that manages all the threads.
        managerThread = new Thread(manager, "Manager-Thread");

        managerThread.start();

        //make the server
        try {
            this.server = new ServerSocket(port);
            this.audioServer = new ServerSocket(port + 1);
            System.out.println("Server started on port " + port + " and " + (port + 1));
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

                Socket audioSocket = audioServer.accept();

                Connection newConnection = new Connection(client, audioSocket, counter); //make a runnable passing the client socket to the new Connection

                Thread thread = new Thread(newConnection, "Client" + counter); //make a new thread to run the connection to the client

                synchronized (connectionKey) {
                    connections.add(thread, newConnection, new User(0));
                }

                thread.start(); //start the thread

                print("New Connection Thread started: " + thread.getName());

                counter++;

                synchronized (clientNumberKey) {
                    clientNumber++;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private class Connection implements Runnable {
        private User thisUser;

        private CircularArray queue;

        private AtomicBoolean mute;

        private boolean online;

        private AtomicInteger otherConnectionID;
        private AtomicBoolean queueReaderRunning = new AtomicBoolean(false);
        private AtomicBoolean queueRunning = new AtomicBoolean(true);
        private AtomicBoolean running = new AtomicBoolean(true);
        private AtomicBoolean closed;

        private Socket client;
        private Socket audioSocket;

        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;

        private Thread queueWriter;
        private Thread queueReader;

        public final Object queueKey = new Object();

        private int connectionNumber;


        public Connection(Socket client, Socket audioSocket, int connectionNumber) {
            this.client = client;
            this.audioSocket = audioSocket;
            //this.server = server;
            this.connectionNumber = connectionNumber;
            online = true;
            this.otherConnectionID = new AtomicInteger(0);
            mute = new AtomicBoolean(true);
            closed = new AtomicBoolean(false);
            int frames = 5;

            this.queue = new CircularArray(frames * ((int) format.getSampleRate() * format.getFrameSize()));

            queueWriter = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //BufferedInputStream bufIn = new BufferedInputStream(audioSocket.getInputStream());

                        int chunkSize = 1024;

                        byte[] oneByte = new byte[chunkSize];

                        while (queueRunning.get()) {
//                                synchronized (queueKey) {
                            audioSocket.getInputStream().read(oneByte, 0, chunkSize);

                            for (int i = 0; i < oneByte.length; i++) {
                                queue.write(oneByte[i]);
                            }
//                                }
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }, "AudioWriter" + this.connectionNumber);

            queueWriter.start();

            queueReader = new Thread(new Runnable() {
                private boolean read(byte[] buffer) {
                    byte[] b = {0};
                    for (int i = 0; i < buffer.length; i++) {
                        if (!readOtherQueue(b)) {
                            return false;
                        } else {
                            buffer[i] = b[0];
                        }
                    }
                    return true;
                }
                //TODO maybe what reduces latency is the synchronization of when the other user decides to start listening to the queue. The queue reader and writer might be moving at the same speed and therefore the latency is caused by the difference between the two
                @Override
                public void run() {
                    try {
                        BufferedOutputStream bufOut = new BufferedOutputStream(audioSocket.getOutputStream());

                        int chunkSize = 1024;

                        byte[] oneByte = new byte[chunkSize];

                        while (queueReaderRunning.get()) {
//                                synchronized (queueKey) {
                            if (!read(oneByte)) {
                                break;
                            } else {
                                audioSocket.getOutputStream().write(oneByte, 0, chunkSize);
                            }
//                                }
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }, "AudioReader" + this.connectionNumber);
        }

        @Override
        public void run() {
            //set up the streams
            try {
                this.objectInputStream = new ObjectInputStream(client.getInputStream());
                this.objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            } catch (IOException io) {
                io.printStackTrace();
            }

            try {
                while (running.get()) {
                    Message request = (Message) objectInputStream.readObject();

                    if (request.getType() == 1) {
                        //its text
                    }
//                    else if (request.getType() == 2) {
//                        //encoding data
//                    }
//                    else if (request.getType() == 3) {
//                        //audio data
//                    }
                    else if (request.getType() == 4) {
                        //user
                        this.updateUser(request);
                        print(Thread.currentThread() + ": updated their username ");
                    } else if (request.getType() == 5) {
                        //user list
                        this.sendUserList();
                        print(Thread.currentThread() + ": requested all the users connected");
//                    }
//                    else if (request.getType() == 6) { //DEPRECATED
//                        //the client wants to listen to another user
//                        //this.addRequest(request);
//                        this.otherConnectionID.set((Integer)request.getData());
//                    }
//                    else if (request.getType() == 7) {
//                        this.sendAllRequests(); //DEPRECATED
//                    }
//                    else if (request.getType() == 8) {
//                        //user accepted request
//                        this.acceptRequest((Call)request.getData()); //DEPRECATED
                    } else if (request.getType() == 60) { //start getting data from other user
                        otherConnectionID.set((Integer) request.getData());
                        queueReaderRunning.set(true);
                        queueReader.start();
                        print(Thread.currentThread() + ": requested the audio from the user with id " + this.otherConnectionID.get());
                    } else if (request.getType() == 70) { //stop read from other conn thread and
                        queueReaderRunning.set(false);
                        try {
                            queueReader.join();
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }

                        otherConnectionID.set(0);
                        audioSocket.getOutputStream().flush();

                        print(Thread.currentThread() + ": has stopped listening to the audio from the user with id " + this.otherConnectionID.get());
                    } else if (request.getType() == 80) {
                        this.mute.set(true);
                        System.out.println(" is muted");
                        objectOutputStream.writeObject(new Message(80));
                    } else if (request.getType() == 85) {
                        this.mute.set(false);
                        System.out.println(" is unmuted");
                        objectOutputStream.writeObject(new Message(85));
                    } else if (request.getType() == -1) {
                        //quit message
                        this.close();
                        this.online = false;
                        break;
                    }

                }

                //TODO show to console that this user has logged off in a better way than just catching an exception
                //TODO join threads when done with them
            } catch (EOFException eofException) {
                synchronized (outputKey) {
                    System.out.println(thisUser.getUsername() + " has logged off");
                    this.online = false;
                    this.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }

        private void updateUser(Message request) throws IOException {
            User user = (User) request.getData();

            synchronized (connectionKey) {
                //System.out.println(connections.size);
                for (int i = 0; i < connections.size(); i++) {
                    if (connections.getUser(i).getUsername() == null) {
                        continue;
                    }
                    //System.out.println(connections.getUser(i).getUsername());
                    if (connections.getUser(i).getUsername().equals(user.getUsername())) {
                        objectOutputStream.writeObject(new Message(-2));
                        return;
                    }
                }
            }

            if (user.getId() == 0) {
                //init
                synchronized (clientNumberKey) {
                    thisUser = new User(clientNumber, user.getUsername());
                }
            } else {
                thisUser.setUsername(user.getUsername());
            }
            this.thisUser.setOnline(true);

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

        public byte readQueue() {
            if (mute.get()) {
                return 0;
            }

            synchronized (queueKey) {
                return queue.read();
            }
        }


        private boolean readOtherQueue(byte[] byteToPopulate) {
            return Server.readQueue(otherConnectionID.get(), byteToPopulate);
        }

        public boolean isOnline() {
            return online;
        }

        public int getID() {
            return thisUser.getId();
        }

        /**
         * Closes all objects that need to be closed
         */

        public void close() {
            try {
                this.closed.set(true);
                objectOutputStream.close();
                objectInputStream.close();
                client.close();
                audioSocket.close();

                queueRunning.set(false);
                running.set(false);

                queueReaderRunning.set(false);
                queueRunning.set(false);

                audioSocket.getInputStream().close();
                audioSocket.getOutputStream().close();
                audioSocket.close();

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        public boolean isClosed() {
            return closed.get();
        }

    }

    private class Manager implements Runnable {
        //TODO implement Manager

        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(100); //check for end of connections every 1 second
                    synchronized (outputKey) {
                        //System.out.println("Checking for logoffs");
                    }

                    /* check for connections that have terminated */

                    synchronized (connectionKey) {
                        if (connections.size() != 0) {
                            for (int i = 0; i < connections.size(); i++) {
                                Connection con = connections.getConnection(i);
                                if(!con.isOnline()) {
                                    if(!con.isClosed()) {
                                        con.close();
                                    }
                                    //this connection is disconnected
                                    connections.getThread(i).join();
                                    connections.remove(i); //TODO remove !!!! still need to implement this!!
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

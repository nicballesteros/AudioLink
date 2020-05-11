import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink
 * @version 1.1.0
 */

public class Client {
    private Socket socket;
    private String host;
    private int port;
    private BufferedReader bufferedReader;
    private BufferedOutputStream writer;
    private BufferedInputStream reader;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private Call currentCall;

    private Scanner scanner;

    private ArrayList<User> users;

    private Thread recorder;
    private Thread listener;

    private AudioRecorder microphone;
    private AudioListener speaker;

    private User me;

    public Client() {
        this.host = "localhost";
        this.port = 8080;

        try {
            this.socket = new Socket(host, port);
            //bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            objectInputStream = new ObjectInputStream(this.socket.getInputStream());
            objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            this.socket = new Socket(host, port);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());

            scanner = new Scanner(System.in);

            this.initiateUser(); //initiate the user and set me to something other than null;

            this.mainMenu(); // run the main menu




            //bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
//            this.choose();
//            this.writer = new BufferedOutputStream(socket.getOutputStream());
//            this.reader = new BufferedInputStream(socket.getInputStream());
//            this.choose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void initiateUser() throws IOException {
        System.out.println("Welcome to AudioLink");
        System.out.println("Creator: Nic Ballesteros");
        System.out.println("Let's set up your profile! (Don't worry you can change this later)");
        while(true) {
            System.out.print("Enter a username: ");

            String username = scanner.nextLine();

            System.out.println("\nHello " + username);
            System.out.println("Sending your information to the world wide web\n");

            me = new User(username); //me only has a username right now. will get id from server response.

            //send username to server and get an id
            objectOutputStream.writeObject(new Message(4, me));

            try {
                Message response = (Message) objectInputStream.readObject();
                if(response.getType() != -2) {
                    me = (User) response.getData(); //update the "me" object with a User object that has an id and username
                    break;
                } else {
                    System.out.println("That username is taken. Please choose another!");
                }
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
    }

    public void updateUser() throws IOException {
        System.out.println("Update User Profile: ");
        System.out.println("Current Username: " + me.getUsername());

        boolean changeUser = false;

        while(true) {
            System.out.print("Would you like to change it? (y/n) ");
            scanner.nextLine();
            String ans = scanner.nextLine();

            //TODO make this more robust.
            if(ans.equals("y")) {
                changeUser = true;
                break;
            } else if (ans.equals("n")) {
                break;
            }
        }

        if(changeUser) {
            while(true) {
                System.out.print("Enter a new username: ");

                String username = scanner.nextLine();

                //me.setUsername(username);

                objectOutputStream.writeObject(new Message(4, new User(0, username))); //since me should have an id other than 0 the server will realize that this is an initialized user and it just needs to update the username field of the User object

                try {
                    Message response = (Message) objectInputStream.readObject();
                    //TODO check that the id matches

                    if (response.getType() != -2) {
                        User userResponseFromServer = (User) response.getData();
//                        if (((User) response.getData()).getUsername().equals(me.getUsername())) {
//                            //all good
//                            System.out.println("Profile updated!");
//                        } else {
//                            //exit the program there has been an error
//                            System.out.println("There's been an error");
//                        }

                        me = userResponseFromServer;
                        System.out.println("Changed Username!\n");
                        break;
                    } else {
                        System.out.println("That username is taken. Please choose another!");
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    classNotFoundException.printStackTrace();
                }
            }
        } else {
            System.out.println("Returning to main menu.\n");
        }
    }

    private void printOnlineUsers(boolean showThisUser) {
        for (User user : users) {
            if(showThisUser || !showThisUser && !user.equals(me)) {
                System.out.println("Id: " + user.getId() + " Username: " + user.getUsername() + " Online: " + user.isOnline()); //print out each user
            }
        }

        System.out.println(); //add an extra \n
    }

    public void getOnlineUsers() throws IOException {
        if(users == null) {
            System.out.println("Retrieving User list ... ");
        } else {
            System.out.println("Updating User list ... ");
        }
        //TODO write a caution message that says that the user that the client is looking for might have changed their username

        objectOutputStream.writeObject(new Message(5));

        try {
            Message response = (Message) objectInputStream.readObject();
            if(response.getType() != 5) {
                //throw an exception
            }

            //TODO this can cause a null pointer exception
            users = (ArrayList<User>) response.getData();

            System.out.println("Done!");
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        }
    }

    public void getIncomingCalls() throws IOException {
        objectOutputStream.writeObject(new Message(7));

        try {
            Message message = (Message) objectInputStream.readObject();
            ArrayList<Call> calls = (ArrayList<Call>) message.getData();

            if(calls.size() > 0) {
                System.out.println("\nCurrent Requests: ");
                int num = 0;

                for (Call call : calls) {
                    num++;

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(num);
                    stringBuilder.append(") From: ");
                    stringBuilder.append(call.getSender().getUsername());
                    stringBuilder.append(" To: ");
                    stringBuilder.append(call.getRecipient().getUsername());
                    stringBuilder.append(" Type: ");

                    if (call.getType() == 1) {
                        stringBuilder.append("Broadcast\n");
                    } else if (call.getType() == 2) {
                        stringBuilder.append("Listen\n");
                    } else if (call.getType() == 3) {
                        stringBuilder.append("Listen and Broadcast\n");
                    }

                    System.out.println(stringBuilder.toString());
                }

                while(true) {
                    System.out.print("Would you like to accept a request? (y/n) ");
                    scanner.nextLine();
                    String input = scanner.nextLine();

                    if (input.equals("y")) {
                        System.out.println("Which request number would you like to accept?");
                        while(true) {
                            System.out.print("Request: ");

                            int requestNumber = scanner.nextInt();

                            if(requestNumber > 0 && requestNumber <= calls.size()) {
                                //accept that or deny the request

                                while(true) {
                                    System.out.print("Would you like to accept or deny this request? (a/d) ");
                                    scanner.nextLine();
                                    String acceptOrDeny = scanner.nextLine();

                                    if(acceptOrDeny.equals("a")) {
                                        currentCall = calls.get(requestNumber - 1);
                                        System.out.println("Entering mode " + currentCall.getType());
                                        objectOutputStream.writeObject(new Message(8, currentCall));
                                        objectInputStream.readObject(); //TODO do something here

                                        enterCall();

                                        break;
                                    } else if (acceptOrDeny.equals("d")){
                                        System.out.println("Deleting Request");
                                        //TODO delete the request
                                    } else {
                                        System.out.println("Please choose a or d");
                                    }
                                }
                                break;
                            } else {
                                System.out.println("That is not a valid request number. Please try again.");
                            }
                        }
                        break;
                    } else if (input.equals("n")) {
                        break;
                    }
                }

            } else {
                System.out.println("No requests to this user\n");
            }
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        }
    }

    public void makeCallRequest() throws IOException {
        getOnlineUsers(); //show the user all the online users
        printOnlineUsers(false);

        if(users.size() == 1) {
            System.out.println("No users to call. Returning to main menu.\n");
            return;
        }

        System.out.println("Enter the id of the user you'd like to call");
        System.out.print("ID (Enter -1 to quit): ");

        int id = scanner.nextInt();

        if(id == -1) {
            System.out.println("Returning to main menu\n");
            return;
        }

        if(id == me.getId()) {
            System.out.println("Cannot call yourself\n");
            return;
        }

        User to = null;

        for(User user : users) {
            if(user.getId() == id) {
                to = user;
//                System.out.println("Found user");
                break;
            }
        }

        if(to != null) { //only if there exists a user that matches the id
            int choice = 0;

            while (choice <= 0) {
                System.out.println("Would you like to ... ");
                System.out.println("1) Send your mic");
                System.out.println("2) Listen to their mic ");
                System.out.println("3) Have a call");
                System.out.println("4) Exit to Main Menu");

                choice = scanner.nextInt();
            }

            Call call = new Call(me, to, choice);
            //System.out.println(choice);
            if (choice >= 1 && choice <= 3) {
                //send that request to the server so that the user that has a request can see it.
                objectOutputStream.writeObject(new Message(6, call));
                System.out.println("Calling"); //TODO add some ringing animations
//NOTE there will not be a way to get out of the call
                try {
                    Message message = (Message) objectInputStream.readObject();
                    if (message.getType() == 6) { //hang here
                        System.out.println(to.getUsername() + " has answered\n");
                        currentCall = (Call) message.getData();
                        enterCall();
                    } else {
                        System.out.println("error");
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    classNotFoundException.printStackTrace();
                }
            } else {
                System.out.println("Returning to main menu\n");
            }
        } else {
            System.out.println("There is no user with that id! Returning to main menu.\n");
        }
    }

    public void mainMenu() {
        while(true) {
            System.out.println("Main Menu: ");
            System.out.println("1: Edit your profile (username)");
            System.out.println("2: View Online Users");
            System.out.println("3: Make a call to another user");
            System.out.println("4: View incoming calls sent to this user");
            System.out.println("5. Exit program");

            //TODO have an instruction option

            int choice = scanner.nextInt();
            try {
                if (choice == 1) {
                    //change username
                    this.updateUser();
                } else if (choice == 2) {
                    //ask for a list of online users
                    this.getOnlineUsers();
                    this.printOnlineUsers(true);
                } else if (choice == 3) {
                    //make a call to another user
                    this.makeCallRequest();
                } else if (choice == 4) {
                    //view current requests to this user
                    this.getIncomingCalls();
                } else if (choice == 5) {
                    System.out.println("Goodbye!");
                    break; //exits program
                } else {
                    //chose something thats not on the list
                    System.out.println("Please choose a number that is on the main menu. \n");
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            //TODO make sure that if the value inputted is not an int, show main menu again.


        }
    }

    public void enterCall() {
        System.out.println("Type \"end\" to stop call");

        //TODO loop till call is accepted

        if(currentCall.getSender().equals(me) && currentCall.getType() == 1) {
            //start a thread that listens to this users mic and sends it over to server
            microphone = new AudioRecorder(objectOutputStream, objectInputStream);
            recorder = new Thread(microphone);
            recorder.start();
//            listener = new Thread(new AudioRecorder(objectOutputStream));
        } else if (currentCall.getSender().equals(me) && currentCall.getType() == 2) {
            //start a thread that takes server data and sends to speakers
            speaker = new AudioListener(objectInputStream, objectOutputStream);
            listener = new Thread(speaker);
            listener.start();
        } else {
            //start both.
            microphone = new AudioRecorder(objectOutputStream, objectInputStream);
            recorder = new Thread(microphone);

            recorder.start();

            speaker = new AudioListener(objectInputStream, objectOutputStream);
            listener = new Thread(speaker);

            listener.start();
        }

        String userInput = "";

        while(!userInput.equals("end")) {
            System.out.println("Listening and Recording");
            if(scanner.hasNextLine()) {
                userInput = scanner.nextLine();
            }
            if(microphone != null) {
                if(!microphone.isRunning()) {
                    break;
                }
            }

            if(speaker != null) {
                if(!speaker.isRunning()) {
                    break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }

        System.out.println("Call ended");

        if(microphone != null) {
            microphone.stopRecording();
        }

        if (speaker != null) {
            speaker.stopListening();
        }
    }

    public void choose() throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
//        if(bufferedReader.readLine().equals("200 success")) {
//            System.out.println("Connected to Server" );
//            System.out.println("Hostname: " + socket.getInetAddress());
//            System.out.println("Port: " + socket.getPort());
//        } else {
//            System.out.println("Error connecting to a Server");
//        }
//        bufferedReader.close();

/*
        try {
            Message connectionResponse = (Message) objectInputStream.readObject();

            if(connectionResponse.getType() != 1) {
                //throw an exception
            }

            System.out.println("Connected to server");
            System.out.print("Please enter a username: ");

            //get username
            String username = scanner.nextLine();

            //send username to server
            objectOutputStream.writeObject(new Message(4, username));

            System.out.println("\nWelcome " + username);
            System.out.println("Getting a list of online users");
            //get list of online users.
            objectOutputStream.writeObject(new Message(1, "list request", 200));
            Message onlineUsers = (Message) objectInputStream.readObject();

            if(onlineUsers.getType() != 5 || onlineUsers.getCode() != 200) {
                //throw exception
            }

            users = (ArrayList<User>) onlineUsers.getData();



        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        }
*/


        /*

        while(true) {
            byte[] res = new byte[8];
            reader.read(res, 0, res.length);

            if(res[0] == "OK".getBytes()[0] && res[1] == "OK".getBytes()[1]) {
                break;
            }
        }

        System.out.println("got ok");

        byte[] size = new byte[1];
        reader.read(size, 0, 1);
        System.out.println("size:" + size[0]);
        byte[] formatBytes = new byte[size[0]];

        reader.read(formatBytes, 0, size[0]);

        for(byte b : formatBytes) {
            System.out.println(b);
        }

        String formatString = new String(formatBytes, StandardCharsets.UTF_8);
        formatString.trim();
        System.out.println(formatString);
        for(byte b : formatString.getBytes()) {
            System.out.println(b);
        }



        //put in new function


        AudioFormat.Encoding encoding;

        String[] params = formatString.split(",");
        if(params[0].equalsIgnoreCase("pcm_signed")) {
            encoding = AudioFormat.Encoding.PCM_SIGNED;
        }
        else if(params[0].equalsIgnoreCase("pcm_unsigned")) {
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        }
        else if(params[0].equalsIgnoreCase("pcm_float")) {
            encoding = AudioFormat.Encoding.PCM_FLOAT;
        }
        else if(params[0].equalsIgnoreCase("ulaw")) {
            encoding = AudioFormat.Encoding.ULAW;
        }
        else if(params[0].equalsIgnoreCase("alaw")) {
            encoding = AudioFormat.Encoding.ALAW;
        }
        else {
            throw new IllegalArgumentException("Encoding string was not of enum AudioFormat.Encoding");
        }

        float sampleRate = Float.parseFloat(params[1]);
        int sampleSizeInBits = Integer.parseInt(params[2]);
        int channels = Integer.parseInt(params[3]);
        int frameSize = Integer.parseInt(params[4]);
        float frameRate = Float.parseFloat(params[5]);
        boolean bigEndian = Boolean.parseBoolean(params[6]);

        AudioFormat audioFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);

        int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();

        byte[] buffer = new byte[bufferSize];
        int count = reader.read(buffer, 0, bufferSize);

        while(count > 0) {
            InputStream is = new ByteArrayInputStream(buffer);
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
            Thread.sleep(clip.getMicrosecondLength());
            clip.stop();
        }

        while(true);
*/
    }

    public static void main(String[] args) throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);

        String host;
        int port;
        Client client;

        switch(args.length) {
            case 1:
                host = args[0];
                port = 80;
                break;
            case 2:
                host = args[0];
                port = Integer.parseInt(args[1]);
                break;
            default:
                System.out.println("Enter Hostname: ");
                host = scanner.nextLine();
                System.out.println("Enter Port: ");
                port = scanner.nextInt();
                scanner.nextLine();
                break;
        }

        client = new Client(host, port);

//        try {
//            client.choose();
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
//        try {
//            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
//            Scanner scanner = new Scanner(System.in);
//
//            String line = scanner.nextLine();
//            while(!line.equalsIgnoreCase("exit")) {
//                pw.println(line);
//                pw.flush();
//                line = scanner.nextLine();
//            }
//            scanner.close();
//            pw.close();
//            socket.close();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private class AudioRecorder implements Runnable {
        private boolean running;

        public final Object runningKey = new Object();

        private ObjectOutputStream objectOutputStream;
        private ObjectInputStream objectInputStream;

        private boolean solo;

        public AudioRecorder(ObjectOutputStream objectOutputStream) { // constructor run when normal call
            running = true;
            this.objectOutputStream = objectOutputStream;
            this.objectInputStream = null;
            solo = false;
        }

        public AudioRecorder(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
            //constructor run when solo
            running = true;
            this.objectInputStream = objectInputStream;
            this.objectOutputStream = objectOutputStream;
            solo = true;
        }

        @Override
        public void run() {
            try {
                //send server the encoding of the mic
                RecordingFormat recordingFormat = new RecordingFormat(8000, 8, 1, true, true);
                AudioFormat format = recordingFormat.getAudioFormat();

                //String formatString = "8000,8,1,true,true";

                objectOutputStream.writeObject(new Message(20, recordingFormat));

                TargetDataLine line;

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);

                line.open(format);
                line.start();

                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte[] buffer = new byte[bufferSize];

                //File file = new File("data.dat");
                //FileWriter fw = new FileWriter(file);


                while(true) {
                    System.out.println("Recording");
                    //do a record
                    //send to server a packet
                    int count = line.read(buffer, 0, bufferSize);
                    //TODO check for an error. If count is not as big as buffersize
//                    for(int i = 0; i < bufferSize; i++) {
//                        fw.write(buffer[i] + " ");
//                    }
//                    fw.write("End Buffer");
                    try {
                        //write the audio data from the mic to the server
                        objectOutputStream.write(buffer);
                        objectOutputStream.write(1);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                    if(solo) {
                        //read ok symbol
                        try {
                            Message message = (Message) objectInputStream.readObject();
                            if(message.getType() == 30) {
                                byte okSig = (Byte) message.getData();
                                if(okSig != 1) {
                                    break; //this is a signal to stop the thread. if other person hangs up
                                }
                            }
                        } catch (ClassNotFoundException classNotFoundException) {
                            classNotFoundException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    } else {
                        try {
                            objectInputStream.readObject(); // TODO check for errors
                        } catch (ClassNotFoundException classNotFoundException) {
                            classNotFoundException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }

                    synchronized (runningKey) {
                        if(!running) {
                            objectOutputStream.write(0);
                            break;
                        }
                    }
            }

            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        public void stopRecording() {
            synchronized (runningKey) {
                this.running = false;
            }
        }

        public boolean isRunning() {
            synchronized (runningKey) {
                return this.running;
            }
        }
    }

    private class AudioListener implements Runnable {

        private boolean running;
        public final Object runningKey = new Object();
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        private boolean solo;

        public AudioListener(ObjectInputStream objectInputStream) {
            this.running = true;
            this.objectInputStream = objectInputStream;
            this.objectInputStream = null;
            solo = false;
        }

        public AudioListener(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) {
            this.running = true;
            this.objectInputStream = objectInputStream;
            this.objectOutputStream = objectOutputStream;
            solo = true;
        }

        @Override
        public void run() {
            try {
                //get encoding
                Message message = (Message) objectInputStream.readObject();
                while(message.getType() != 20) {
                    message = (Message) objectInputStream.readObject();
                }

                RecordingFormat recordingFormat = (RecordingFormat) message.getData();

                AudioFormat format = recordingFormat.getAudioFormat();

                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte[] buffer = new byte[bufferSize];

                File file = new File("inputstream.dat");
                FileWriter writer = new FileWriter(file);

                while (true) {
                    //listen to server and play in speaker

                    objectInputStream.read(buffer, 0, buffer.length);
                    for(int i = 0; i < bufferSize; i++) {
                        writer.write(buffer[i]);
                    }
                    writer.write("\n");

                    writer.close();
                    //if (message.getType() == 40) {
                        //buffer =  message.getData();
                        //TODO there might be a continuity issue right here because the client has to wait for the clip to stop before it can get the next frame
                        //play to speakers
                        InputStream is = new ByteArrayInputStream(buffer);
                        AudioInputStream ais = AudioSystem.getAudioInputStream(is);
                        Clip clip = AudioSystem.getClip();
                        clip.open(ais);
                        clip.start();
                        //Thread.sleep(clip.getMicrosecondLength());
                        clip.stop();

                    //} else if(message.getType() == 30) {
                        /*if((Byte) message.getData() != 1) {
                            break;
                        }
                    //}*/

                    synchronized (runningKey) {
                        if (!this.running) {
                            break;
                        }
                    }
                }

                if (solo) {
                    objectOutputStream.writeObject(new Message(100)); //hang up call
                }
            } catch (IOException ioException) {
                ioException.printStackTrace(); //TODO if exception is ever thrown in this stage shut down everything... like all calls
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            } catch (UnsupportedAudioFileException unsupportedAudioFileException) {
                unsupportedAudioFileException.printStackTrace();
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
//            } catch (InterruptedException interruptedException) {
//                interruptedException.printStackTrace();
//            }

        }

        public void stopListening() {
            synchronized (runningKey) {
                this.running = false;
            }
        }

        public boolean isRunning() {
            synchronized (runningKey) {
                return this.running;
            }
        }
    }
}

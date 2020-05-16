import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.sound.sampled.AudioSystem.getMixer;
import static javax.sound.sampled.AudioSystem.getMixerInfo;

/**
 * @author Nic Ballesteros
 * @repo https://github.com/nicballesteros/AudioLink
 * @version 1.2.0
 */

public class Client {
    private String host;
    private int port;

    private Socket socket;
    private Socket audioSocket;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private Scanner scanner;

    private ArrayList<User> users;

    private Thread recorder;
    private Thread listener;

    private AudioRecorder microphone;
    private AudioListener speaker;

    private User me;

    private AudioFormat format;

    private Mixer mixer;
    private TargetDataLine microphoneLine;
    private SourceDataLine speakerLine;

    public Client() {
        this.host = "localhost";
        this.port = 8080;

        try {
            this.socket = new Socket(host, port);
            objectInputStream = new ObjectInputStream(this.socket.getInputStream());
            objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private RecordingFormat recordingFormat;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            this.recordingFormat = new RecordingFormat(44100.0f, 16, 1, true, true);
            this.format = recordingFormat.getAudioFormat();

//            this.mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[10]);

//            this.microphoneLine = (TargetDataLine) mixer.getLine(mixer.getTargetLineInfo()[0]); for testing

//            this.speakerLine = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]); for testing

            this.socket = new Socket(host, port);
            this.audioSocket = new Socket(host, (port + 1));

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());

            microphone = new AudioRecorder(audioSocket.getOutputStream());

            recorder = new Thread(microphone);

            speaker = new AudioListener(audioSocket.getInputStream());
            listener = new Thread(speaker);

            scanner = new Scanner(System.in);

            this.initiateUser(); //initiate the user and set me to something other than null;

            this.mainMenu(); // run the main menu
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

        this.setAudioDevices();

        System.out.println("Please keep in mind that your microphone is currently muted");
        System.out.println("If you want someone to hear you, unmute yourself in the main menu");

        recorder.start();
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
                        if (((User) response.getData()).getUsername().equals(me.getUsername())) {
                            //all good
                            System.out.println("Profile updated!");
                        } else {
                            //exit the program there has been an error
                            System.out.println("There's been an error");
                        }

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

    public void muteMe() throws IOException, ClassNotFoundException {
        objectOutputStream.writeObject(new Message(80));
        if (((Message) objectInputStream.readObject()).getType() == 80) {
            System.out.println("You are Muted");
        } else { //type 81
            System.out.println("You are not muted");
        }
    }

    public void unmuteMe() throws IOException, ClassNotFoundException {
        objectOutputStream.writeObject(new Message(85));
        if (((Message) objectInputStream.readObject()).getType() == 85) {
            System.out.println("You not are Muted");
        } else { //type 81
            System.out.println("You still muted");
        }
    }

    public void listenToAudio() throws IOException {
        //show the client the available users they can connect to
        this.getOnlineUsers();
        this.printOnlineUsers(false);

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
            System.out.println("Cannot listen to yourself\n");
            return;
        }

        User to = null;

        for(User user : users) {
            if(user.getId() == id) {
                to = user;
                break;
            }
        }

        if(to != null) { //only if there exists a user that matches the id
            objectOutputStream.writeObject(new Message(60, id));
            listener.start();
        } else {
            System.out.println("There is no user with that id! Returning to main menu.\n");
        }
    }

    private void stopListenToAudio() throws IOException, InterruptedException {
        objectOutputStream.writeObject(new Message(70));
        speaker.stopListening();
        listener.join();
    }

    /**
     *
     * @return how many different mixers there are connected to the machine
     */

    private int listMixers() {
        Mixer.Info[] mixerInfo = getMixerInfo();

        for(int i = 0; i < mixerInfo.length; i++) {
            System.out.println((i + 1) + ": " + mixerInfo[i].getName());
        }

        return mixerInfo.length;
    }

    private void setAudioDevices() {
        System.out.println("Please set your microphone: ");
        int mixerNum = -1;
        while(true) {
            int mixers = listMixers();

            mixerNum = scanner.nextInt();

            if(mixerNum > mixers || mixerNum < 1) {
                System.out.println("That was not an option, returning to the main menu");
            } else {
                break;
            }
        }
        try {
            mixer = getMixer(getMixerInfo()[mixerNum - 1]);
            microphoneLine = (TargetDataLine) mixer.getLine(mixer.getTargetLineInfo()[0]);
        } catch (LineUnavailableException lineUnavailableException) {
            System.out.println("Please try again");
            setAudioDevices();
        }

        System.out.println("Please set your speaker");

        while(true) {
            int mixers = listMixers();

            mixerNum = scanner.nextInt();

            if(mixerNum > mixers || mixerNum < 1) {
                System.out.println("That was not an option, returning to the main menu");
            } else {
                break;
            }
        }
        try {
            mixer = getMixer(getMixerInfo()[mixerNum - 1]);
            speakerLine = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]);
        } catch (LineUnavailableException lineUnavailableException) {
            System.out.println("Please try again");
            setAudioDevices();
        }
    }

    private void changeAudioDevice() throws IOException {
        System.out.println("What type of audio device would you like to change?");
        System.out.println("1. Microphone");
        System.out.println("2. Speaker");
        System.out.println("3. Exit");

        int choice = scanner.nextInt();

        if(choice == 1) {
            microphone.stopRecording();
            audioSocket.getOutputStream().flush();

            int mixers = 0;
            int mixerNum = -1;

            while(true) {

                System.out.println("Which device would you like to listen from");

                mixers = listMixers();

                mixerNum = scanner.nextInt();

                if(mixerNum > mixers || mixerNum < 1) {
                    System.out.println("That was not an option, returning to the main menu");
                } else {
                    break;
                }
            }

            mixer = getMixer(getMixerInfo()[mixerNum - 1]); //Get a new mixer based on user input

            try {
                microphoneLine = (TargetDataLine) mixer.getLine(mixer.getTargetLineInfo()[0]);
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            } catch (ClassCastException classCastException) {
                System.out.println("That is not a speaker");
                changeAudioDevice();
            }

            microphone = new AudioRecorder(audioSocket.getOutputStream());
            recorder = new Thread(microphone);
        } else if (choice == 2) {
            int mixers = 0;
            int mixerNum = -1;

            speaker.stopListening();


            while(true) {
                audioSocket.getOutputStream().flush();

                System.out.println("Which device would you like to listen from");

                mixers = listMixers();

                mixerNum = scanner.nextInt();

                if(mixerNum > mixers || mixerNum < 1) {
                    System.out.println("That was not an option, returning to the main menu");
                } else {
                    break;
                }
            }

            mixer = getMixer(getMixerInfo()[mixerNum - 1]); //Get a new mixer based on user input

            try {
                speakerLine = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]);
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            } catch (ClassCastException classCastException) {
                System.out.println("That is not a speaker");
                changeAudioDevice();
            }

            speaker = new AudioListener(audioSocket.getInputStream());
            listener = new Thread(speaker, "Listener");
        } else {
            changeAudioDevice();
        }
    }

    public void mainMenu() {
        while(true) {
            System.out.println("Main Menu: ");
            System.out.println("1: Edit your profile (username)");
            System.out.println("2: View Online Users");
            System.out.println("3: Listen to another user's Audio");
            System.out.println("4: Stop playing audio to your speakers");
            System.out.println("5: Mute your microphone");
            System.out.println("6: Unmute your microphone");
            System.out.println("7. Change your preferred audio device");
            System.out.println("8: Exit program");

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
                    //listen to another user's audio
//                    this.makeCallRequest();
                    this.listenToAudio();
                } else if (choice == 4) {
                    //listen to another user's audio
//                    this.makeCallRequest();
                    this.stopListenToAudio();
                } else if (choice == 5) {
                    //view current requests to this user
                    //this.getIncomingCalls();
                    this.muteMe();
                } else if (choice == 6) {
                    //unmute mic
                    this.unmuteMe();
                } else if (choice == 7) {
                    this.changeAudioDevice();
                } else if (choice == 8) {
                    System.out.println("Goodbye!");
                    try {
                        objectOutputStream.writeObject(new Message(-1));
                        this.speaker.stopListening();
                        this.microphone.stopRecording();
                        this.listener.join();
                        this.recorder.join();

                        Thread.sleep(100);
                        socket.close();
                        audioSocket.close();
                    } catch (SocketException socketException) {
                        System.out.println("Closed connection");
                    }
                    break; //exits program
                } else {
                    //chose something thats not on the list
                    System.out.println("Please choose a number that is on the main menu. \n");
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            //TODO make sure that if the value inputted is not an int, show main menu again.
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);

        String host;
        int port;

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

        new Client(host, port);
    }

    private class AudioRecorder implements Runnable {
        private AtomicBoolean running;
        private int chunkSize;
        public final Object runningKey = new Object();

        private OutputStream outputStream;

        private BufferedOutputStream bufferedOutputStream;

        public AudioRecorder(OutputStream outputStream) { // constructor run when normal call
            this.running = new AtomicBoolean(true);
            this.outputStream = outputStream;
            this.bufferedOutputStream = new BufferedOutputStream(this.outputStream);
        }

        @Override
        public void run() {
            try {
                this.chunkSize = 1024;
                microphoneLine.open(format);
                microphoneLine.start();

                //int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte[] buffer = new byte[chunkSize];

                while(this.running.get()) {
                    microphoneLine.read(buffer, 0, chunkSize);

                    try {
                        //write the audio data from the mic to the server
                        outputStream.write(buffer);
//                        objectOutputStream.write(1);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
        }

        public void stopRecording() {
            this.running.set(false);
        }

        public boolean isRunning() {
            return running.get();
        }
    }

    private class AudioListener implements Runnable {
        private SourceDataLine speakers;
        private AtomicBoolean running;
        private InputStream inputStream;
        private BufferedInputStream bufferedInputStream;

        public AudioListener(InputStream inputStream) {
            this.running = new AtomicBoolean(true);
            this.inputStream = inputStream;
            this.bufferedInputStream = new BufferedInputStream(inputStream);
            try {
                this.speakers = AudioSystem.getSourceDataLine(format);
            } catch (LineUnavailableException lineUnavailableException) {
                lineUnavailableException.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                int chunkSize = 64;

                byte[] buffer = new byte[chunkSize];

                while (this.running.get()) {
                    //listen to server and play in speaker
                    bufferedInputStream.read(buffer, 0, chunkSize);

                    speakerLine.write(buffer, 0 , buffer.length);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace(); //TODO if exception is ever thrown in this stage shut down everything... like all calls
                this.stopListening();
            }
        }

        public void stopListening() {
            this.running.set(false);
        }

        public boolean isRunning() {
            return this.running.get();
        }
    }
}

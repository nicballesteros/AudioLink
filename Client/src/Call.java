import javax.sound.sampled.AudioFormat;
import java.io.Serializable;
import java.util.Objects;

public class Call implements Serializable {
    private User recipient;
    private User sender;
    private int type;
    private boolean accepted;
    private CircularArray senderQueue;    //sender's mic
    private CircularArray recipientQueue; //recipient's mic
    private RecordingFormat format;
    //TODO add a timer so that the person seeing "calling" doesnt sit in an infinite loop. NOT IN THIS FILE!!!!!
    public static final Object senderKey = new Object();
    public static final Object recipientKey = new Object();

    public RecordingFormat getFormat() {
        return format;
    }

    public void setFormat(RecordingFormat format) {
        this.format = format;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Call(User from, User to, int type) {
        this.recipient = to;
        this.sender = from;
        this.type = type;
        accepted = false;
    }

    public User getRecipient() {
        return recipient;
    }

    public User getSender() {
        return sender;
    }

    public int getType() {
        return type;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void accept() {
        this.accepted = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Call call = (Call) o;

        if(call.getType() == this.type && this.type == 3) {
            if(Objects.equals(recipient, call.recipient) &&
                    Objects.equals(sender, call.sender) || Objects.equals(sender, call.recipient) &&
                    Objects.equals(recipient, call.sender)) {
                return true;
            } else {
                return false;
            }
        } else if (this.type == 1 && call.type == 2 || this.type == 2 && call.type == 1) {
            if(this.sender.equals(call.recipient) && this.recipient.equals(call.sender)) {
                return true;
            } else {
                return false;
            }
        }

        return type == call.type &&
                Objects.equals(recipient, call.recipient) &&
                Objects.equals(sender, call.sender) ||
                type == 3 &&
                        Objects.equals(sender, call.recipient) &&
                        Objects.equals(recipient, call.sender);
    }

    public boolean equalsExact(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Call call = (Call) o;
        return type == call.type &&
                Objects.equals(recipient, call.recipient) &&
                Objects.equals(sender, call.sender);
    }

    public void writeToQueue(int queueNumber, byte b) {
        if(queueNumber == 0) { //sender queue
            synchronized (senderKey) {
                senderQueue.write(b);
            }
        } else if (queueNumber == 1) { //recipient queue
            synchronized (recipientKey) {
                System.out.println(b);
                recipientQueue.write(b);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public byte readFromQueue(int queueNumber) {
        if(queueNumber == 0) { //sender queue
            synchronized (senderKey) {
                return senderQueue.read();
            }
        } else if (queueNumber == 1) { //recipient queue
            synchronized (recipientKey) {
                return recipientQueue.read();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
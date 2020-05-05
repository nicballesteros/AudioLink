import java.io.Serializable;

public class Request implements Serializable {
    private User recipient;
    private User sender;
    private int type;

    private CircularArray queue;
    private CircularArray secondQueue;
    private boolean accepted;

    public void setType(int type) {
        this.type = type;
    }

    public Request(User from, User to, int type) {
        this.recipient = to;
        this.sender = from;
        this.type = type;
        accepted = false;
    }

    public Request(Request another) {
        this.recipient = another.getRecipient();
        this.sender = another.getSender();
        this.type = another.getType();
        accepted = another.isAccepted();
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

    public void accept() {
        this.accepted = true;
    }

    public void deny() {
        this.accepted = false;
    }

    public boolean isAccepted() {
        return accepted;
    }

    @Override
    public boolean equals(Object o) { //Generated by Intellij
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return type == request.type &&
                recipient.equals(request.recipient) &&
                sender.equals(request.sender);
    }

    public void addToSenderQueue(byte data) {
        //add to queue
    }

    public byte readSenderQueue() {
        return 0;
    }

    public void addToSecondQueue() {

    }

    public byte readSecondQueue() {
        return 0;
    }
}

import java.io.Serializable;

public class Request implements Serializable {
    private User recipient;
    private User sender;
    private int type;

    public void setType(int type) {
        this.type = type;
    }

    public Request(User from, User to, int type) {
        this.recipient = to;
        this.sender = from;
        this.type = type;
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
}

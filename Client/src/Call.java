import java.io.Serializable;
import java.util.Objects;

public class Call implements Serializable {
    private User recipient;
    private User sender;
    private int type;
    private boolean accepted;

    public void setType(int type) {
        this.type = type;
    }

    public Call(User from, User to, int type) {
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
        return type == call.type &&
                Objects.equals(recipient, call.recipient) &&
                Objects.equals(sender, call.sender);
    }
}

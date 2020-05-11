import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Nic Ballesteros
 */

public class Message implements Serializable {
    private int type;
    private Object data;

    /**
     *
     * @param type is -1 if it is a quit message, 1 if it is a text, 2 if it is encoding data, 3 if it audio data, 4 if it is a user, 5 if it is a user list, 6 if it is a request, 7 if the user wants to retieve all reqests, -2 if there is an username error,
     * @param data the actual data in a byte[]
     */

    public Message(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Message(int type) {
        this.type = type;
        this.data = null;
    }

    public int getType() {
        return type;
    }

    public Object getData() {
//        if(type == 1) {
//            return (String) data;
//        } else if (type == 2) {
//            return (String) data;
//        } else if (type == 3) {
//            return (Byte[]) data;
//        } else if (type == 5) {
//            return (ArrayList<User>) data;
//        } else if (type ){
//            return data;
//        }

        return data;
    }
}

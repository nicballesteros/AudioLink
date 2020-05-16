public class CircularArray {
    private Node head;
    private Node tail;
    private int currentNodeID;
    private int numOfElements;
    private Node writeNode;
    private Node readNode;

    private final Object queueKey = new Object();

    public CircularArray(int size) {
        this.currentNodeID = 0;
        this.numOfElements = size;
        this.head = new Node(null, 0);
        this.tail = new Node(this.head, size - 1);
        this.writeNode = this.head;
        this.readNode = this.head;

        Node currentNode = this.head;

        for(int i = 1; i < size - 1; i++) { //create the remaining nodes
            if(i == size - 2) {
                currentNode.link = this.tail;
            } else {
                currentNode.link = new Node(null, i);
            }
            currentNode = currentNode.link;
        }
    }

    private class Node {
        public Node link;
//        private Node backwardLink;
        private int elementNumber;
        private byte data;

        public Node(Node link, int elementNumber) {
            this.link = link;
            this.elementNumber = elementNumber;
        }

        public byte getData() {
            return data;
        }

        public void setData(byte data) {
            this.data = data;
        }
    }

    public void write(byte b) {
        synchronized (queueKey) {
//            if(this.currentNodeID == 0) {
//                System.out.println("HEAD");
//            }

            this.currentNodeID++;
            this.writeNode.setData(b);
            this.writeNode = this.writeNode.link; //go to next element
            if(writeNode.elementNumber == 0) {
                currentNodeID = 0;
            }
            queueKey.notify();
        }
    }

    public boolean readable() {
        return this.writeNode == this.readNode;
    }

    public byte read() {
        synchronized (queueKey) {
            if (readNode == writeNode) {
                try {
                    queueKey.wait();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }

            byte data = this.readNode.getData();
            this.readNode = this.readNode.link;
            return data;
        }
    }

    public int size() {
        return this.numOfElements;
    }
}

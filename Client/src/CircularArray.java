public class CircularArray {
    private Node head;
    private Node tail;
    private int currentNode;
    private int numOfElements;
    private Node writeNode;
    private Node readNode;

    public CircularArray(int size) {
        this.currentNode = 0;
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
        }
    }

    private class Node {
        public Node link;
        private Node backwardLink;
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
        this.currentNode++;
        this.writeNode.setData(b);
        this.writeNode = this.writeNode.link; //go to next element
        this.notify();
    }

    public boolean readable() {
        return this.writeNode == this.readNode;
    }

    public byte read() {
        if(readNode == writeNode) {
            try {
                Thread.currentThread().wait();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }

        byte data = this.readNode.getData();
        this.readNode = this.readNode.link;
        return data;
    }

    public int size() {
        return this.numOfElements;
    }
}

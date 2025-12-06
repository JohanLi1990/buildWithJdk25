package netty.practice.miniprotocol.models;

public class CancelOrderMessage implements Message{

    private long id;

    public CancelOrderMessage(long id) {
        this.id =id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "CancelOrderMessage{" +
                "id=" + id +
                '}';
    }

    @Override
    public String getMessage() {
        return this.toString();
    }
}

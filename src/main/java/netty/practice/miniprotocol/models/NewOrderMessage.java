package netty.practice.miniprotocol.models;

public class NewOrderMessage implements Message {

    private State orderState;

    private long id; // id of the financial product

    private int qty;

    private Side side;

    public NewOrderMessage(long id, int qty, String side) {
        this.orderState = State.NEW;
        this.side = Side.valueOf(side);
        this.id = id;
        this.qty = qty;
    }

    @Override
    public String getMessage() {
        return this.toString();
    }

    @Override
    public String toString() {
        return "NewOrderMessage{" +
                "orderState=" + orderState +
                ", id=" + id +
                ", qty=" + qty +
                ", side=" + side +
                '}';
    }

    public State getOrderState() {
        return orderState;
    }

    public void setOrderState(State orderState) {
        this.orderState = orderState;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    enum State {
        NEW, ACCEPT, CANCEL;
    }

    enum Side {
        SELL, BUY
    }
}

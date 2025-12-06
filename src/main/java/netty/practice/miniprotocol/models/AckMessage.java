package netty.practice.miniprotocol.models;

public record AckMessage(long id) implements Message {
    @Override
    public String getMessage() {
        return String.format("200, Order [%s] placed", id);
    }
}

package netty.practice.miniprotocol.models;

public record Ping() implements Message {
    @Override
    public String getMessage() {
        return "200, Ping";
    }
}

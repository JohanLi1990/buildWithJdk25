package netty.practice.miniprotocol.models;

public record RejMessage(String reason) implements Message {

    @Override
    public String getMessage() {
        return "400, Rejected";
    }
}

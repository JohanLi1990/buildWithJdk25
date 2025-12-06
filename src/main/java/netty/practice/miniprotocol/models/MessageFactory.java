package netty.practice.miniprotocol.models;

public final class MessageFactory {

    public static Message createMessage(String[] fields) throws Exception {

        switch (fields[0]) {
            case "NEW" -> {
                if (fields.length < 4) {
                    throw new Exception("Message ill-formated");
                }
                return new NewOrderMessage(Long.parseLong(fields[1]), Integer.parseInt(fields[2]), fields[3]);
            }
            case "CXL" -> {
                return new CancelOrderMessage(Long.parseLong(fields[1]));
            }
            case "ACK" -> {
                return new AckMessage(Long.parseLong(fields[1]));
            }
            case "PING" -> {
                return new Ping();
            }
            case "REJ" -> {
                return new RejMessage(fields[1]);
            }
            case null, default -> throw new Exception("Message ill-formated");
        }

    }
}

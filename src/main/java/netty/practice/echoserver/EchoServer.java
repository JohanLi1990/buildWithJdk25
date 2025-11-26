package netty.practice.echoserver;

public class EchoServer {


    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    static void main() {
        int port = 9999;
        var echoServer = new EchoServer(port);
        System.out.println("Server is going to start");
        echoServer.start();
        System.out.println("Server shutting down");
    }
}

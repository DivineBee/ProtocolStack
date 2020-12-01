package Server;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MainServer {

    public static void main(String[] args) throws SocketException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        EchoServer server = new EchoServer();
        server.start();
    }
}

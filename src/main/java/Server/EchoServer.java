package Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import Packet.PacketService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EchoServer extends Thread {
    //  reference to connection between client and server
    private DatagramSocket socket;

    //  status of server thread
    private boolean running;

    private PacketService packetService;

    public EchoServer() throws SocketException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        socket = new DatagramSocket(4445);
        packetService = new PacketService();
        packetService.initializeBufferSize(true);
        packetService.setCurrentPort(4445);
    }

    public EchoServer(int listenerPort) throws SocketException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        socket = new DatagramSocket(listenerPort);
        packetService = new PacketService();
        packetService.initializeBufferSize(true);
    }

    /**
     * listen for incoming packets (put it inside run() with while-loop)
     */
    public void listen() {
        try {
            System.out.println(packetService.receiveData(socket));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //  set running flag as true (setting it to false will shut down the running process)
        running = true;

        //  perform listening while thread is considered as running one
        while (running) {
            listen();
        }

        //  close connection
        socket.close();
    }
}

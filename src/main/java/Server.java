/**
 * @author Beatrice V.
 * @created 01.11.2020 - 20:12
 * @project ProtocolStack
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.zip.CRC32;

public class Server {
    boolean isPacketReceive = true;
    final String DELIMITER = "/";
    // Step 1 : Create a socket to listen at port 1234 (Client)
    DatagramSocket socket = new DatagramSocket(1234);
    byte[] buf = new byte[65535];
    //instantiate a DatagramPacket to receive incoming messages
    DatagramPacket packetReceive = null;

    public Server() throws SocketException {
    }

    public void getPacket() throws IOException {
        while (true) {
            // Step 2 : create a DatgramPacket to receive the convertByteToString.
            packetReceive = new DatagramPacket(buf, buf.length);
            // Step 3 : receive() blocks until a message arrives and it stores the
            // message inside the byte array of the DatagramPacket passed to it.
            socket.receive(packetReceive);

            String received = new String(packetReceive.getData(), 0, packetReceive.getLength());

            System.out.println("Client:-" + received);

            checkPacketIntegrity(received);

            // Exit the server if the client sends "quit"
            if (received.equalsIgnoreCase("quit"))
            {
                System.out.println("Client sent 'quit'.....EXITING");
                break;
            }

            // Clear the buffer after every message. UDP supports packets up to 65535 bytes in length
            buf = new byte[65535];
        }
    }

    public void checkPacketIntegrity(String received) {
        String[] str = received.split(DELIMITER);
        //declare this or the Adler32 implementation
        CRC32 checksum = new CRC32();
        //update the checksum as many times as packets you receive
        checksum.update(str[0].getBytes());
        Long value = checksum.getValue(); //this is the real checksum
        System.out.println("value " + value);

        System.out.println(received);

        if(received.contains(value.toString())){
            System.out.println("Package is intact");
            isPacketReceive = true;
        } else {
            System.out.println("Package is damaged");
            isPacketReceive = false;
        }
    }

    /*public void sendResponse(){
        String response = null;
        if (isPacketReceive){
            response = "200";
            sendResponse();
        } else {
            response = "404 packet error";
            sendResponse();
        }
    }*/
    // nujen 3 metod gde ya otpravliaiu packet clientu

    public static void main(String[] args) throws IOException
    {
        Server server = new Server();
        server.getPacket();

    }
}
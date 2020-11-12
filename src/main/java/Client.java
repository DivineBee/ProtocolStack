/**
 * @author Beatrice V.
 * @created 01.11.2020 - 20:14
 * @project ProtocolStack
 */
import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.zip.CRC32;

public class Client {
    final String DELIMITER = "/";
    Scanner sc = new Scanner(System.in);

    // Step 1:Create the socket object for carrying the convertByteToString.
    private DatagramSocket socket;
    private InetAddress ip;
    byte[] buf = null;
    byte[] receiverBuf = null;

    public Client() throws UnknownHostException, SocketException {
        socket = new DatagramSocket();
        ip = InetAddress.getLocalHost();
    }

    public void sendPacket() throws IOException {
        // loop while user does not enter "quit"
        while (true) {
            String message = sc.nextLine();
            // convert the String input into the byte array.
            buf = message.getBytes();
            //Create the datagramPacket for sending
            DatagramPacket packetSend = new DatagramPacket(buf, buf.length, ip, 1234);

            //declare this or the Adler32 implementation
            CRC32 checksum = new CRC32();
            //update the checksum as many times as packets receive
            checksum.update(packetSend.getData(),0,packetSend.getLength());
            Long value = checksum.getValue(); //this is the real checksum
            System.out.println(value);

            message = message + DELIMITER + value;
            System.out.println(message);

            buf = message.getBytes();

            packetSend = new DatagramPacket(buf, buf.length, ip, 1234);

            // Step 3 : invoke the send call to actually send
            socket.send(packetSend);

            // break the loop if user enters "quit"
            if (message.contains("quit"))
                break;

            //here
            //	receive response
            receiverBuf = new byte[20];
            DatagramPacket responsePacket = new DatagramPacket(receiverBuf, receiverBuf.length);
            socket.receive(responsePacket);

            //	form a string out of response and return it
            String received = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if(received.contains("404")){
                // Step 3 : invoke the send call to actually send
                System.out.println("404 - broken package.");
                // Clear the buffer after every message. UDP supports packets up to 65535 bytes in length
                buf = new byte[65535];
                socket.send(packetSend);
            } else if(received.contains("200")) {
                System.out.println("200 - everything ok.");
            }
        }
    }

    public static void main(String args[]) throws IOException {
        Client client = new Client();
        client.sendPacket();
    }
}

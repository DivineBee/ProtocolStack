import java.io.IOException;
import java.net.*;
import java.util.zip.CRC32;

public class Sender {
    static DatagramSocket socket;
    static InetAddress ip;
    static byte[] buf = null;
    private static final String DELIMITER = "777";


    public Sender() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        ip = InetAddress.getLocalHost();
    }

    public String sendEcho(String msg) throws IOException {
        //	append hash-sum to the message (look for function lower in this code)
        String messageForPacketing = appendHashCheck(msg);

        //	transform String-formatted message into byte-array format, making possible transmission
        buf = messageForPacketing.getBytes();

        //	create UDP packet, setting byte-array message, length of the message, address where it is sent, port
        DatagramPacket packet = new DatagramPacket(buf, buf.length, ip, 1234);
        System.out.println(messageForPacketing);

        //declare this or the Adler32 implementation
        CRC32 checksum = new CRC32();

        //update the checksum as many times as packets you receive
        checksum.update(packet.getData(),0,packet.getLength());

        long value = checksum.getValue(); //this is the real checksum

        System.out.println(value);

        //	send packet
        socket.send(packet);

        //	receive response
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        //	form a string out of response and return it
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    //	close connection
    public void close() {
        socket.close();
    }

    //	append to the message its hash-sum for further check of packet integrity
    public String appendHashCheck(String message) {
        //	form a hash-sum using CRC32 algorithm (paste your code or function)

        CRC32 hashSum = new CRC32();
        hashSum.update(message.getBytes());
        return hashSum.getValue() + DELIMITER + message;
    }

    public static void main(String args[]) throws IOException
    {
        //Scanner sc = new Scanner(System.in);

        // Step 1:Create the socket object for carrying the convertByteToString.


        // loop while user does not enter "quit"

            //String message = sc.nextLine();
            // convert the String input into the byte array.
            Sender sender = new Sender();
            sender.sendEcho("hello everyone");
            sender.close();

    }
}

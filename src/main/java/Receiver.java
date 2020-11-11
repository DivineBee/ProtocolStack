import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.zip.CRC32;

public class Receiver {
    private DatagramSocket socket;
    private boolean running = true;
    private byte[] buf = new byte[256];
    private static final String DELIMITER = "777";
    InternetChecksum checksum = new InternetChecksum();

    public Receiver() throws SocketException, IOException {
        socket = new DatagramSocket(1234);
    }

    //	Runnable function for starting new server
    public void createListeningChannel() throws IOException {
        while (running) {
            //	create a buffer for receiving packet
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("gersdfs");
            socket.receive(packet);
            System.out.println(packet);

            //	get sender address and port from packet
            InetAddress address = packet.getAddress();
            int port = packet.getPort();

            //	create new packet for further sending back
            packet = new DatagramPacket(buf, buf.length, address, port);

            //	get payload of packet
            String received = new String(packet.getData(), 0, packet.getLength());
            long actual = checksum.calculateChecksum(buf);
            System.out.println(actual);

            //declare this or the Adler32 implementation
            CRC32 checksum = new CRC32();

            //update the checksum as many times as packets you receive
            checksum.update(packet.getData(),0,packet.getLength());

            long value = checksum.getValue(); //this is the real checksum

            System.out.println(value);


            //	check if packet was damaged
            String message = checkPacketIntegrity(received);
            if (message == null) {
                System.err.println("packet is damaged!");
            } else {
                System.out.println("packet is ok");
            }

            //	if payload has "end" then stop it and get some help
            if (received.equals("end")) {
                running = false;
            }
            //	send response to the client
            socket.send(packet);
        }
        //	close connection
        socket.close();
    }

    public String checkPacketIntegrity(String payload) {
        //	get hash-sum and message out of payload via splitting using delimiter
        String[] payloadContent = payload.split(DELIMITER);
        String receivedHashSum = payloadContent[0];
        String message = payloadContent[1];

        //	generate hash-sum out of message
        CRC32 localHashSum = new CRC32();
        localHashSum.update(message.getBytes());
        System.out.println(localHashSum.getValue());
        System.out.println(message);
        //	compare local hash-sum with received one. If they're not equal = packet was damaged
        if(localHashSum.getValue() == Long.parseLong(receivedHashSum)) {
            return message;
        }
        return null;
    }

    public static void main(String[] args) throws IOException
    {
        Receiver receiver = new Receiver();
        receiver.createListeningChannel();
    }
}

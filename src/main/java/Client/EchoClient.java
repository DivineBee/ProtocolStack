package Client;

import java.io.IOException;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import Packet.CommandDictionary;
import Packet.CustomPacket;
import Packet.PacketService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EchoClient {
    //  reference to connection between client and server
    private DatagramSocket socket;

    //  receiver address
    private InetAddress address;

    //  port of this client
    private int port;

    //  service that will deal with all packets
    private PacketService packetService;

    //  client constructors
    public EchoClient(int port) throws SocketException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        socket = new DatagramSocket(port);
        this.port = port;
        address = InetAddress.getByName("localhost");
        packetService = new PacketService();
        packetService.initializeBufferSize(false);
        packetService.setCurrentPort(port);
    }

    public EchoClient(InetAddress address) throws SocketException, UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        socket = new DatagramSocket();
        this.address = address;
        packetService = new PacketService();
        packetService.initializeBufferSize(false);
        packetService.setCurrentPort(-1);
    }

    //  ______________________________________________________________________________________________________________

    /**
     * Send simple message with specifying command and destination port
     * @param message message to send
     * @param command command appended to packet
     * @param destinationPort destination port where to send packet
     * @return response from server as String
     * @throws IOException error
     */
    public void sendSimpleEcho(String message, byte command, int destinationPort) throws IOException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formEncryptAndPerformDataTransmission(socket, message, command, address, destinationPort);
    }

    /**
     * Send port to which server will perform connection
     * @param destinationPort where to send data
     * @throws IOException
     */
    public void sendHandshake(int destinationPort) throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formAndPerformDataTransmission(socket, String.valueOf(port), CommandDictionary.COMMAND_HANDSHAKE, address, destinationPort);
        System.out.println(packetService.receiveData(socket));
    }

    /**
     * send Rsa handshake rsa public key and receive one from server
     * @param destinationPort where to send
     * @throws IOException
     * @throws URISyntaxException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    public void sendRsaHandshake(int destinationPort) throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formAndPerformDataTransmission(socket, packetService.getGeneratedKeys().getPublicRsaKey(), CommandDictionary.COMMAND_RSA, address, destinationPort);
        System.out.println(packetService.receiveData(socket));
    }

    /**
     * send aes key request and register locally that key
     * @param destinationPort where to send
     * @throws IOException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws URISyntaxException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    public void sendAesHandshake(int destinationPort)
            throws IOException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            URISyntaxException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formAndPerformDataTransmission(
                socket, "AES_request: give it to me, man...", CommandDictionary.COMMAND_AES, address,
                destinationPort
        );
        System.out.println(packetService.receiveData(socket));
    }

    /**
     * Send GET request to the server to get content of specified file
     * @param fileUri path to file
     * @param command command appended to packet
     * @param destinationPort where to send packet
     * @throws IOException
     * @throws URISyntaxException
     */
    public void sendFileGetRequest(String fileUri, byte command, int destinationPort)
            throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formEncryptAndPerformDataTransmission(socket, fileUri, command, address, destinationPort);
        String fileResult = packetService.receiveData(socket);
        System.out.println(fileResult);
    }

    /**
     * Send PUT request for creation/modification of file with specified file content
     * @param fileUri path to file
     * @param fileContent content of file to be added/updated
     * @param command command to be appended to packet
     * @param destinationPort where to send packet
     * @throws IOException
     * @throws URISyntaxException
     */
    public void sendFilePutRequest(String fileUri, String fileContent, byte command, int destinationPort)
            throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        packetService.formEncryptAndPerformDataTransmission(
                socket, fileUri + CustomPacket.PACKET_PAYLOAD_SEPARATOR + fileContent,
                command, address, destinationPort
        );
        String fileResult = packetService.receiveData(socket);
        System.out.println(fileResult);
    }

    //  ____________________________________________________________________________________________________________

    //  getters and setters

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

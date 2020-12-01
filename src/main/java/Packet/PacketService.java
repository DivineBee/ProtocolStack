package Packet;

import Security.AesService;
import Security.GeneratedKeys;
import Security.RsaService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Class that handles all interactions with custom packets
 */
public class PacketService {
    //  constant specifying size of custom packet in bytes
    public static final int OVERALL_CUSTOM_PACKET_SIZE = 1024;

    //  size of payload for custom packet
    public int customPacketPayloadSize;

    //  buffer for receiving packets
    private byte[] receivingBuffer;

    //  where to send packets
    private InetAddress destinationAddress;

    //  port where to send packets
    private int destinationPort;

    //  port on which this service is launched
    private int currentPort;

    private GeneratedKeys generatedKeys;

    private RsaService rsaService;

    private AesService aesService;

    /**
     *  Calculate max available size of payload taking into account possible sizes of other custom packet components
     * and initialize service instances
     * @throws UnknownHostException not found destination address
     */
    public void initializeBufferSize(boolean isItServer) throws UnknownHostException, InvalidKeySpecException, NoSuchAlgorithmException {
        //  possible max amount of bytes in ID
        short testID = Short.MAX_VALUE;

        //  possible max amount of bytes in transmission size
        short testTransmissionSize = Short.MAX_VALUE;

        //  possible max amount of bytes in command
        byte testCommand = Byte.MAX_VALUE;

        //  possible max size of MD-5 hash
        String testHash = "here you can see a hash sum of .";

        //  separator of components
        String componentsSeparator = "~~~";

        //  find how many bytes all mentioned above components will occupy in custom packet
        byte[] testBuffer = (testID +
                componentsSeparator + testTransmissionSize +
                componentsSeparator + testCommand +
                componentsSeparator + testHash + componentsSeparator).getBytes();

        //  find how many byte remain for payload of custom packet
        customPacketPayloadSize = OVERALL_CUSTOM_PACKET_SIZE - testBuffer.length;

        // initialize receiving buffer and set destination address
        receivingBuffer = new byte[OVERALL_CUSTOM_PACKET_SIZE];
        destinationAddress = InetAddress.getLocalHost();

        rsaService = new RsaService();
        aesService = new AesService();
        generatedKeys = new GeneratedKeys(isItServer);
        if(isItServer) {
            aesService.setKey(generatedKeys.getAesKey(), generatedKeys);
        }
    }

    /**
     * get amount of custom packets required for sending data through network
     * @param lengthOfData length of data required to send
     * @return amount of packets required to send data through network
     */
    public int getAmountOfCustomPacketsRequired(int lengthOfData) {
        //  amount of packets that are full with data
        int amountOfCustomPacketsFullfilledWithData = lengthOfData / customPacketPayloadSize;

        //  check how many bytes remain for the last packet
        int remainingBytesOfData = lengthOfData % customPacketPayloadSize;

        //  if there are bytes remaining then add one more packet
        if(remainingBytesOfData != 0) {
            amountOfCustomPacketsFullfilledWithData++;
        }

        //  how many packets required to send
        return amountOfCustomPacketsFullfilledWithData;
    }

    /**
     * integrate data into custom packets and send them through network with waiting for acceptance by client
     * @param socket connection
     * @param dataToSend data to send via custom packets
     * @param command command attached to data
     * @param receiverAddress address of receiver
     * @param receiverPort port of receiver
     * @throws IOException error in I/O
     */
    public void formAndPerformDataTransmission(
            DatagramSocket socket, String dataToSend, byte command, InetAddress receiverAddress, int receiverPort)
            throws IOException {
        //  calculate required custom packets amount to send data
        int transmissionCustomPacketsAmount = getAmountOfCustomPacketsRequired(dataToSend.length());

        //  if there must be sent one packet
        if(transmissionCustomPacketsAmount == 1) {

            //  form one custom packet and send it
            CustomPacket customPacket = new CustomPacket((short) 0, (short) 1, command, dataToSend);
            sendAndAcceptResponse(socket, customPacket, receiverAddress, receiverPort);

        //  if there are more than one packets to send
        } else if (transmissionCustomPacketsAmount > 1) {
            int startCustomPacketIndex;
            int endCustomPacketIndex;
            String dataForCustomPacket;

            for (int i = 0; i < transmissionCustomPacketsAmount; i++) {
                //  find starting index of segment that will be inserted in current custom packet
                startCustomPacketIndex = i * customPacketPayloadSize;

                //  if this is not the last packet to send
                if (i != transmissionCustomPacketsAmount - 1) {
                    //  calculate end of segment for this packet and specify segment of data
                    endCustomPacketIndex = startCustomPacketIndex + customPacketPayloadSize;
                    dataForCustomPacket = dataToSend.substring(startCustomPacketIndex, endCustomPacketIndex);
                //  if this is the last packet to send
                } else {
                    //  specify segment of data until the end of this data
                    dataForCustomPacket = dataToSend.substring(startCustomPacketIndex);
                }

                //  form custom packet and send it, with specifying size of transmission and ID of current packet
                CustomPacket customPacket = new CustomPacket(
                        (short) i, (short) transmissionCustomPacketsAmount,
                        command, dataForCustomPacket
                );
                sendAndAcceptResponse(socket, customPacket, receiverAddress, receiverPort);
            }
        }
    }

    /**
     * Encrypt data using AES and send via network waiting for response
     * @param socket connection
     * @param dataToSend what to send
     * @param command command to attach to the packet
     * @param receiverAddress where to send
     * @param receiverPort to which port
     * @throws IOException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    public void formEncryptAndPerformDataTransmission(
            DatagramSocket socket, String dataToSend, byte command, InetAddress receiverAddress, int receiverPort)
            throws IOException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        //  perform aes encryption of data
        String aesEncryptedData = aesService.encrypt(dataToSend);

        //  calculate required custom packets amount to send data
        int transmissionCustomPacketsAmount = getAmountOfCustomPacketsRequired(aesEncryptedData.length());

        //  if there must be sent one packet
        if(transmissionCustomPacketsAmount == 1) {

            //  form one custom packet and send it
            CustomPacket customPacket = new CustomPacket((short) 0, (short) 1, command, aesEncryptedData);
            sendAndAcceptResponse(socket, customPacket, receiverAddress, receiverPort);

            //  if there are more than one packets to send
        } else if (transmissionCustomPacketsAmount > 1) {
            int startCustomPacketIndex;
            int endCustomPacketIndex;
            String dataForCustomPacket;

            for (int i = 0; i < transmissionCustomPacketsAmount; i++) {
                //  find starting index of segment that will be inserted in current custom packet
                startCustomPacketIndex = i * customPacketPayloadSize;

                //  if this is not the last packet to send
                if (i != transmissionCustomPacketsAmount - 1) {
                    //  calculate end of segment for this packet and specify segment of data
                    endCustomPacketIndex = startCustomPacketIndex + customPacketPayloadSize;
                    dataForCustomPacket = aesEncryptedData.substring(startCustomPacketIndex, endCustomPacketIndex);
                    //  if this is the last packet to send
                } else {
                    //  specify segment of data until the end of this data
                    dataForCustomPacket = aesEncryptedData.substring(startCustomPacketIndex);
                }

                //  form custom packet and send it, with specifying size of transmission and ID of current packet
                CustomPacket customPacket = new CustomPacket(
                        (short) i, (short) transmissionCustomPacketsAmount,
                        command, dataForCustomPacket
                );
                sendAndAcceptResponse(socket, customPacket, receiverAddress, receiverPort);
            }
        }
    }

    /**
     * receive custom packets and perform actions basing on commands attached to payloads of those packets
     * @param socket connection
     * @return reaction of server to those custom packets
     * @throws IOException exception in I/O
     * @throws URISyntaxException syntax error
     */
    public String receiveData(DatagramSocket socket) throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        boolean packetAccepted = false;
        byte retransmissionTries = CustomPacket.RETRANSMISSION_TRIES;

        //  listen while packet will not be accepted or amount of retries is out
        while(!packetAccepted && retransmissionTries >= 0) {

            //  receive UDP packet
            DatagramPacket udpPacket = new DatagramPacket(receivingBuffer, receivingBuffer.length);
            socket.receive(udpPacket);

            //  encapsulate received UDP packet into custom packet
            CustomPacket customPacket = new CustomPacket(new String(udpPacket.getData()));
            System.out.println(customPacket.toString());

            //  check if this packet is intact
            if(customPacket.isPacketIntact()) {
                packetAccepted = true;

                String multiPacketReceivedMessage = customPacket.getPayload();
                Arrays.fill(receivingBuffer, (byte) 0);

                //  check if it is not handshake commands (otherwise connection will not be established)
                if(customPacket.getCommand() == CommandDictionary.COMMAND_HANDSHAKE) {
                    receiveHandshake(socket, multiPacketReceivedMessage);
                    return String.valueOf(destinationPort);
                } else if (customPacket.getCommand() == CommandDictionary.RESPONSE_HANDSHAKE) {
                    return receiveHandshakeResponse(socket, multiPacketReceivedMessage);
                }

                //  send response to client about successful receive of first packet in transaction
                sendAcceptedResponse(
                        socket, customPacket.getId(), customPacket.getCommand(), destinationAddress, destinationPort
                );

                //  if there is more than one custom packet is transmission receive them and concatenate
                if(customPacket.getTransmissionSize() != 1) {
                    multiPacketReceivedMessage = receiveMultiPacketData(socket, udpPacket, multiPacketReceivedMessage);
                }

                //  perform rsa handshake (without it can't be shared AES key)
                if (customPacket.getCommand() == CommandDictionary.COMMAND_RSA) {
                    return receiveRsaHandshake(socket, multiPacketReceivedMessage);
                } else if (customPacket.getCommand() == CommandDictionary.RESPONSE_RSA) {
                    return receiveRsaHandshakeResponse(socket, multiPacketReceivedMessage);
                //  perform aes key (without it cannot start encrypted connection)
                } else if (customPacket.getCommand() == CommandDictionary.COMMAND_AES) {
                    return receiveAesHandshake(socket, multiPacketReceivedMessage);
                } else if (customPacket.getCommand() == CommandDictionary.RESPOSNE_AES) {
                    return receiveAesHandshakeResponse(socket, multiPacketReceivedMessage);
                }

                System.out.println(multiPacketReceivedMessage);
                String decryptedData = aesService.decrypt(multiPacketReceivedMessage.trim());

                //  choose actions basing on command of this packet
                switch (customPacket.getCommand()) {

                    case CommandDictionary.MESSAGE:
                        return receiveMessage(decryptedData);

                    case CommandDictionary.COMMAND_FILE_GET:
                        return receiveFileGetCommand(socket, decryptedData);

                    case CommandDictionary.COMMAND_FILE_PUT:
                        return receiveFilePutCommand(socket, decryptedData);

                    case CommandDictionary.COMMAND_FILE_OPTIONS:
                        return receiveFileOptionsCommand(socket, decryptedData);

                    case CommandDictionary.RESPONSE_FILE_GET:
                        return receiveFileGetResponse(socket, decryptedData);

                    case CommandDictionary.RESPONSE_FILE_PUT:
                        return receiveFilePutResponse(socket, decryptedData);

                    case CommandDictionary.RESPONSE_FILE_OPTIONS:
                        return receiveFileOptionsResponse(socket, decryptedData);

                    default:
                        return "n/a";
                }
            } else {
                //  decrease retransmission tries because will be called resend request
                retransmissionTries--;

                //  form request to resend packet and send it
                sendResendCommandResponse(socket, customPacket.getId(), destinationAddress, destinationPort);
                Arrays.fill(receivingBuffer, (byte) 0);
            }
        }
        //  this is returned if packet command is unknown
        return "n/a";
    }

    /**
     * receive message to print in terminal
     * @return concatenated message
     * @throws IOException error in i/o
     */
    public String receiveMessage(String messageToPrint)
            throws IOException {

        String messageToPrintWithRemovedTrailings = messageToPrint.trim();
        System.out.println(messageToPrintWithRemovedTrailings);

        //  return printed message
        return messageToPrintWithRemovedTrailings;
    }

    /**
     * receive file GET command and send to the client content of requested file
     * @param socket connection
     * @return content of file that was sent
     * @throws IOException error in i/o
     * @throws URISyntaxException error in syntax
     */
    public String receiveFileGetCommand(DatagramSocket socket, String fileUriReceived) throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        //  get file URI from first packet
        URI fileUri = new URI(fileUriReceived.trim());

        //  find file and read its content
        File file = new File(fileUri);
        System.out.println(file);
        String fileContent = readFile(file.getPath(), StandardCharsets.US_ASCII);

        //  send content of the file to the client
        System.out.println("\n\n start \n\n");
        formEncryptAndPerformDataTransmission(
                socket, fileContent, CommandDictionary.RESPONSE_FILE_GET, destinationAddress, destinationPort
        );
        return fileContent;
    }

    /**
     * receive file PUT command and change it to received content
     * @param socket connection
     * @return changed file content
     * @throws IOException
     * @throws URISyntaxException
     */
    public String receiveFilePutCommand(DatagramSocket socket, String receivedPutCommandPayload)
            throws IOException, URISyntaxException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        //  separate parts of request: URI to file and content to put in file
        String[] postRequestParts = receivedPutCommandPayload.trim().split(CustomPacket.PACKET_PAYLOAD_SEPARATOR);
        URI fileUri = new URI(postRequestParts[CustomPacket.URI_INDEX]);
        String fileContent = postRequestParts[CustomPacket.FILE_CONTENT_INDEX];

        //  create new file or find existing one
        File file = new File(fileUri);
        if (file.createNewFile()) {
            System.out.println(fileUri + " File Created");
        } else {
            System.out.println("File " + fileUri + " already exists");
        }

        //  write to file
        FileWriter fileWriter = new FileWriter(file.getPath());
        fileWriter.write(fileContent);
        fileWriter.close();

        //  send changed file content back to client
        fileContent = "File " + file.getName() + " has been created/modified";
        formEncryptAndPerformDataTransmission(
                socket, fileContent, CommandDictionary.RESPONSE_FILE_PUT, destinationAddress, destinationPort
        );
        return fileContent;
    }

    /**
     * receive OPTIONS request and send to client metadata about requested file
     * @param socket connection
     * @return metadata of requested file
     * @throws URISyntaxException
     * @throws IOException
     */
    public String receiveFileOptionsCommand(DatagramSocket socket, String fileUriReceived)
            throws URISyntaxException, IOException, BadPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {

        //  get path to requested file
        URI fileUri = new URI(fileUriReceived.trim());
        File file = new File(fileUri);
        Arrays.fill(receivingBuffer, (byte) 0);

        //  get all metadata about file
        BasicFileAttributes attr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
        String fileOptions = file.getName() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                file.getPath() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                file.length() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                attr.creationTime() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                attr.lastAccessTime() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                attr.lastModifiedTime() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                attr.isDirectory() + CustomPacket.PACKET_PAYLOAD_SEPARATOR +
                attr.isRegularFile() + CustomPacket.PACKET_PAYLOAD_SEPARATOR;

        //  send metadata to the client
        formEncryptAndPerformDataTransmission(
                socket, fileOptions, CommandDictionary.RESPONSE_FILE_OPTIONS, destinationAddress, destinationPort
        );
        return fileOptions;
    }

    /**
     * receive multiple parts of transmission and concatenate them into one big payload
     * @param socket connection
     * @param udpPacket UDP packet
     * @param dataCollector concatenator
     * @return concatenated payloads
     * @throws IOException
     */
    private String receiveMultiPacketData(DatagramSocket socket, DatagramPacket udpPacket, String dataCollector)
            throws IOException {
        boolean transmissionFinished = false;
        CustomPacket customPacket;
        while(!transmissionFinished) {
            //  receive UDP packet
            socket.receive(udpPacket);

            //  encapsulate payload of UDP packet into custom packet
            customPacket = new CustomPacket(new String(udpPacket.getData()));
            Arrays.fill(receivingBuffer, (byte) 0);

            //  check if packet is intact
            if(customPacket.isPacketIntact()) {
                //  concatenate payload of packet to concatenator
                dataCollector += customPacket.getPayload();

                //  check if its the last packet in transmission and stop receiving if it is
                if(customPacket.getId() == customPacket.getTransmissionSize() - 1) {
                    transmissionFinished = true;
                }

                //  send response to the client about successful packet receive
                sendAcceptedResponse(
                        socket, customPacket.getId(), customPacket.getCommand(), destinationAddress, destinationPort
                );
            } else {
                //  request packet resend
                sendResendCommandResponse(socket, customPacket.getId(), destinationAddress, destinationPort);
            }
        }

        //  concatenated payloads
        return dataCollector;
    }

    /**
     * receive file GET response with content of requested file
     * @param socket connection
     * @return content of requested file
     * @throws IOException
     */
    private String receiveFileGetResponse(DatagramSocket socket, String receivedFileContent) throws IOException {
        String trimmedReceivedFileContent = receivedFileContent.trim();
        System.out.println(trimmedReceivedFileContent);

        return trimmedReceivedFileContent;
    }

    /**
     * receive response to the PUT command
     * @param socket connection
     * @param receivedFileContent what was changed
     * @return what was changed
     * @throws IOException
     */
    private String receiveFilePutResponse(DatagramSocket socket, String receivedFileContent) throws IOException {
        return receiveFileGetResponse(socket, receivedFileContent);
    }

    /**
     * receive metadata of file
     * @param socket connection
     * @param receivedFileContent metadata of requested file
     * @return metadata of requested file
     * @throws IOException
     */
    private String receiveFileOptionsResponse(DatagramSocket socket, String receivedFileContent) throws IOException {
        return receiveFileGetResponse(socket, receivedFileContent);
    }

    /**
     * get client port from handshake of communication
     * @param socket connection
     * @return received port
     * @throws IOException
     */
    private String receiveHandshake(DatagramSocket socket, String communicationHandshakeReceived)
            throws IOException {

        String trimmedCommunicationHandshake = communicationHandshakeReceived.trim();

        //  set destination port where to send messages
        destinationPort = Integer.parseInt(trimmedCommunicationHandshake);

        sendAcceptedResponse(
                socket, (short) 0, CommandDictionary.COMMAND_HANDSHAKE, destinationAddress, destinationPort
        );

        //  send response to client about successful receive of first packet in transaction
        formAndPerformDataTransmission(
                socket, String.valueOf(currentPort),
                CommandDictionary.RESPONSE_HANDSHAKE, destinationAddress, destinationPort
        );

        return trimmedCommunicationHandshake;
    }

    /**
     * receive connection port for communication
     * @param socket connection
     * @param communicationHandshakeReceived port where to send
     * @return port where to send data
     * @throws IOException
     */
    private String receiveHandshakeResponse(DatagramSocket socket, String communicationHandshakeReceived)
            throws IOException {
        String trimmedCommunicationHandshake = communicationHandshakeReceived.trim();

        //  set destination port where to send messages
        destinationPort = Integer.parseInt(trimmedCommunicationHandshake);
        sendAcceptedResponse(
                socket, (short) 0, CommandDictionary.RESPONSE_HANDSHAKE, destinationAddress, destinationPort
        );
        System.out.println(trimmedCommunicationHandshake);
        return trimmedCommunicationHandshake;
    }

    /**
     * receive rsa public key from client
     * @param socket connection
     * @param rsaPublicKey received public RSA key
     * @return received public RSA key
     * @throws IOException
     */
    private String receiveRsaHandshake(DatagramSocket socket, String rsaPublicKey) throws IOException {
        generatedKeys.setReceivedPublicRSAKey(rsaPublicKey.trim());
        formAndPerformDataTransmission(
                socket, generatedKeys.getPublicRsaKey(),
                CommandDictionary.RESPONSE_RSA, destinationAddress, destinationPort
        );
        return rsaPublicKey.trim();
    }

    /**
     * receive rsa public key from server
     * @param socket connection
     * @param rsaPublicKey received rsa key
     * @return key from server
     */
    private String receiveRsaHandshakeResponse(DatagramSocket socket, String rsaPublicKey) {
        generatedKeys.setReceivedPublicRSAKey(rsaPublicKey.trim());
        return rsaPublicKey.trim();
    }

    /**
     * receive aes key request
     * @param socket connection
     * @param aesRequest request to aes key
     * @return status of request
     * @throws IOException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    private String receiveAesHandshake(DatagramSocket socket, String aesRequest) throws IOException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        if(aesRequest.trim().equals("AES_request: give it to me, man...")) {
            String encryptedAesKey = rsaService.encrypt(generatedKeys.getAesKey(), generatedKeys);
            formAndPerformDataTransmission(
                    socket, encryptedAesKey,
                    CommandDictionary.RESPOSNE_AES, destinationAddress, destinationPort
            );
        }
        return "given aes key";
    }

    /**
     * receive aes key from server
     * @param socket connection
     * @param aesKey key to get
     * @return key
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    private String receiveAesHandshakeResponse(DatagramSocket socket, String aesKey) throws BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        String decryptedAesKey = rsaService.decrypt(aesKey.trim(), generatedKeys);
        generatedKeys.setAesKey(decryptedAesKey);
        aesService.setKey(generatedKeys.getAesKey(), generatedKeys);
        return generatedKeys.getAesKey();
    }

    /**
     * send response to the client
     * @param socket connection
     * @param customPacket custom packet to send with response
     * @param receiverAddress where to send
     * @param receiverPort port where to send
     * @throws IOException
     */
    private void sendResponse(DatagramSocket socket, CustomPacket customPacket, InetAddress receiverAddress, int receiverPort) throws IOException {
        System.out.println("response ::::::::::::::::::::::::::::           " + customPacket.toString());
        DatagramPacket udpPacket = customPacket.formUdpCustomPacket(receiverAddress, receiverPort);
        socket.send(udpPacket);
    }

    /**
     * send response with "accepted" status
     * @param socket connection
     * @param idThatWasAccepted id of accepted packet
     * @param command command of the packet
     * @param receiverAddress where to send
     * @param receiverPort which port to send
     * @throws IOException
     */
    private void sendAcceptedResponse(DatagramSocket socket, short idThatWasAccepted, byte command, InetAddress receiverAddress, int receiverPort) throws IOException {
        CustomPacket responsePacket = new CustomPacket(
                (short) 0, (short) 1, CommandDictionary.RESPONSE_ACCEPTED, "ok " + command + " " + idThatWasAccepted
        );
        sendResponse(socket, responsePacket, receiverAddress, receiverPort);
    }

    /**
     * send request for resending packet
     * @param socket connection
     * @param idToResend what packet to resend
     * @param receiverAddress where to send request
     * @param receiverPort port where to send request
     * @throws IOException
     */
    private void sendResendCommandResponse(DatagramSocket socket, short idToResend, InetAddress receiverAddress, int receiverPort) throws IOException {
        CustomPacket responsePacket = new CustomPacket(
                (short) 0, (short) 1, CommandDictionary.COMMAND_RESEND, String.valueOf(idToResend)
        );
        sendResponse(socket, responsePacket, receiverAddress, receiverPort);
    }

    /**
     * send packet and wait for response, resend if not accepted by receiver
     * @param socket connection
     * @param customPacket packet to send
     * @param receiverAddress where to send
     * @param receiverPort port where to send
     * @return send status
     * @throws IOException
     */
    private byte sendAndAcceptResponse(
            DatagramSocket socket, CustomPacket customPacket, InetAddress receiverAddress, int receiverPort)
            throws IOException {
        boolean receiverAccepted = false;
        byte retransmissionTries = CustomPacket.RETRANSMISSION_TRIES;
        CustomPacket receivableCustomPacket;
        DatagramPacket udpCustomPacket = customPacket.formUdpCustomPacket(receiverAddress, receiverPort);
        while(!receiverAccepted && retransmissionTries >= 0) {
            //  try sending data
            System.out.println("customPacket sent -------------->     " + customPacket.toString());
            socket.send(udpCustomPacket);

            //  get response
            DatagramPacket destinationResponsePacket = new DatagramPacket(
                    receivingBuffer, receivingBuffer.length, receiverAddress, receiverPort
            );
            socket.receive(destinationResponsePacket);

            //  check what type of response it is
            receivableCustomPacket = new CustomPacket(new String(destinationResponsePacket.getData()));
            System.out.println(
                    "received response to custom packet ________________________      " +
                            receivableCustomPacket.toString()
            );
            Arrays.fill(receivingBuffer, (byte) 0);
            if(receivableCustomPacket.getCommand() == CommandDictionary.COMMAND_RESEND) {
                retransmissionTries--;
            } else {
                receiverAccepted = true;
                return 0;
            }
        }
        return -1;
    }

    /**
     * read file content
     * @param filePath path to file
     * @return content of file
     */
    private String readLineByLineFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    /**
     * read file content
     * @param path path to file
     * @param encoding encoding of the file
     * @return file content
     * @throws IOException
     */
    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    //  getters and setters
    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public void setCurrentPort(int currentPort) {
        this.currentPort = currentPort;
    }

    public int getCurrentPort() {
        return this.currentPort;
    }

    public RsaService getRsaService() {
        return rsaService;
    }

    public AesService getAesService() {
        return aesService;
    }

    public GeneratedKeys getGeneratedKeys() {
        return generatedKeys;
    }

    private String performRsaEncryption(String dataToEncrypt) throws BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        int amountOfEncryptions = dataToEncrypt.length() / 245;
        int remainingFragmentLength = dataToEncrypt.length() % 245;
        String intermediate;
        String encryptedConcatenator = "";

        if(remainingFragmentLength != 0) {
            amountOfEncryptions++;
        }
        int startIndex = 0;
        int endIndex = 0;

        for(int i = 0; i < amountOfEncryptions; i++) {
            startIndex = i * 245;

            if(i == amountOfEncryptions - 1) {
                intermediate = rsaService.encrypt(dataToEncrypt.substring(startIndex), generatedKeys);
                encryptedConcatenator += intermediate;
            } else {
                endIndex = startIndex + 245;
                intermediate = rsaService.encrypt(dataToEncrypt.substring(startIndex, endIndex), generatedKeys);
                encryptedConcatenator += intermediate;
            }
        }

        return encryptedConcatenator;
    }
}

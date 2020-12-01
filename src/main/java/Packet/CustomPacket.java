package Packet;

import org.apache.commons.codec.digest.DigestUtils;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Class for custom packets with their specifications and parameters
 */
public class CustomPacket {
    //  separator for components of the packet and indexes of those components in the packet
    public static final String PACKET_COMPONENTS_SEPARATOR = "~~~";
    public static final byte ID_INDEX = 0;
    public static final byte TRANSMISSION_SIZE_INDEX = 1;
    public static final byte COMMAND_INDEX = 2;
    public static final byte HASH_SUM_INDEX = 3;
    public static final byte PAYLOAD_INDEX = 4;

    //  separator for payload if there is sent uri of file and content of this file
    public static final String PACKET_PAYLOAD_SEPARATOR = "___";
    public static final byte URI_INDEX = 0;
    public static final byte FILE_CONTENT_INDEX = 1;

    //  amount of retransmission tries
    public static final byte RETRANSMISSION_TRIES = 3;

    //  ID of the packet in transmission
    private short id;

    //  size of current transmission
    private short transmissionSize;

    //  command in the packet
    private byte command;

    //  hash sum of the packet (generated from id, transmission size, command, and payload)
    private String hashSum;

    //  message, file content or other data sent via packet
    private String payload;

    /**
     * default constructor for packet
     * @param id id of the packet it transmission
     * @param transmissionSize size of current transmission
     * @param command command in packet
     * @param payload message, file content or other data
     */
    public CustomPacket(short id, short transmissionSize, byte command, String payload) {
        this.id = id;
        this.transmissionSize = transmissionSize;
        this.command = command;
        this.payload = payload;
        this.hashSum = formHashSum(this.id + this.transmissionSize + this.command + this.payload.trim());
    }

    /**
     * default empty constructor
     */
    public CustomPacket() {}

    /**
     * constructor that creates packet basing on received via network payload of UDP packet decomposing its components
     * @param stringFormattedCustomPacket string formatted UDP packet payload
     */
    public CustomPacket(String stringFormattedCustomPacket) {
        //  split components of the packet
        String[] customPacketComponents = stringFormattedCustomPacket.split(PACKET_COMPONENTS_SEPARATOR);

        //  set separated parts to fields of the packet
        this.id = Short.parseShort(customPacketComponents[ID_INDEX]);
        this.transmissionSize = Short.parseShort(customPacketComponents[TRANSMISSION_SIZE_INDEX]);
        this.command = Byte.parseByte(customPacketComponents[COMMAND_INDEX]);
        this.hashSum = customPacketComponents[HASH_SUM_INDEX];
        this.payload = customPacketComponents[PAYLOAD_INDEX];
    }

    /**
     * Check packet integrity via comparing recreated packet hash sum with received one
     * @return Is packet intact or not
     */
    public boolean isPacketIntact() {
        //  generate again MD-5 hash sum from payload and compare with received one
        String md5PacketHashSum = DigestUtils.md5Hex(this.id + this.transmissionSize + this.command + this.payload.trim()).toUpperCase();
        return hashSum.equals(md5PacketHashSum);
    }

    /**
     * Form MD-5 hash sum of data
     * @param data data from which hash sum is required
     * @return Data hash sum in MD-5
     */
    public String formHashSum(String data) {
        return DigestUtils.md5Hex(data).toUpperCase();
    }

    /**
     * create UDP packet with setting as payload byte-formatted components of the packet
     * @param receiverAddress Inet address of the receiver
     * @param receiverPort port of the receiver
     * @return
     */
    public DatagramPacket formUdpCustomPacket(InetAddress receiverAddress, int receiverPort) {
        //  to avoid filling at receiver end payload with empty bytes reserved for other components of the packet,
        // those elements are made to be with fixed size. Also separate all components with separators
        String packetPayload = String.format("%05d", this.id) +
                PACKET_COMPONENTS_SEPARATOR + String.format("%05d", this.transmissionSize) +
                PACKET_COMPONENTS_SEPARATOR + this.command +
                PACKET_COMPONENTS_SEPARATOR + this.hashSum +
                PACKET_COMPONENTS_SEPARATOR + this.payload;

        //  create payload from byte-formatted components of the packet
        byte[] packetBuffer = packetPayload.getBytes();

        //  return created UDP packet
        return new DatagramPacket(packetBuffer, packetBuffer.length, receiverAddress, receiverPort);
    }

    //  standard getters, setters, and toString()

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public short getTransmissionSize() {
        return transmissionSize;
    }

    public void setTransmissionSize(short transmissionSize) {
        this.transmissionSize = transmissionSize;
    }

    public byte getCommand() {
        return command;
    }

    public void setCommand(byte command) {
        this.command = command;
    }

    public String getHashSum() {
        return hashSum;
    }

    public void setHashSum(String hashSum) {
        this.hashSum = hashSum;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "CustomPacket{" +
                "id=" + id +
                ", transmissionSize=" + transmissionSize +
                ", command='" + command + '\'' +
                ", hashSum='" + hashSum + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}

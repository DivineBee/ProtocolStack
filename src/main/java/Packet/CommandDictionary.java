package Packet;

/**
 * Dictionary class of all used commands
 */
public class CommandDictionary {
    //  command for performing handshake and sending client port to the server
    public static final byte COMMAND_HANDSHAKE = 1;
    public static final byte RESPONSE_HANDSHAKE = 2;

    public static final byte COMMAND_RSA = 3;
    public static final byte RESPONSE_RSA = 4;

    public static final byte COMMAND_AES = 5;
    public static final byte RESPOSNE_AES = 6;

    //  command for showing message in terminal
    public static final byte MESSAGE = 7;


    //  file interaction requests and responses

    //  GET flags
    public static final byte COMMAND_FILE_GET = 8;
    public static final byte RESPONSE_FILE_GET = 9;

    //  PUT flags
    public static final byte COMMAND_FILE_PUT = 10;
    public static final byte RESPONSE_FILE_PUT = 11;

    //  OPTIONS flags
    public static final byte COMMAND_FILE_OPTIONS = 12;
    public static final byte RESPONSE_FILE_OPTIONS = 13;


    //  accept and resend interaction
    public static final byte COMMAND_RESEND = 100;
    public static final byte RESPONSE_ACCEPTED = 101;
}

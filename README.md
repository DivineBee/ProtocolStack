# Lab 2 - Implementation of a protocol stack
#### University: Technical University of Moldova  
#### Group: FAF 182   
#### Teacher: Alexandru Burlacu  
#### Student: Vizant Beatrice

## Table of contents

* [Requirements](#requirements)
* [Explanation](#explanation)
* [Error-checking](#error-checking)
* [Retransmission](#retransmission)
* [Secure Connection](#secure-connection)
* [Packeting](#packeting-this-part-is-more-about-implementation-of-the-protocol)
* [Technologies](#technologies)
* [How to use](#how-to-use)
* [Output example](#output-example)
* [Owner](#owner)
* [Status](#status)

## Requirements
To implement a protocol stack, namely a transport protocol based on UDP, a session-level security  
protocol inspired by SSL/TLS, and an application-level protocol. You must present this project as a  
client and a server, both using a library that contains the protocol logic. The library must be made up  
of 3 modules, for each level of the protocol stack, with a well-defined API and that adheres to the  
layered architecture. 
 
## Explanation
#### Making of Client and Server(establishing connection)
For this I created two classes _Server_ and _Client_ and using _DatagramSocket_ established the  
connection between them two. Later on I divided these classes in 2 separate ones which are now called  
_EchoClient_ and _MainClient_ and put them to the according package(same for server). This separates  
the runner class from methods and make code more readable and easier to modify in the future.  
__All the code is commented, I will specify only the important parts and their realisation.__  
##### Client  
In _EchoClient_ I made 2 constructors, one for local(which gets as parameter only the port) and  
anohter one for the remote connection where the port and address will be passed. Next are the  
methods which are called from _Main_ where first the client object is instantiated, after that  
client performs "handshake" with server this is to provide privacy and data integrity for  
communication between the two. During the Handshake, server and client exchange information  
required to establish the connection. To make the connection secure I made Rsa and Aes handshake  
more about it will be described below. After establishing connection the client is ready to send  
messages to the server, because i made a FTP like protocol I can send a file indicating URI of it.  
Then the client can send different commands to the server with requests of what should be done with  
the file(put, options, get).  
##### Server
Server too has constructors and fields which permit establishing connection with the client, also  
it has a method which is of major importance is the _listen()_ which listens to client requests on  
connection.
#### Error-checking  
First I had to check packet integrity via comparing recreated packet hash sum with received one,  
for this I used MD5 hashsum the perfect match of MD5 checksum value ensures that the digital  
integrity and security of the message has not been broken by someone else and also that it is the  
accurate copy of the original file. _Client_ when sending a packet generates the hashsum upon the  
message, then Server does the same and compares the values, if it matches then the packet is intact  
else the retransmission of packet must be done.  
#### Retransmission
_sendAndAcceptResponse()_ line 691 class _PacketService_ is responsible for resending the packet in  
not accepted by the server. First it forms the packet then while the number of retries is bigger than  
0 it tries to send the data after which gets response and checks what type of response it is, if the  
command is "Resend" then decrease the retransmission tries by one and enter the cycle again doing the  
same steps, the number of tries is defined by a constant which can be modified, for now is set for up to  
3 retries. But if the command is accepted it returns that the server accepted the packet because he sent  
a successful command.  
```java
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
```
The retransmission and error checking are called in a bigger method called receiveData()  
on line 238 within the same class, it receives the custom packets and perform actions based on commands which  
are attached to payloads of the packets. Here is a while loop which listens while the packet is not accepted  
or amount of retries is out, it receives the packet, encapsulates that into a custom one and performs a series  
of checks upon it, the chek if packet is intact, if it contains handshake commands, also if the packets were  
divided into smaller packets, if so then the method has to put them together in order. Inside this method  
is performing the rsa handshake and shared the aes key.  
```java
    . . . if(customPacket.isPacketIntact()) {
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
        } else if . . .
```
#### Secure Connection  
For this I dedicated an entire package and few days of research, I chose a hybrid method of RSA with AES  
I picked RSA over Diffie-Hellman key exchange, because it makes it susceptible to man-in-the-middle attacks,  
since it doesn't authenticate either party involved in the exchange. RSA key exchange involves the server's  
secret RSA key, which DH does not. This means that even if the attacker obtains means of cracking the RSA  
key, he will have to expend the cracking effort for each server/key. With DH, most of the cracking effort  
goes to breaking the entire, so-called, algebraic/elliptic group. After that, breaking individual  
connections is cheap. The method I used is that the data is AES encrypted using a unique one-time-use  
key and then the AES key is encrypted by a recipient generated RSA public key. The encrypted data  
and the encrypted AES key are placed in the bucket. The recipient can then use their private key to  
decrypt the AES key, and then use that to decrypt the data. The server creates an RSA key pair. The  
sender generates a random AES key.The data is encrypted using this key, then AES key is encrypted with  
the RSA public key. The encrypted data and the encrypted key is sent to the server. The server uses their  
RSA private key to decrypt the AES key. Next, server uses the AES key to decrypt the data. Benefits of such  
approach are: Each data object is encrypted with a different key. Large messages are encrypted decrypted  
with the fast AES algorithm. Only the key, itself, uses the slower RSA algorithm. If the same data needs  
to be sent to multiple endpoints then this process is easily extended; for each recipient use their public  
key and store multiple copies of the encrypted AES key, one copy for each recipient. In the _AesService_  
are 3 methods, one which sets the key(which will be used for encryption of messages) one method to decrypt  
and to encrypt the data. Example of setting the key:  
```java
public void setKey(String clientKey, GeneratedKeys generatedKeys) throws NoSuchAlgorithmException {
        generatedKeys.setAesKey(clientKey);

        // make byte-form of message
        byte[] key = generatedKeys.getAesKey().getBytes(StandardCharsets.UTF_8);

        // set type of key that will be used for encryption
        MessageDigest sha = MessageDigest.getInstance("SHA-1");

        // setting private key for encryption
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        secretAesKeySpec = new SecretKeySpec(key, "AES");
    }
```
Receiving rsa public key from client:  
```java
private String receiveRsaHandshake(DatagramSocket socket, String rsaPublicKey) throws IOException {
        generatedKeys.setReceivedPublicRSAKey(rsaPublicKey.trim());
        formAndPerformDataTransmission(
                socket, generatedKeys.getPublicRsaKey(),
                CommandDictionary.RESPONSE_RSA, destinationAddress, destinationPort
        );
        return rsaPublicKey.trim();
    }
```
Receiving aes key request:  
```java
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
```
Generation of keys is done this way:  
(The RSA key must be at least 2048bits in size (giving 112bits of strength)  
I used base64 encoder because when you have some binary data that you want to ship  
across a network, you generally don't do it by just streaming the bits and bytes over  
the wire in a raw format. Because some media are made for streaming text. You never  
know - protocols may interpret binary data as control characters, or binary data could  
be screwed up because the underlying protocol might think that you've entered a special  
character combination (like how FTP translates line endings). So to get around this,  
I encoded the binary data into characters using Base64 because I can generally rely on  
the same 64 characters being present in many character sets, and to be confident that  
data's going to end up on the other side of the wire uncorrupted.  
```java
    public GeneratedKeys(boolean isItServer) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //  create rsa 2048-bit keys
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        //  append them and save them
        Key publicKey = keyPair.getPublic();
        Key privateKey = keyPair.getPrivate();

        //  create encoder for saving them into String form
        Base64.Encoder encoder = Base64.getEncoder();

        //  append key-strings
        this.publicRsaKey = encoder.encodeToString(publicKey.getEncoded());
        this.privateRsaKey = encoder.encodeToString(privateKey.getEncoded());

        //  initialize AES key if it is a server (client will receive this key from server)
        if(isItServer) {
            this.aesKey = "beatrixKido";
        }
    }
```
#### Packeting (this part is more about implementation of the protocol)
I chose to make a FTP-like protocol.   
Everything regarding packet transmission, handling, splitting, checking and so on is in here.  
The methods used in this classes I optimized to make universal, to be used by Server and also  
by Client. In the package called _Packet_ are 3 classes:  
* _CommandDictionary_
* _CustomPacket_
* _PacketService_

_CommandDictionary_ contains a set of command constants which are being send with the packet they  
denote what the client or server should do with them next. Constants starting with COMMAND    
are sent by the client, constants starting with RESPONSE are used by servers. Example:  
```java
//  command for performing handshake and sending client port to the server
    public static final byte COMMAND_HANDSHAKE = 1;
    public static final byte RESPONSE_HANDSHAKE = 2;

    public static final byte COMMAND_RSA = 3;
    public static final byte RESPONSE_RSA = 4;
    . . .
    //  command for showing message in terminal
    public static final byte MESSAGE = 7;

    //  file interaction requests and responses
    //  GET flags
    public static final byte COMMAND_FILE_GET = 8;
    public static final byte RESPONSE_FILE_GET = 9;
    . . .
    //  OPTIONS flags
    public static final byte COMMAND_FILE_OPTIONS = 12;
    public static final byte RESPONSE_FILE_OPTIONS = 13;

    //  accept and resend interaction
    public static final byte COMMAND_RESEND = 100;
    public static final byte RESPONSE_ACCEPTED = 101;
```
_CustomPacket_ is used for customing the packets, setting parameters and specifications  
In the first listed fields are the components of the packet and I used a separator to  
divide these components into parts, for example first separator - separes the this part  
into id of the packet, transmission size, command, hashsum and payload. The next separator  
is for the payload where the uri and file context index. When a packet is received it is  
being decomposed inside the _CustomPacket_ constructor with described above components as  
ids, hashsum etc.:    
```java
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
```  
Also there is a method of forming the packet with setting as payload byte-formatted components of the packet  
```java
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
```
_PacketService_ is the class which handles all interactions with custom packets. I set that if the  
message will exceed the size of 1024 bytes then it will be divided into segments and will be sent  
separately and then combined on arrival. To ensure that the packets will arrive in order, on dividing  
the intitial message I make the indexing of the packets which are then reassembled based on the increasing  
number of the index. You will ask but how the server knows how many packets he needs to wait to reassemble?  
For this, when the packet is being separated in smaller packets on client-side it counts the number of  
how many parts he got, for example he had a message of size 6004, it splits then conform the set size 1024  
and gets 6 smaller packets which will be later sent, when sending the first packet he sends the client also  
the number of packets to be expected so its 6. Then, server when receives the packets, compares the number of  
received packets with the number of expected(6) and waits until they are all sent so he can reassemble the  
packets. For calculation of max available size of payload we need to take into account the size of other  
packets' components such as hashsum, ids, transmission size and so on in order to correctly split the data  
without loss. It would be very bad if half of the one's hashsum will be split into 2 different packets  
causing errors and broken packages. Below is the method for this.
```java
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
```
Line 125 method _formAndPerformDataTransmission_ integrate data into custom packets  
and send them through network with waiting for acceptance by client. The below for loop  
is from this method which shows how the packets are being send if there is more than 1 packet.  
```java
. . .
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
```
The biggest method is at line 238 is the receiveData() which receive custom packets  
and perform actions based on commands attached to payloads of those packets I described  
part of it above, now I will mention the part with commands. The commands are handled  
through a switch case which calls the respective method, works as follows:  
```java
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
            return "n/a";//this is returned if packet command is unknown
```
After that I started to implement the FTP like commands such as Get, Put, Options.  
Below is an example of the method GET command.
```java
public String receiveFileGetCommand(DatagramSocket socket, String fileUriReceived) throws ...{
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
```
The receive GET response with content of requested file:
```java
 private String receiveFileGetResponse(DatagramSocket socket, String receivedFileContent) throws IOException {
        String trimmedReceivedFileContent = receivedFileContent.trim();
        System.out.println(trimmedReceivedFileContent);

        return trimmedReceivedFileContent;
    }
```
The PUT method:  
```java
public String receiveFilePutCommand(DatagramSocket socket, String receivedPutCommandPayload){
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
```
For the rest of methods you can check the class from line 355-519.  
After that in the class are present the handshakes which were already explained.  

## Technologies
Language: Java  
IDE: IntelliJ IDEA  

## How to use
1. import project with pom.xml or clone project or download zip.  
2. Run _MainServer_  
3. Run _MainClient_

## Output example
Server:  
![Alt text](https://raw.githubusercontent.com/DivineBee/ProtocolStack/master/src/main/images/server1.JPG)
![Alt text](https://raw.githubusercontent.com/DivineBee/ProtocolStack/master/src/main/images/server2.JPG)  
Client:  
![Alt text](https://raw.githubusercontent.com/DivineBee/ProtocolStack/master/src/main/images/client1.JPG)
![Alt text](https://raw.githubusercontent.com/DivineBee/ProtocolStack/master/src/main/images/client2.JPG)
## Owner
> 3rd year student FAF - 182  

| <a href="https://github.com/DivineBee" target="_blank">**Vizant Beatrice**</a>
| :---: |
| [![Vizant Beatrice](https://avatars0.githubusercontent.com/u/49019844?s=200&u=b232b6a4e7d387d304f0b7938eabe6cf742bacb8&v=4)](http://github.com/DivineBee)    | [![Lesco Andrei](https://avatars2.githubusercontent.com/u/53511833?s=200&u=4b5de9bd5272530cf96b9d5a174dc6af3e3ecbf0&v=4)](http://github.com/whysoserious97) |
| <a href="//github.com/DivineBee" target="_blank">`github.com/DivineBee`</a> | <a href="http://github.com/whysoserious97" target="_blank">`github.com/whysoserious97`</a> |

## Status
Project is: _finished_ - but can be improved by adding more features.

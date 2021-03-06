package Client;

import Packet.CommandDictionary;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MainClient {

    //  testing message for transmission of data
    public static String testMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus porttitor arcu quam, sit amet euismod lacus congue nec. Vivamus dictum metus sed est lobortis fermentum. Etiam suscipit lobortis egestas. Aenean ac odio risus. Donec dapibus leo vel sapien consectetur bibendum. Aenean et diam elit. Etiam diam massa, semper ut mattis ac, vulputate non tellus. Aenean nisl augue, bibendum sit amet fermentum vitae, gravida nec metus. Quisque maximus nisi nec erat pretium varius. Quisque massa diam, suscipit sed vestibulum at, vehicula a nisl. Sed vitae magna a augue condimentum aliquam. In faucibus dolor quis eros condimentum facilisis. Aenean aliquam vel enim faucibus interdum. Quisque vehicula ac turpis ac fermentum. Suspendisse posuere at ante id dapibus.\n" +
            "Quisque eu sem feugiat enim porta pellentesque in a nulla. Nunc feugiat, diam vel elementum pellentesque, diam sapien tempus dolor, vitae sodales purus nibh quis nunc. Nulla venenatis lacinia nibh at mattis. Sed convallis enim sed feugiat placerat. Nam et tincidunt mauris. Duis mauris sapien, maximus quis congue a, iaculis eu quam. Vivamus eget ante faucibus diam aliquam finibus. Phasellus eu urna dictum, luctus quam et, rutrum lorem. Donec finibus nisi non purus viverra, id ultrices enim egestas. Morbi suscipit leo ut facilisis tristique.\n" +
            "Nullam at ipsum commodo, lobortis eros ac, egestas justo. Proin ullamcorper, ligula quis suscipit pulvinar, arcu odio facilisis tellus, quis pharetra tortor nisi sit amet risus. Ut bibendum ante lectus, vel eleifend metus consequat id. Sed nec felis nec tortor faucibus egestas at aliquam erat. Nullam et porttitor lectus. In vel risus eu nunc ultrices venenatis eu non risus. Phasellus eu diam urna. Maecenas eu erat tellus. Integer orci turpis, dapibus porttitor augue quis, sodales egestas eros. Nulla vitae faucibus ligula, sit amet aliquet nisl. Donec aliquet interdum nibh. Praesent orci purus, vehicula sed hendrerit non, pulvinar eu diam. Sed arcu nibh, pulvinar vitae urna nec, aliquet facilisis libero. Nunc dignissim dolor a ultricies consequat. Ut a maximus orci. Curabitur lacinia scelerisque metus sodales ultricies.\n" +
            "Integer elementum accumsan mi vel hendrerit. Phasellus eu purus nisi. Nulla augue lorem, consectetur vel efficitur a, egestas eget magna. Quisque at massa mattis, finibus quam et, varius ex. Vestibulum tempus a augue condimentum tincidunt. Ut aliquet placerat dolor non ullamcorper. Proin velit est, dictum sed aliquet quis, euismod ac nisl. Aliquam erat volutpat.\n" +
            "Integer et est semper, posuere nisl sed, euismod odio. Ut pretium ex et sollicitudin venenatis. Etiam interdum nec metus sit amet consequat. Duis faucibus arcu eget pretium mollis. Duis volutpat laoreet posuere. Ut eu massa ullamcorper, faucibus nulla ac, pharetra metus. Etiam sollicitudin lobortis ex, quis varius arcu dictum nec. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut feugiat velit eu lectus sollicitudin fringilla. Integer id nunc aliquam turpis auctor bibendum eget vel quam. Fusce ultricies id purus varius aliquet. Nulla eget consequat turpis, quis finibus nulla. Maecenas volutpat hendrerit erat ac tincidunt. Morbi efficitur suscipit diam at consequat. Cras molestie vestibulum lorem et placerat.";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, URISyntaxException, InvalidKeySpecException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
        EchoClient client = new EchoClient(4446);

        //  send handshake to the client specifying port of client
        client.sendHandshake(4445);

        //  send rsa handshake
        client.sendRsaHandshake(4445);

        //  send aes handshake
        client.sendAesHandshake(4445);

        //  send data to pring on server side
        client.sendSimpleEcho(testMessage, CommandDictionary.MESSAGE, 4445);

        URI myFile = new File("B:\\PROG\\PROJECTS\\ProtocolStack\\src\\main\\java\\test.txt").toURI();
        File f = new File(myFile);
        System.out.println(f);

        //  send get request
        client.sendFileGetRequest(myFile.toString(), CommandDictionary.COMMAND_FILE_GET, 4445);

        //  send put request
        client.sendFilePutRequest(myFile.toString(), testMessage, CommandDictionary.COMMAND_FILE_PUT, 4445);

        //  request metadata of file
        client.sendFileGetRequest(myFile.toString(), CommandDictionary.COMMAND_FILE_OPTIONS, 4445);
    }
}

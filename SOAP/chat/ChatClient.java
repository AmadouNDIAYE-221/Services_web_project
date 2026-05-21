package chat;

import java.net.URL;
import jakarta.xml.ws.Service;
import javax.xml.namespace.QName;

/**
 * Client console du Chat SOAP — mis à jour pour les nouvelles signatures
 * getMessageCount(pseudo) et getMessages(pseudo, fromIndex)
 */
public class ChatClient {

    public static void main(String[] args) throws Exception {

        URL    wsdlURL  = new URL("http://localhost:9090/chat?wsdl");
        QName  svcName  = new QName("http://chat/", "ChatRoomImplService");
        QName  portName = new QName("http://chat/", "ChatRoomImplPort");
        Service service = Service.create(wsdlURL, svcName);
        IChatRoom room  = service.getPort(portName, IChatRoom.class);

        String pseudo = args.length > 0 ? args[0] : "Console";

        // Connexion
        System.out.println(room.subscribe(pseudo));

        // Polling simple en console
        int[] lastIndex = {0};

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { room.unsubscribe(pseudo); } catch (Exception ignored) {}
        }));

        while (true) {
            // ── Nouvelles signatures ──────────────────────────────────────
            int count = room.getMessageCount(pseudo);   // String pseudo requis
            if (count > lastIndex[0]) {
                String[] msgs = room.getMessages(pseudo, lastIndex[0]); // pseudo + index
                for (String m : msgs) System.out.println(m);
                lastIndex[0] = count;
            }
            Thread.sleep(1000);
        }

    }
}
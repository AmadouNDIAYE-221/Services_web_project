package chat;


import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import java.net.URL;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) throws Exception {
        // Paramètres du service SOAP
        URL    wsdlURL  = new URL("http://localhost:9090/chat?wsdl");
        QName  svcName  = new QName("http://chat/", "ChatRoomImplService");
        QName  portName = new QName("http://chat/", "ChatRoomImplPort");

        Service service = Service.create(wsdlURL, svcName);
        IChatRoom room  = service.getPort(portName, IChatRoom.class);

        Scanner scanner = new Scanner(System.in);

        // 1. S'inscrire
        System.out.print("Entrez votre pseudo : ");
        String pseudo = scanner.nextLine().trim();
        System.out.println("Serveur : " + room.subscribe(pseudo));
        System.out.println("=== Tapez 'exit' pour quitter ===\n");

        // 2. Thread de polling
        final int[] lastIndex = {0};
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int count = room.getMessageCount();
                    if (count > lastIndex[0]) {
                        String[] msgs = room.getMessages(lastIndex[0]);
                        for (String msg : msgs) System.out.println(msg);
                        lastIndex[0] = count;
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Erreur polling : " + e.getMessage());
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        // 3. Boucle d'envoi
        while (true) {
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                room.unsubscribe(pseudo);
                System.out.println("Déconnecté.");
                break;
            }
            if (!message.isEmpty()) {
                room.postMessage(pseudo, message);
            }
        }
        poller.interrupt();
        scanner.close();
    }
}
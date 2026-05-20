import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import java.net.URL;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://localhost:8181/RPC2"));

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);

        Scanner scanner = new Scanner(System.in);

        // 1. S'inscrire
        System.out.print("Entrez votre pseudo : ");
        String pseudo = scanner.nextLine().trim();
        String resp = (String) client.execute("ChatRoom.subscribe",
                                               new Object[]{pseudo});
        System.out.println(resp);
        System.out.println("=== Tapez 'exit' pour quitter ===\n");

        // 2. Thread de polling pour afficher les nouveaux messages
        final int[] lastIndex = {0};
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int count = (Integer) client.execute(
                            "ChatRoom.getMessageCount", new Object[]{});
                    if (count > lastIndex[0]) {
                        Object[] msgs = (Object[]) client.execute(
                                "ChatRoom.getMessages",
                                new Object[]{lastIndex[0]});
                        for (Object msg : msgs) {
                            System.out.println(msg);
                        }
                        lastIndex[0] = count;
                    }
                    Thread.sleep(500); // poll toutes les 500ms
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
                client.execute("ChatRoom.unsubscribe", new Object[]{pseudo});
                System.out.println("Déconnecté.");
                break;
            }
            if (!message.isEmpty()) {
                client.execute("ChatRoom.postMessage",
                               new Object[]{pseudo, message});
            }
        }
        poller.interrupt();
        scanner.close();
    }
}
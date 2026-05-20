import java.rmi.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 1. Demander le pseudo
            System.out.print("Entrez votre pseudo : ");
            String pseudo = scanner.nextLine().trim();

            // 2. Créer l'objet distant représentant ce client
            ChatUserImpl userObj = new ChatUserImpl();

            // 3. Rechercher la salle de discussion dans le registre
            ChatRoom room = (ChatRoom) Naming.lookup("//localhost/ChatRoom");

            // 4. S'inscrire à la salle
            room.subscribe(userObj, pseudo);
            System.out.println("=== Connecté au chat. Tapez 'exit' pour quitter. ===\n");

            // 5. Boucle d'envoi de messages
            String message;
            while (true) {
                message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    room.unsubscribe(pseudo);
                    System.out.println("Déconnexion...");
                    break;
                }
                if (!message.isEmpty()) {
                    room.postMessage(pseudo, message);
                }
            }

        } catch (NotBoundException e) {
            System.err.println("Serveur introuvable. Vérifiez que ChatServer est lancé.");
        } catch (Exception e) {
            System.err.println("Erreur client : " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
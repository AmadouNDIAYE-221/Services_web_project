import java.rmi.*;
import java.rmi.registry.LocateRegistry;

public class ChatServer {
    public static void main(String[] args) {
        try {
            // Créer la salle de discussion
            ChatRoomImpl room = new ChatRoomImpl();

            // Lancer le registre RMI sur le port 1099
            LocateRegistry.createRegistry(1099);

            // Enregistrer la salle sous le nom "ChatRoom"
            Naming.rebind("//localhost/ChatRoom", room);

            System.out.println("=== Serveur Chat RMI démarré (port 1099) ===");
            System.out.println("En attente de connexions...");

        } catch (Exception e) {
            System.err.println("Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
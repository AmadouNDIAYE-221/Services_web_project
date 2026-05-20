import java.util.ArrayList;
import java.util.List;

public class ChatRoom {

    // Historique des messages
    private static List<String> messageHistory = new ArrayList<>();
    // Liste des pseudos connectés
    private static List<String> connectedUsers = new ArrayList<>();

    // Inscription d'un utilisateur
    public String subscribe(String pseudo) {
        if (!connectedUsers.contains(pseudo)) {
            connectedUsers.add(pseudo);
            String notif = "*** " + pseudo + " a rejoint le chat ***";
            messageHistory.add(notif);
            System.out.println(notif);
            return "Connecté avec le pseudo : " + pseudo;
        }
        return "Pseudo déjà utilisé.";
    }

    // Désinscription
    public String unsubscribe(String pseudo) {
        connectedUsers.remove(pseudo);
        String notif = "*** " + pseudo + " a quitté le chat ***";
        messageHistory.add(notif);
        System.out.println(notif);
        return "Déconnecté.";
    }

    // Envoi d'un message
    public String postMessage(String pseudo, String message) {
        if (!connectedUsers.contains(pseudo)) {
            return "Erreur : vous n'êtes pas connecté.";
        }
        String fullMsg = "[" + pseudo + "] " + message;
        messageHistory.add(fullMsg);
        System.out.println(fullMsg);
        return "Message envoyé.";
    }

    // Récupérer les messages depuis un index (polling)
    public Object[] getMessages(int fromIndex) {
        List<String> result = new ArrayList<>();
        for (int i = fromIndex; i < messageHistory.size(); i++) {
            result.add(messageHistory.get(i));
        }
        return result.toArray();
    }

    // Nombre total de messages (pour le polling)
    public int getMessageCount() {
        return messageHistory.size();
    }

    // Liste des utilisateurs connectés
    public Object[] getConnectedUsers() {
        return connectedUsers.toArray();
    }
}
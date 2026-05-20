import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;

public class ChatRoomImpl extends UnicastRemoteObject implements ChatRoom {

    // Table pseudo -> référence distante du ChatUser
    private Hashtable<String, ChatUser> users;

    public ChatRoomImpl() throws RemoteException {
        super();
        users = new Hashtable<>();
    }

    @Override
    public synchronized void subscribe(ChatUser user, String pseudo)
            throws RemoteException {
        users.put(pseudo, user);
        System.out.println("[Serveur] " + pseudo + " a rejoint le chat.");
        // Notifier tous les autres
        broadcast("*** " + pseudo + " a rejoint la salle ***");
    }

    @Override
    public synchronized void unsubscribe(String pseudo) throws RemoteException {
        users.remove(pseudo);
        System.out.println("[Serveur] " + pseudo + " a quitté le chat.");
        broadcast("*** " + pseudo + " a quitté la salle ***");
    }

    @Override
    public synchronized void postMessage(String pseudo, String message)
            throws RemoteException {
        String fullMessage = "[" + pseudo + "] " + message;
        System.out.println(fullMessage);
        broadcast(fullMessage);
    }

    // Envoyer un message à tous les utilisateurs connectés
    private void broadcast(String message) {
        for (String pseudo : users.keySet()) {
            try {
                users.get(pseudo).displayMessage(message);
            } catch (RemoteException e) {
                System.err.println("[Serveur] Erreur envoi vers " + pseudo + " : " + e.getMessage());
                // Supprimer le client déconnecté
                users.remove(pseudo);
            }
        }
    }
}
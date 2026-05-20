package chat;


import jakarta.jws.WebService;
import java.util.*;

@WebService(endpointInterface = "chat.IChatRoom")
public class ChatRoomImpl implements IChatRoom {

    private static final Map<String, Long> users    = new LinkedHashMap<>();
    private static final List<String>      messages = new ArrayList<>();

    @Override
    public synchronized String subscribe(String pseudo) {
        if (users.containsKey(pseudo))
            return "Erreur : pseudo déjà utilisé";
        users.put(pseudo, System.currentTimeMillis());
        addMessage("*** " + pseudo + " a rejoint le chat ***");
        return "Connecté en tant que " + pseudo;
    }

    @Override
    public synchronized String unsubscribe(String pseudo) {
        if (!users.containsKey(pseudo))
            return "Erreur : utilisateur introuvable";
        users.remove(pseudo);
        addMessage("*** " + pseudo + " a quitté le chat ***");
        return "Déconnecté";
    }

    @Override
    public synchronized String postMessage(String pseudo, String message) {
        if (!users.containsKey(pseudo))
            return "Erreur : vous n'êtes pas connecté";
        addMessage("[" + pseudo + "] " + message);
        return "Message envoyé";
    }

    @Override
    public synchronized String[] getMessages(int fromIndex) {
        if (fromIndex < 0) fromIndex = 0;
        List<String> result = new ArrayList<>();
        for (int i = fromIndex; i < messages.size(); i++)
            result.add(messages.get(i));
        return result.toArray(new String[0]);
    }

    @Override
    public synchronized int getMessageCount() {
        return messages.size();
    }

    @Override
    public synchronized String[] getConnectedUsers() {
        return users.keySet().toArray(new String[0]);
    }

    private void addMessage(String msg) {
        messages.add(msg);
        System.out.println(msg);
    }
}
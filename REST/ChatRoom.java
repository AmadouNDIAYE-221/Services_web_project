import java.util.*;

public class ChatRoom {

    private static final Map<String, Long> users = new LinkedHashMap<>();
    private static final List<String> messages = new ArrayList<>();

    public static synchronized String subscribe(String pseudo) {
        if (users.containsKey(pseudo))
            return "{\"status\":\"error\",\"message\":\"Pseudo déjà utilisé\"}";
        users.put(pseudo, System.currentTimeMillis());
        addMessage("*** " + pseudo + " a rejoint le chat ***");
        return "{\"status\":\"ok\",\"message\":\"Connecté en tant que " + pseudo + "\"}";
    }

    public static synchronized String unsubscribe(String pseudo) {
        if (!users.containsKey(pseudo))
            return "{\"status\":\"error\",\"message\":\"Utilisateur introuvable\"}";
        users.remove(pseudo);
        addMessage("*** " + pseudo + " a quitté le chat ***");
        return "{\"status\":\"ok\",\"message\":\"Déconnecté\"}";
    }

    public static synchronized String postMessage(String pseudo, String message) {
        if (!users.containsKey(pseudo))
            return "{\"status\":\"error\",\"message\":\"Non connecté\"}";
        addMessage("[" + pseudo + "] " + message);
        return "{\"status\":\"ok\",\"message\":\"Message envoyé\"}";
    }

    public static synchronized String getMessages(int fromIndex) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = fromIndex; i < messages.size(); i++) {
            sb.append("\"").append(messages.get(i)
                .replace("\\", "\\\\")
                .replace("\"", "\\\""))
                .append("\"");
            if (i < messages.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static synchronized int getMessageCount() {
        return messages.size();
    }

    public static synchronized String getUsers() {
        StringBuilder sb = new StringBuilder("[");
        List<String> keys = new ArrayList<>(users.keySet());
        for (int i = 0; i < keys.size(); i++) {
            sb.append("\"").append(keys.get(i)).append("\"");
            if (i < keys.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void addMessage(String msg) {
        messages.add(msg);
        System.out.println(msg);
    }
}
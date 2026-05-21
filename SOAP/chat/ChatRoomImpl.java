package chat;

import jakarta.jws.WebService;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implémentation du service SOAP ChatRoom.
 *
 * PRINCIPE : chaque utilisateur possède sa propre file de messages
 * (boîte de réception). Les messages globaux vont dans TOUTES les boîtes,
 * les messages de canal uniquement dans les boîtes des membres du canal,
 * les invitations uniquement dans la boîte du destinataire.
 */
@WebService(endpointInterface = "chat.IChatRoom")
public class ChatRoomImpl implements IChatRoom {

    // ── Utilisateurs connectés ──────────────────────────────────────────
    private static final List<String> connectedUsers = new CopyOnWriteArrayList<>();

    // ── Boîte de réception par utilisateur : pseudo → ses messages ──────
    // Chaque user ne voit QUE ses propres messages
    private static final Map<String, List<String>> inboxes =
        Collections.synchronizedMap(new HashMap<>());

    // ── Canaux : canal → liste des membres ──────────────────────────────
    private static final Map<String, Set<String>> channels =
        Collections.synchronizedMap(new HashMap<>());

    // ── Init : canaux par défaut ─────────────────────────────────────────
    static {
        channels.put("#général",   Collections.synchronizedSet(new HashSet<>()));
        channels.put("#annonces",  Collections.synchronizedSet(new HashSet<>()));
        channels.put("#aide",      Collections.synchronizedSet(new HashSet<>()));
    }

    // ════════════════════════════════════════════════════════════════════
    //  Connexion / déconnexion
    // ════════════════════════════════════════════════════════════════════
    @Override
    public String subscribe(String pseudo) {
        if (!connectedUsers.contains(pseudo)) {
            connectedUsers.add(pseudo);
            inboxes.put(pseudo, new CopyOnWriteArrayList<>());
            // Rejoindre #général automatiquement
            channels.get("#général").add(pseudo);
            // Annoncer à TOUS
            broadcastSystem("*** " + pseudo + " a rejoint le chat ***");
        }
        return "Connecté en tant que " + pseudo;
    }

    @Override
    public String unsubscribe(String pseudo) {
        connectedUsers.remove(pseudo);
        // Retirer de tous les canaux
        for (Set<String> membres : channels.values()) membres.remove(pseudo);
        broadcastSystem("*** " + pseudo + " a quitté le chat ***");
        inboxes.remove(pseudo);
        return pseudo + " déconnecté.";
    }

    // ════════════════════════════════════════════════════════════════════
    //  Messages globaux (visible par tous)
    // ════════════════════════════════════════════════════════════════════
    @Override
    public void postMessage(String pseudo, String message) {
        String formatted = "[" + pseudo + "] " + message;
        deliverToAll(formatted);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Messages de canal (visible UNIQUEMENT par les membres du canal)
    // ════════════════════════════════════════════════════════════════════
    @Override
    public void postChannelMessage(String pseudo, String canal, String message) {
        Set<String> membres = channels.get(canal);
        if (membres == null || !membres.contains(pseudo)) {
            // L'expéditeur n'est pas membre : on lui signale
            deliver(pseudo, "⚠️ Vous n'êtes pas membre de " + canal);
            return;
        }
        String formatted = "[" + canal + "][" + pseudo + "] " + message;
        // Livrer uniquement aux membres du canal
        for (String membre : membres) {
            deliver(membre, formatted);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Invitation privée (visible UNIQUEMENT par le destinataire)
    // ════════════════════════════════════════════════════════════════════
    @Override
    public void sendInvitation(String expéditeur, String destinataire, String canal) {
        // Créer le canal s'il n'existe pas
        channels.putIfAbsent(canal, Collections.synchronizedSet(new HashSet<>()));
        // Livrer UNIQUEMENT au destinataire
        String msg = "[INVITATION]" + expéditeur + "|" + canal;
        deliver(destinataire, msg);
        // Confirmer à l'expéditeur
        deliver(expéditeur, "📨 Invitation envoyée à " + destinataire + " pour " + canal);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Gestion des canaux
    // ════════════════════════════════════════════════════════════════════
    @Override
    public void joinChannel(String pseudo, String canal) {
        channels.putIfAbsent(canal, Collections.synchronizedSet(new HashSet<>()));
        channels.get(canal).add(pseudo);
        // Annoncer dans le canal
        String msg = "[" + canal + "][***] " + pseudo + " a rejoint " + canal;
        for (String m : channels.get(canal)) deliver(m, msg);
    }

    @Override
    public void leaveChannel(String pseudo, String canal) {
        Set<String> membres = channels.get(canal);
        if (membres != null) {
            membres.remove(pseudo);
            String msg = "[" + canal + "][***] " + pseudo + " a quitté " + canal;
            for (String m : membres) deliver(m, msg);
        }
    }

    @Override
    public String[] getChannelMembers(String canal) {
        Set<String> membres = channels.get(canal);
        if (membres == null) return new String[0];
        return membres.toArray(new String[0]);
    }

    @Override
    public String[] getChannels() {
        return channels.keySet().toArray(new String[0]);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Lecture des messages (boîte personnelle)
    // ════════════════════════════════════════════════════════════════════
    @Override
    public int getMessageCount(String pseudo) {
        List<String> inbox = inboxes.get(pseudo);
        return inbox == null ? 0 : inbox.size();
    }

    @Override
    public String[] getMessages(String pseudo, int fromIndex) {
        List<String> inbox = inboxes.get(pseudo);
        if (inbox == null || fromIndex >= inbox.size()) return new String[0];
        List<String> sub = new ArrayList<>(inbox.subList(fromIndex, inbox.size()));
        return sub.toArray(new String[0]);
    }

    @Override
    public String[] getConnectedUsers() {
        return connectedUsers.toArray(new String[0]);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Helpers internes
    // ════════════════════════════════════════════════════════════════════

    /** Livrer un message à un utilisateur précis */
    private void deliver(String pseudo, String message) {
        List<String> inbox = inboxes.get(pseudo);
        if (inbox != null) inbox.add(message);
    }

    /** Livrer un message à TOUS les connectés */
    private void deliverToAll(String message) {
        for (String u : connectedUsers) deliver(u, message);
    }

    /** Message système envoyé à tous */
    private void broadcastSystem(String message) {
        deliverToAll(message);
    }
}
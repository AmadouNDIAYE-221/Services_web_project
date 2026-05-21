package chat;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public interface IChatRoom {

    @WebMethod String   subscribe(String pseudo);
    @WebMethod String   unsubscribe(String pseudo);

    /** Message global (canal #général) */
    @WebMethod void     postMessage(String pseudo, String message);

    /** Message dans un canal spécifique — seuls les membres le voient */
    @WebMethod void     postChannelMessage(String pseudo, String canal, String message);

    /** Invitation privée : seul le destinataire la reçoit */
    @WebMethod void     sendInvitation(String expéditeur, String destinataire, String canal);

    /** Rejoindre un canal */
    @WebMethod void     joinChannel(String pseudo, String canal);

    /** Quitter un canal */
    @WebMethod void     leaveChannel(String pseudo, String canal);

    /** Membres d'un canal */
    @WebMethod String[] getChannelMembers(String canal);

    /** Messages globaux + canal depuis un index */
    @WebMethod int      getMessageCount(String pseudo);
    @WebMethod String[] getMessages(String pseudo, int fromIndex);

    /** Tous les utilisateurs connectés */
    @WebMethod String[] getConnectedUsers();

    /** Canaux existants */
    @WebMethod String[] getChannels();
}
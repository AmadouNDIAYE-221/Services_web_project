import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class ChatUserImpl extends UnicastRemoteObject implements ChatUser {

    public ChatUserImpl() throws RemoteException {
        super();
    }

    @Override
    public void displayMessage(String message) throws RemoteException {
        // Affiche le message reçu dans la console du client
        System.out.println(message);
    }
}
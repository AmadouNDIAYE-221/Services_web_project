import java.rmi.*;

public interface ChatUser extends Remote {
    public void displayMessage(String message) throws RemoteException;
}
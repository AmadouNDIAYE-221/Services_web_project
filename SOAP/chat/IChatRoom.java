package chat;


import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService
public interface IChatRoom {

    @WebMethod
    String subscribe(@WebParam(name = "pseudo") String pseudo);

    @WebMethod
    String unsubscribe(@WebParam(name = "pseudo") String pseudo);

    @WebMethod
    String postMessage(
        @WebParam(name = "pseudo")  String pseudo,
        @WebParam(name = "message") String message
    );

    @WebMethod
    String[] getMessages(@WebParam(name = "fromIndex") int fromIndex);

    @WebMethod
    int getMessageCount();

    @WebMethod
    String[] getConnectedUsers();
}
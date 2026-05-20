import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.webserver.WebServer;

public class ChatServer {
    public static void main(String[] args) throws Exception {
        // Création du serveur HTTP sur le port 8080
        WebServer Server = new WebServer(8181);

        // Récupération du serveur XML-RPC à partir du WebServer
        XmlRpcServer xmlRpcServer = Server.getXmlRpcServer();

        // Création du handler mapping
        PropertyHandlerMapping phm = new PropertyHandlerMapping();

        // Enregistrement de la classe ChatRoom
        phm.addHandler("ChatRoom", ChatRoom.class);

        // Affectation du handler au serveur XML-RPC
        xmlRpcServer.setHandlerMapping(phm);

        // Démarrage du serveur
        Server.start();

        System.out.println("Serveur Chat XML-RPC démarré sur le port 8080...");
    }
}

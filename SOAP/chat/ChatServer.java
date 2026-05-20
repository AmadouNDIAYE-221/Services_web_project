package chat;

import jakarta.xml.ws.Endpoint;

public class ChatServer {
    public static void main(String[] args) {
        String url = "http://localhost:9090/chat";
        Endpoint.publish(url, new ChatRoomImpl());
        System.out.println("=== Serveur Chat SOAP démarré ===");
        System.out.println("URL du service : " + url);
        System.out.println("WSDL           : " + url + "?wsdl");
        System.out.println("En attente de connexions...");
    }
}
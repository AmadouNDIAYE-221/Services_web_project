import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

public class ChatServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // POST /chat/subscribe?pseudo=Alice
        server.createContext("/chat/subscribe", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            String pseudo = getParam(exchange, "pseudo");
            sendResponse(exchange, 200, ChatRoom.subscribe(pseudo));
        });

        // DELETE /chat/unsubscribe?pseudo=Alice
        server.createContext("/chat/unsubscribe", exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) {
                sendResponse(exchange, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            String pseudo = getParam(exchange, "pseudo");
            sendResponse(exchange, 200, ChatRoom.unsubscribe(pseudo));
        });

        // POST /chat/messages?pseudo=Alice&message=Bonjour
        server.createContext("/chat/messages", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                // Envoi d'un message
                String pseudo  = getParam(exchange, "pseudo");
                String message = getParam(exchange, "message");
                sendResponse(exchange, 201, ChatRoom.postMessage(pseudo, message));

            } else if (exchange.getRequestMethod().equals("GET")) {
                // Récupération des messages (polling)
                String fromStr = getParam(exchange, "from");
                int from = 0;
                try { from = Integer.parseInt(fromStr); }
                catch (NumberFormatException ignored) {}

                String body = "{\"count\":" + ChatRoom.getMessageCount()
                            + ",\"messages\":" + ChatRoom.getMessages(from) + "}";
                sendResponse(exchange, 200, body);

            } else {
                sendResponse(exchange, 405, "{\"error\":\"Méthode non autorisée\"}");
            }
        });

        // GET /chat/users
        server.createContext("/chat/users", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendResponse(exchange, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            sendResponse(exchange, 200,
                "{\"users\":" + ChatRoom.getUsers() + "}");
        });

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("=== Serveur Chat REST démarré sur http://localhost:8080 ===");
        System.out.println("Endpoints disponibles :");
        System.out.println("  POST   /chat/subscribe?pseudo=<pseudo>");
        System.out.println("  DELETE /chat/unsubscribe?pseudo=<pseudo>");
        System.out.println("  POST   /chat/messages?pseudo=<pseudo>&message=<msg>");
        System.out.println("  GET    /chat/messages?from=<index>");
        System.out.println("  GET    /chat/users");
    }

    // Envoyer une réponse JSON
    private static void sendResponse(HttpExchange ex, int code, String body)
            throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Extraire un paramètre de la query string (?clé=valeur)
    private static String getParam(HttpExchange ex, String key) throws IOException {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], "UTF-8");
            }
        }
        return "";
    }
}
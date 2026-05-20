import java.io.*;
import java.net.*;

public class ChatClient {

    private static final String BASE = "http://localhost:8080/chat";

    public static void main(String[] args) throws Exception {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // 1. S'inscrire
        System.out.print("Entrez votre pseudo : ");
        String pseudo = scanner.nextLine().trim();

        String resp = post(BASE + "/subscribe?pseudo=" + encode(pseudo), "POST");
        System.out.println("Serveur : " + resp);
        System.out.println("=== Tapez 'exit' pour quitter ===\n");

        // 2. Thread de polling
        final int[] lastIndex = {0};
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String json = post(BASE + "/messages?from=" + lastIndex[0], "GET");
                    // Extraction manuelle du count et des messages (sans lib JSON)
                    int count = extractInt(json, "count");
                    if (count > lastIndex[0]) {
                        String[] msgs = extractArray(json, "messages");
                        for (String msg : msgs) System.out.println(msg);
                        lastIndex[0] = count;
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Erreur polling : " + e.getMessage());
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        // 3. Boucle d'envoi
        while (true) {
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                post(BASE + "/unsubscribe?pseudo=" + encode(pseudo), "DELETE");
                System.out.println("Déconnecté.");
                break;
            }
            if (!message.isEmpty()) {
                post(BASE + "/messages?pseudo=" + encode(pseudo)
                          + "&message=" + encode(message), "POST");
            }
        }
        poller.interrupt();
        scanner.close();
    }

    // Requête HTTP générique
    private static String post(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        if (method.equals("POST") || method.equals("DELETE")) {
            conn.setDoOutput(true);
            conn.getOutputStream().close(); // corps vide
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // Extraire un entier d'un JSON simple : {"count":5,...}
    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int start = idx + search.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    // Extraire un tableau JSON de strings : ["msg1","msg2"]
    private static String[] extractArray(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return new String[0];
        start = json.indexOf('[', start) + 1;
        int end = json.indexOf(']', start);
        String content = json.substring(start, end).trim();
        if (content.isEmpty()) return new String[0];
        // Séparer les éléments entre guillemets
        java.util.List<String> result = new java.util.ArrayList<>();
        int i = 0;
        while (i < content.length()) {
            if (content.charAt(i) == '"') {
                int j = content.indexOf('"', i + 1);
                if (j > i) {
                    result.add(content.substring(i + 1, j)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\"));
                    i = j + 1;
                } else break;
            } else i++;
        }
        return result.toArray(new String[0]);
    }

    private static String encode(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }
}
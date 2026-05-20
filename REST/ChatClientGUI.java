import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChatClientGUI extends JFrame {

    private static final String BASE =
            "http://localhost:8080/chat";

    private JTextPane zoneMessages;
    private JTextField champMessage;
    private JButton boutonEnvoyer;

    private JScrollPane scrollPane;

    private String pseudo;
    private int lastIndex = 0;

    public ChatClientGUI(String pseudo) {

        this.pseudo = pseudo;

        // Fenêtre
        setTitle("Chat REST - " + pseudo);
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Zone messages
        zoneMessages = new JTextPane();
        zoneMessages.setEditable(false);
        zoneMessages.setFont(new Font("Consolas", Font.PLAIN, 14));

        // Bloquer complètement le caret auto-scroll
        DefaultCaret caret = (DefaultCaret) zoneMessages.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        scrollPane = new JScrollPane(zoneMessages);
        scrollPane.setAutoscrolls(false);

        // Champ message
        champMessage = new JTextField();
        boutonEnvoyer = new JButton("Envoyer");

        JPanel bas = new JPanel(new BorderLayout());
        bas.add(champMessage, BorderLayout.CENTER);
        bas.add(boutonEnvoyer, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bas, BorderLayout.SOUTH);

        // Connexion serveur
        try {
            String resp = sendPOST(BASE + "/subscribe?pseudo=" + pseudo);
            addMessage(resp, Color.BLUE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Actions
        boutonEnvoyer.addActionListener(e -> envoyer());
        champMessage.addActionListener(e -> envoyer());

        // Déconnexion propre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    sendDELETE(BASE + "/unsubscribe?pseudo=" + pseudo);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Polling REST
        startPolling();

        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // ENVOI MESSAGE
    // -------------------------------------------------------------------------
    private void envoyer() {
        try {
            String msg = champMessage.getText();
            if (!msg.trim().isEmpty()) {
                sendPOST(BASE + "/messages?pseudo="
                        + pseudo + "&message="
                        + URLEncoder.encode(msg, "UTF-8"));
                champMessage.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // POLLING
    // -------------------------------------------------------------------------
    private void startPolling() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    String json = sendGET(BASE + "/messages?from=" + lastIndex);

                    int count = extractCount(json);
                    String[] msgs = extractMessages(json);

                    for (String m : msgs) {
                        if (!m.isEmpty()) {
                            afficher(m);
                        }
                    }

                    lastIndex = count;

                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // AFFICHAGE
    // -------------------------------------------------------------------------
    private void afficher(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.startsWith("***")) {
                addMessage(msg, Color.BLUE);
            } else if (msg.startsWith("[" + pseudo + "]")) {
                addMessage(msg, new Color(0, 140, 0));
            } else {
                addMessage(msg, Color.RED);
            }
        });
    }

    // -------------------------------------------------------------------------
    // AJOUT MESSAGE — SCROLL FIXÉ
    // Sauvegarde la position du scroll AVANT l'insertion,
    // puis la restaure APRÈS pour empêcher tout défilement automatique.
    // -------------------------------------------------------------------------
    private void addMessage(String text, Color color) {

        StyledDocument doc = zoneMessages.getStyledDocument();
        Style style = zoneMessages.addStyle("style", null);
        StyleConstants.setForeground(style, color);

        // Sauvegarder la position de la scrollbar avant insertion
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        int scrollValue   = scrollBar.getValue();
        int scrollMax     = scrollBar.getMaximum();
        int scrollVisible = scrollBar.getVisibleAmount();

        // Était-on tout en bas ? (tolérance de 5px)
        boolean wasAtBottom = (scrollValue + scrollVisible) >= (scrollMax - 5);

        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Restaurer la position après insertion (le layout n'est pas encore mis
        // à jour → on passe par invokeLater pour avoir le nouveau maximum)
        SwingUtilities.invokeLater(() -> {
            if (wasAtBottom) {
                // L'utilisateur était en bas → suivre les nouveaux messages
                scrollBar.setValue(scrollBar.getMaximum());
            } else {
                // L'utilisateur a remonté → ne pas bouger
                scrollBar.setValue(scrollValue);
            }
        });
    }

    // -------------------------------------------------------------------------
    // REQUÊTES HTTP
    // -------------------------------------------------------------------------
    private String sendGET(String url) throws Exception {
        return request(url, "GET");
    }

    private String sendPOST(String url) throws Exception {
        return request(url, "POST");
    }

    private String sendDELETE(String url) throws Exception {
        return request(url, "DELETE");
    }

    private String request(String urlStr, String method) throws Exception {

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(method);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (!method.equals("GET")) {
            conn.setDoOutput(true);
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // PARSING JSON — ROBUSTE
    // -------------------------------------------------------------------------

    /**
     * Extrait le champ "count" du JSON retourné par le serveur.
     * Format attendu : {"count":12,"messages":[...]}
     */
    private int extractCount(String json) {
        try {
            int i = json.indexOf("\"count\":") + 8;
            // Chercher la fin du nombre (virgule ou accolade fermante)
            int j = i;
            while (j < json.length() && (Character.isDigit(json.charAt(j)))) {
                j++;
            }
            return Integer.parseInt(json.substring(i, j).trim());
        } catch (Exception e) {
            return lastIndex; // Garder l'index courant en cas d'erreur
        }
    }

    /**
     * Extrait le tableau de messages du JSON.
     * Parse correctement les strings JSON pour gérer les virgules
     * et les caractères échappés dans les messages.
     *
     * Format attendu : {"count":12,"messages":["msg1","msg2","..."]}
     */
    private String[] extractMessages(String json) {
        try {
            int i = json.indexOf("[");
            int j = json.lastIndexOf("]");

            if (i == -1 || j == -1 || j <= i) return new String[0];

            String content = json.substring(i + 1, j).trim();
            if (content.isEmpty()) return new String[0];

            List<String> messages = new ArrayList<>();
            int pos = 0;

            while (pos < content.length()) {

                // Chercher le prochain guillemet ouvrant
                int start = content.indexOf("\"", pos);
                if (start == -1) break;

                // Chercher le guillemet fermant en gérant les échappements
                int end = start + 1;
                while (end < content.length()) {
                    char c = content.charAt(end);
                    if (c == '\\') {
                        end += 2; // Sauter le caractère échappé
                        continue;
                    }
                    if (c == '"') break;
                    end++;
                }

                if (end >= content.length()) break;

                // Décoder les séquences échappées courantes
                String msg = content.substring(start + 1, end)
                        .replace("\\n",  "\n")
                        .replace("\\t",  "\t")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");

                messages.add(msg);
                pos = end + 1;
            }

            return messages.toArray(new String[0]);

        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    // -------------------------------------------------------------------------
    // MAIN
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String pseudo = JOptionPane.showInputDialog("Pseudo :");
            if (pseudo != null && !pseudo.trim().isEmpty()) {
                new ChatClientGUI(pseudo);
            }
        });
    }
}
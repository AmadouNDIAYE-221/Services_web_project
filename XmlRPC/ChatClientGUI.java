import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;

import java.net.URL;

public class ChatClientGUI extends JFrame {

    private JTextPane zoneMessages;

    private JTextField champMessage;

    private JButton boutonEnvoyer;

    private XmlRpcClient client;

    private String pseudo;

    private int lastIndex = 0;

    public ChatClientGUI(String pseudo) {

        this.pseudo = pseudo;

        // Fenêtre
        setTitle("Chat XML-RPC - " + pseudo);

        setSize(600, 450);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        // Zone messages
        zoneMessages = new JTextPane();

        zoneMessages.setEditable(false);

        zoneMessages.setFont(
                new Font("Consolas", Font.PLAIN, 14));

        JScrollPane scroll =
                new JScrollPane(zoneMessages);

        // Champ message
        champMessage = new JTextField();

        champMessage.setFont(
                new Font("Arial", Font.PLAIN, 14));

        // Bouton
        boutonEnvoyer = new JButton("Envoyer");

        // Panel bas
        JPanel bas =
                new JPanel(new BorderLayout());

        bas.add(champMessage, BorderLayout.CENTER);

        bas.add(boutonEnvoyer, BorderLayout.EAST);

        // Ajout composants
        add(scroll, BorderLayout.CENTER);

        add(bas, BorderLayout.SOUTH);

        // Connexion XML-RPC
        try {

            XmlRpcClientConfigImpl config =
                    new XmlRpcClientConfigImpl();

            config.setServerURL(
                    new URL(
                            "http://localhost:8181/RPC2"));

            client = new XmlRpcClient();

            client.setConfig(config);

            // Subscribe
            String resp =
                    (String) client.execute(
                            "ChatRoom.subscribe",
                            new Object[]{pseudo});

            ajouterMessageColore(
                    resp,
                    Color.BLUE);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Erreur connexion serveur");

            e.printStackTrace();
        }

        // Envoi message
        boutonEnvoyer.addActionListener(
                e -> envoyerMessage());

        champMessage.addActionListener(
                e -> envoyerMessage());

        // Fermeture
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                try {

                    client.execute(
                            "ChatRoom.unsubscribe",
                            new Object[]{pseudo});

                } catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        });

        // Démarrer polling
        demarrerPolling();

        setVisible(true);
    }

    // Envoi message
    private void envoyerMessage() {

        try {

            String msg =
                    champMessage.getText();

            if (!msg.trim().isEmpty()) {

                client.execute(
                        "ChatRoom.postMessage",
                        new Object[]{
                                pseudo,
                                msg
                        });

                champMessage.setText("");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // Polling automatique
    private void demarrerPolling() {

        Thread poller = new Thread(() -> {

            while (true) {

                try {

                    int count =
                            (Integer) client.execute(
                                    "ChatRoom.getMessageCount",
                                    new Object[]{});

                    if (count > lastIndex) {

                        Object[] messages =
                                (Object[]) client.execute(
                                        "ChatRoom.getMessages",
                                        new Object[]{
                                                lastIndex
                                        });

                        for (Object msg : messages) {

                            afficherMessage(
                                    msg.toString());
                        }

                        lastIndex = count;
                    }

                    Thread.sleep(500);

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        });

        poller.setDaemon(true);

        poller.start();
    }

    // Affichage coloré
    private void afficherMessage(String message) {

        SwingUtilities.invokeLater(() -> {

            // Messages système
            if (message.startsWith("***")) {

                ajouterMessageColore(
                        message,
                        Color.BLUE);
            }

            // Mes messages
            else if (
                    message.startsWith(
                            "[" + pseudo + "]")) {

                ajouterMessageColore(
                        message,
                        new Color(0, 140, 0));
            }

            // Autres messages
            else {

                ajouterMessageColore(
                        message,
                        Color.RED);
            }

            zoneMessages.setCaretPosition(
                    zoneMessages
                            .getDocument()
                            .getLength());
        });
    }

    // Ajouter texte coloré
    private void ajouterMessageColore(
            String message,
            Color color) {

        StyledDocument doc =
                zoneMessages.getStyledDocument();

        Style style =
                zoneMessages.addStyle(
                        "ColorStyle",
                        null);

        StyleConstants.setForeground(
                style,
                color);

        try {

            doc.insertString(
                    doc.getLength(),
                    message + "\n",
                    style);

        } catch (BadLocationException e) {

            e.printStackTrace();
        }
    }

    // Main
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            String pseudo =
                    JOptionPane.showInputDialog(
                            null,
                            "Entrez votre pseudo :",
                            "Connexion",
                            JOptionPane.PLAIN_MESSAGE);

            if (pseudo != null
                    && !pseudo.trim().isEmpty()) {

                new ChatClientGUI(pseudo);
            }
        });
    }
}
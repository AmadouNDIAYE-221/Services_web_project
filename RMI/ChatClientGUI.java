import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;

public class ChatClientGUI extends JFrame implements ChatUser {

    private JTextPane zoneMessages;

    private JTextField champMessage;

    private JButton boutonEnvoyer;

    private ChatRoom room;

    private String username;

    public ChatClientGUI(String username) {

        this.username = username;

        try {

            // Export RMI callback
            UnicastRemoteObject.exportObject(this, 0);

        } catch (RemoteException e) {

            e.printStackTrace();
        }

        // Configuration fenêtre
        setTitle("Chat RMI - " + username);

        setSize(600, 450);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        // Zone des messages
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

        // Bouton envoyer
        boutonEnvoyer = new JButton("Envoyer");

        boutonEnvoyer.setFont(
                new Font("Arial", Font.BOLD, 14));

        // Panel bas
        JPanel panelBas =
                new JPanel(new BorderLayout());

        panelBas.add(champMessage, BorderLayout.CENTER);

        panelBas.add(boutonEnvoyer, BorderLayout.EAST);

        // Ajout composants
        add(scroll, BorderLayout.CENTER);

        add(panelBas, BorderLayout.SOUTH);

        // Connexion serveur
        try {

            room = (ChatRoom)
                    Naming.lookup("//localhost/ChatRoom");

            room.subscribe(this, username);

            ajouterMessageColore(
                    "*** Connecté au serveur ***",
                    Color.BLUE);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Erreur connexion serveur : "
                            + e.getMessage());

            e.printStackTrace();
        }

        // Bouton envoyer
        boutonEnvoyer.addActionListener(
                e -> envoyerMessage());

        // Touche entrée
        champMessage.addActionListener(
                e -> envoyerMessage());

        // Déconnexion fermeture
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                try {

                    room.unsubscribe(username);

                } catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    // Envoyer message
    private void envoyerMessage() {

        try {

            String msg = champMessage.getText();

            if (!msg.trim().isEmpty()) {

                room.postMessage(username, msg);

                champMessage.setText("");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // Ajouter message coloré
    private void ajouterMessageColore(
            String message,
            Color color) {

        StyledDocument doc =
                zoneMessages.getStyledDocument();

        Style style =
                zoneMessages.addStyle(
                        "ColorStyle",
                        null);

        StyleConstants.setForeground(style, color);

        try {

            doc.insertString(
                    doc.getLength(),
                    message + "\n",
                    style);

        } catch (BadLocationException e) {

            e.printStackTrace();
        }
    }

    // Callback RMI
    @Override
    public void displayMessage(String message)
            throws RemoteException {

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
                            "[" + username + "]")) {

                ajouterMessageColore(
                        message,
                        new Color(0, 140, 0));

            }

            // Messages autres utilisateurs
            else {

                ajouterMessageColore(
                        message,
                        Color.RED);
            }

            // Scroll automatique
            zoneMessages.setCaretPosition(
                    zoneMessages
                            .getDocument()
                            .getLength());
        });
    }

    // Main
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            String username =
                    JOptionPane.showInputDialog(
                            null,
                            "Entrez votre pseudo :",
                            "Connexion",
                            JOptionPane.PLAIN_MESSAGE);

            if (username != null
                    && !username.trim().isEmpty()) {

                new ChatClientGUI(username);

            } else {

                JOptionPane.showMessageDialog(
                        null,
                        "Pseudo invalide");
            }
        });
    }
}
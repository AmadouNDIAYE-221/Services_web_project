package chat;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;

import java.net.URL;
import jakarta.xml.ws.Service;

import javax.xml.namespace.QName;

public class ChatClientGUI extends JFrame {

    private IChatRoom room;

    private JTextPane zoneMessages;
    private JTextField champMessage;
    private JButton bouton;

    private String pseudo;
    private int lastIndex = 0;

    public ChatClientGUI(String pseudo) throws Exception {

        this.pseudo = pseudo;

        // SOAP INIT
        URL wsdlURL = new URL("http://localhost:9090/chat?wsdl");
        QName svcName = new QName("http://chat/", "ChatRoomImplService");
        QName portName = new QName("http://chat/", "ChatRoomImplPort");

        Service service = Service.create(wsdlURL, svcName);
        room = service.getPort(portName, IChatRoom.class);

        // UI
        setTitle("SOAP Chat - " + pseudo);
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        zoneMessages = new JTextPane();
        zoneMessages.setEditable(false);
        zoneMessages.setFont(new Font("Consolas", Font.PLAIN, 14));

        JScrollPane scroll = new JScrollPane(zoneMessages);

        champMessage = new JTextField();
        bouton = new JButton("Envoyer");

        JPanel bas = new JPanel(new BorderLayout());
        bas.add(champMessage, BorderLayout.CENTER);
        bas.add(bouton, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bas, BorderLayout.SOUTH);

        // SUBSCRIBE
        String resp = room.subscribe(pseudo);
        addMessage(resp, Color.BLUE);

        // EVENTS
        bouton.addActionListener(e -> envoyer());
        champMessage.addActionListener(e -> envoyer());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    room.unsubscribe(pseudo);
                } catch (Exception ex) {}
            }
        });

        // POLLING SOAP
        startPolling();

        setVisible(true);
    }

    // ENVOI MESSAGE
    private void envoyer() {

        try {

            String msg = champMessage.getText();

            if (!msg.trim().isEmpty()) {

                room.postMessage(pseudo, msg);
                champMessage.setText("");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // POLLING
    private void startPolling() {

        Thread t = new Thread(() -> {

            while (true) {

                try {

                    int count = room.getMessageCount();

                    if (count > lastIndex) {

                        String[] msgs =
                                room.getMessages(lastIndex);

                        for (String m : msgs) {
                            afficher(m);
                        }

                        lastIndex = count;
                    }

                    Thread.sleep(500);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.setDaemon(true);
        t.start();
    }

    // AFFICHAGE
    private void afficher(String msg) {

        SwingUtilities.invokeLater(() -> {

            if (msg.startsWith("***")) {
                addMessage(msg, Color.BLUE);
            }
            else if (msg.startsWith("[" + pseudo + "]")) {
                addMessage(msg, new Color(0, 150, 0));
            }
            else {
                addMessage(msg, Color.RED);
            }
        });
    }

    // AJOUT TEXTE
    private void addMessage(String text, Color color) {

        StyledDocument doc = zoneMessages.getStyledDocument();
        Style style = zoneMessages.addStyle("style", null);

        StyleConstants.setForeground(style, color);

        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MAIN
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                String pseudo = JOptionPane.showInputDialog("Pseudo :");

                if (pseudo != null && !pseudo.trim().isEmpty()) {
                    new ChatClientGUI(pseudo);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
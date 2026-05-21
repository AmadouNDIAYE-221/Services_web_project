package chat;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;

import jakarta.xml.ws.Service;
import javax.xml.namespace.QName;

public class ChatClientGUI extends JFrame {

    // ─── Palette ──────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(18,  20,  28);
    private static final Color BG_PANEL     = new Color(25,  28,  40);
    private static final Color BG_SIDEBAR   = new Color(20,  22,  32);
    private static final Color BG_INPUT     = new Color(32,  36,  52);
    private static final Color ACCENT       = new Color(99,  102, 241);
    private static final Color ACCENT_HOVER = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY = new Color(230, 232, 245);
    private static final Color TEXT_MUTED   = new Color(120, 125, 160);
    private static final Color TEXT_ONLINE  = new Color(52,  211, 153);
    private static final Color MSG_SELF     = new Color(99,  102, 241);
    private static final Color MSG_OTHER    = new Color(230, 150,  80);
    private static final Color MSG_SYSTEM   = new Color(100, 160, 220);
    private static final Color SEPARATOR    = new Color(40,  44,  60);

    // ─── Polices ──────────────────────────────────────────────────────────
    private static final Font FONT_UI    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  15);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 13);

    // ─── SOAP ─────────────────────────────────────────────────────────────
    private IChatRoom room;

    // ─── État ─────────────────────────────────────────────────────────────
    private String pseudo;
    private int    lastIndex     = 0;
    private String currentChannel = "#général";
    private final Set<String> knownUsers    = new HashSet<>();
    private final Set<String> joinedChannels = new HashSet<>();

    // ─── Composants ───────────────────────────────────────────────────────
    private JTextPane              zoneMessages;
    private JTextField             champMessage;
    private JButton                boutonEnvoyer;
    private JList<String>          listeUtilisateurs;
    private DefaultListModel<String> modelUtilisateurs;
    private JList<String>          listeCanaux;
    private DefaultListModel<String> modelCanaux;
    private JLabel                 labelChannel;
    private JLabel                 labelNbConnectes;
    private JLabel                 labelStatut;

    // ══════════════════════════════════════════════════════════════════════
    public ChatClientGUI(String pseudo) throws Exception {
        this.pseudo = pseudo;
        URL wsdlURL  = new URL("http://localhost:9090/chat?wsdl");
        QName svcName  = new QName("http://chat/", "ChatRoomImplService");
        QName portName = new QName("http://chat/", "ChatRoomImplPort");
        Service service = Service.create(wsdlURL, svcName);
        room = service.getPort(portName, IChatRoom.class);

        buildUI();
        connecter();
        startPolling();
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Construction UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {
        setTitle("💬 SOAP Chat — " + pseudo);
        setSize(960, 640);
        setMinimumSize(new Dimension(760, 480));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        add(buildSidebar(),   BorderLayout.WEST);
        add(buildMain(),      BorderLayout.CENTER);
        add(buildUserPanel(), BorderLayout.EAST);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try { room.unsubscribe(pseudo); } catch (Exception ignored) {}
            }
        });
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SEPARATOR));

        // Header utilisateur
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BG_SIDEBAR);
        header.setBorder(new EmptyBorder(16, 14, 16, 14));
        JLabel avatar = makeAvatar(pseudo, 36, ACCENT);
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setBackground(BG_SIDEBAR);
        JLabel nomLabel = new JLabel(pseudo);
        nomLabel.setFont(FONT_BOLD.deriveFont(14f));
        nomLabel.setForeground(TEXT_PRIMARY);
        labelStatut = new JLabel("● En ligne");
        labelStatut.setFont(FONT_SMALL);
        labelStatut.setForeground(TEXT_ONLINE);
        info.add(nomLabel); info.add(labelStatut);
        header.add(avatar, BorderLayout.WEST);
        header.add(info,   BorderLayout.CENTER);

        // Section canaux
        JPanel canauxSection = new JPanel(new BorderLayout(0, 4));
        canauxSection.setBackground(BG_SIDEBAR);
        canauxSection.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel canauxHeader = new JPanel(new BorderLayout());
        canauxHeader.setBackground(BG_SIDEBAR);
        canauxHeader.setBorder(new EmptyBorder(0, 14, 6, 8));
        JLabel canauxLabel = new JLabel("CANAUX");
        canauxLabel.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        canauxLabel.setForeground(TEXT_MUTED);
        JButton btnAdd = createIconButton("+");
        btnAdd.addActionListener(e -> creerGroupe());
        canauxHeader.add(canauxLabel, BorderLayout.WEST);
        canauxHeader.add(btnAdd,      BorderLayout.EAST);

        modelCanaux = new DefaultListModel<>();
        // Canaux par défaut — l'user y est déjà membre (subscribe les rejoint côté serveur)
        modelCanaux.addElement("#général");
        joinedChannels.add("#général");

        listeCanaux = new JList<>(modelCanaux);
        listeCanaux.setBackground(BG_SIDEBAR);
        listeCanaux.setFont(FONT_UI);
        listeCanaux.setSelectionBackground(new Color(50, 55, 80));
        listeCanaux.setSelectionForeground(TEXT_PRIMARY);
        listeCanaux.setFixedCellHeight(32);
        listeCanaux.setBorder(null);
        listeCanaux.setSelectedIndex(0);
        listeCanaux.setCellRenderer(new CanalCellRenderer());
        listeCanaux.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listeCanaux.getSelectedValue() != null)
                changerCanal(listeCanaux.getSelectedValue());
        });

        canauxSection.add(canauxHeader, BorderLayout.NORTH);
        canauxSection.add(listeCanaux,  BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG_SIDEBAR);
        body.add(canauxSection, BorderLayout.NORTH);
        sidebar.add(header, BorderLayout.NORTH);
        sidebar.add(body,   BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_PANEL);

        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARATOR),
            new EmptyBorder(12, 18, 12, 18)));
        labelChannel = new JLabel("#général");
        labelChannel.setFont(FONT_TITLE);
        labelChannel.setForeground(TEXT_PRIMARY);
        topBar.add(labelChannel, BorderLayout.WEST);

        zoneMessages = new JTextPane();
        zoneMessages.setEditable(false);
        zoneMessages.setBackground(BG_PANEL);
        zoneMessages.setFont(FONT_MONO);
        zoneMessages.setBorder(new EmptyBorder(10, 16, 10, 16));
        JScrollPane scroll = new JScrollPane(zoneMessages);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);

        main.add(topBar,             BorderLayout.NORTH);
        main.add(scroll,             BorderLayout.CENTER);
        main.add(buildInputPanel(),  BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR),
            new EmptyBorder(12, 16, 14, 16)));
        champMessage = new JTextField();
        champMessage.setBackground(BG_INPUT);
        champMessage.setForeground(TEXT_PRIMARY);
        champMessage.setCaretColor(ACCENT_HOVER);
        champMessage.setFont(FONT_UI);
        champMessage.setBorder(new CompoundBorder(
            new RoundedBorder(8, SEPARATOR),
            new EmptyBorder(8, 12, 8, 12)));
        boutonEnvoyer = makeStyledButton("Envoyer", ACCENT, Color.WHITE, 90, 36);
        boutonEnvoyer.addActionListener(e -> envoyer());
        champMessage.addActionListener(e -> envoyer());
        panel.add(champMessage,  BorderLayout.CENTER);
        panel.add(boutonEnvoyer, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SIDEBAR);
        panel.setPreferredSize(new Dimension(190, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, SEPARATOR));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBackground(BG_SIDEBAR);
        header.setBorder(new EmptyBorder(16, 14, 10, 14));
        JLabel titre = new JLabel("MEMBRES EN LIGNE");
        titre.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        titre.setForeground(TEXT_MUTED);
        labelNbConnectes = new JLabel("0");
        labelNbConnectes.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        labelNbConnectes.setForeground(ACCENT_HOVER);
        header.add(titre,            BorderLayout.WEST);
        header.add(labelNbConnectes, BorderLayout.EAST);

        modelUtilisateurs = new DefaultListModel<>();
        listeUtilisateurs = new JList<>(modelUtilisateurs);
        listeUtilisateurs.setBackground(BG_SIDEBAR);
        listeUtilisateurs.setFont(FONT_UI);
        listeUtilisateurs.setFixedCellHeight(40);
        listeUtilisateurs.setBorder(null);
        listeUtilisateurs.setSelectionBackground(new Color(40, 44, 64));
        listeUtilisateurs.setCellRenderer(new UserCellRenderer());
        listeUtilisateurs.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = listeUtilisateurs.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        listeUtilisateurs.setSelectedIndex(idx);
                        showUserContextMenu(listeUtilisateurs.getSelectedValue(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(listeUtilisateurs);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SIDEBAR);

        JButton btnInviter = makeStyledButton("+ Inviter", new Color(50,55,80), ACCENT_HOVER, 160, 32);
        btnInviter.addActionListener(e -> inviterUtilisateur());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(BG_SIDEBAR);
        footer.setBorder(new EmptyBorder(4, 0, 10, 0));
        footer.add(btnInviter);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(footer,  BorderLayout.SOUTH);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Logique métier
    // ══════════════════════════════════════════════════════════════════════
    private void connecter() {
        try {
            String resp = room.subscribe(pseudo);
            addMessage("⚡ " + resp, MSG_SYSTEM);
        } catch (Exception e) {
            addMessage("❌ Erreur connexion : " + e.getMessage(), Color.RED);
        }
    }

    /** Envoie dans le canal courant via postChannelMessage (SOAP) */
    private void envoyer() {
        try {
            String msg = champMessage.getText().trim();
            if (!msg.isEmpty()) {
                room.postChannelMessage(pseudo, currentChannel, msg);
                champMessage.setText("");
            }
        } catch (Exception e) {
            addMessage("❌ " + e.getMessage(), Color.RED);
        }
    }

    private void changerCanal(String canal) {
        currentChannel = canal;
        labelChannel.setText(canal);
        // Rejoindre côté serveur si pas encore membre
        if (!joinedChannels.contains(canal)) {
            try {
                room.joinChannel(pseudo, canal);
                joinedChannels.add(canal);
            } catch (Exception e) {
                addMessage("❌ Impossible de rejoindre " + canal, Color.RED);
            }
        }
        addMessage("── " + canal + " ──", TEXT_MUTED);
    }

    private void creerGroupe() {
        String nom = JOptionPane.showInputDialog(this, "Nom du canal :", "Créer un canal", JOptionPane.PLAIN_MESSAGE);
        if (nom != null && !nom.trim().isEmpty()) {
            String canal = "#" + nom.trim().toLowerCase().replaceAll("\\s+", "-");
            if (!modelCanaux.contains(canal)) {
                modelCanaux.addElement(canal);
                listeCanaux.setSelectedValue(canal, true);
                try {
                    room.joinChannel(pseudo, canal); // crée + rejoint côté serveur
                    joinedChannels.add(canal);
                } catch (Exception ignored) {}
                changerCanal(canal);
            }
        }
    }

    /** Invitation réelle via SOAP sendInvitation — seul le destinataire la reçoit */
    private void envoyerInvitation(String cible) {
        try {
            room.sendInvitation(pseudo, cible, currentChannel);
            // La confirmation arrive via notre boîte (polling)
        } catch (Exception e) {
            addMessage("❌ Erreur invitation : " + e.getMessage(), Color.RED);
        }
    }

    private void inviterUtilisateur() {
        String[] users = knownUsers.stream()
            .filter(u -> !u.equals(pseudo)).sorted().toArray(String[]::new);
        if (users.length == 0) {
            JOptionPane.showMessageDialog(this, "Aucun autre utilisateur connecté.");
            return;
        }
        String cible = (String) JOptionPane.showInputDialog(this,
            "Inviter dans " + currentChannel + " :",
            "Inviter", JOptionPane.PLAIN_MESSAGE, null, users, users[0]);
        if (cible != null) envoyerInvitation(cible);
    }

    private void showUserContextMenu(String cible, int x, int y) {
        if (cible == null || cible.equals(pseudo)) return;
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(BG_INPUT);
        JMenuItem itemInviter = new JMenuItem("📨 Inviter dans " + currentChannel);
        itemInviter.setFont(FONT_UI); itemInviter.setForeground(TEXT_PRIMARY);
        itemInviter.setBackground(BG_INPUT);
        itemInviter.addActionListener(e -> envoyerInvitation(cible));
        JMenuItem itemMP = new JMenuItem("💬 Message privé");
        itemMP.setFont(FONT_UI); itemMP.setForeground(TEXT_PRIMARY);
        itemMP.setBackground(BG_INPUT);
        itemMP.addActionListener(e -> envoyerMP(cible));
        menu.add(itemInviter); menu.addSeparator(); menu.add(itemMP);
        menu.show(listeUtilisateurs, x, y);
    }

    private void envoyerMP(String cible) {
        String contenu = JOptionPane.showInputDialog(this,
            "Message à " + cible + " :", "Message privé", JOptionPane.PLAIN_MESSAGE);
        if (contenu != null && !contenu.trim().isEmpty()) {
            try {
                // Convention MP : canal fictif "[MP→cible]"
                room.sendInvitation(pseudo, cible, "[MP→" + cible + "]:" + contenu);
            } catch (Exception e) {
                addMessage("❌ " + e.getMessage(), Color.RED);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Polling — lit la BOÎTE PERSONNELLE de l'utilisateur
    // ══════════════════════════════════════════════════════════════════════
    private void startPolling() {
        new Thread(() -> {
            while (true) {
                try {
                    // 1. Messages perso
                    int count = room.getMessageCount(pseudo);
                    if (count > lastIndex) {
                        String[] msgs = room.getMessages(pseudo, lastIndex);
                        for (String m : msgs) SwingUtilities.invokeLater(() -> afficher(m));
                        lastIndex = count;
                    }
                    // 2. Utilisateurs connectés
                    try {
                        String[] users = room.getConnectedUsers();
                        SwingUtilities.invokeLater(() -> rafraichirUtilisateurs(users));
                    } catch (Exception ignored) {}

                    // 3. Canaux disponibles (pour détecter créations par d'autres)
                    try {
                        String[] canauxServeur = room.getChannels();
                        SwingUtilities.invokeLater(() -> rafraichirCanaux(canauxServeur));
                    } catch (Exception ignored) {}

                    Thread.sleep(800);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }, "polling").start();
    }

    private void rafraichirUtilisateurs(String[] users) {
        Set<String> serverSet = new HashSet<>(Arrays.asList(users));
        for (String ancien : new HashSet<>(knownUsers)) {
            if (!serverSet.contains(ancien)) { knownUsers.remove(ancien); modelUtilisateurs.removeElement(ancien); }
        }
        for (String u : users) {
            if (!knownUsers.contains(u)) { knownUsers.add(u); modelUtilisateurs.addElement(u); }
        }
        labelNbConnectes.setText(String.valueOf(modelUtilisateurs.size()));
    }

    private void rafraichirCanaux(String[] canauxServeur) {
        for (String c : canauxServeur) {
            if (!modelCanaux.contains(c)) modelCanaux.addElement(c);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Affichage — dispatch selon le type de message
    // ══════════════════════════════════════════════════════════════════════
    private void afficher(String msg) {
        // ── Invitation me concernant ──────────────────────────────────────
        if (msg.startsWith("[INVITATION]")) {
            // Format : [INVITATION]expéditeur|canal
            String payload  = msg.substring("[INVITATION]".length());
            String[] parts  = payload.split("\\|", 2);
            String expéditeur = parts.length > 0 ? parts[0] : "?";
            String canal      = parts.length > 1 ? parts[1] : "#général";
            addInvitationBanner(expéditeur, canal);
            return;
        }
        // ── Système ───────────────────────────────────────────────────────
        if (msg.startsWith("***") || msg.contains("][***]")) {
            addMessage("⚡ " + msg, MSG_SYSTEM); return;
        }
        // ── Mes messages ─────────────────────────────────────────────────
        if (msg.contains("][" + pseudo + "]") || msg.startsWith("[" + pseudo + "]")) {
            addMessage(msg, MSG_SELF); return;
        }
        // ── Tout le reste (autres users, confirmations) ───────────────────
        addMessage(msg, MSG_OTHER);
    }

    /** Bannière interactive affichée dans le flux de messages */
    private void addInvitationBanner(String expéditeur, String canal) {
        addMessage("", TEXT_MUTED);
        JPanel banner = new JPanel(new BorderLayout(10, 0));
        banner.setBackground(new Color(40, 45, 70));
        banner.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
            new EmptyBorder(8, 12, 8, 12)));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel texte = new JLabel(
            "<html><b style='color:#C7C9F0'>📨 Invitation de " + expéditeur + "</b>"
            + "<br><span style='color:#8890B8'>Rejoindre <b>" + canal + "</b> ?</span></html>");
        texte.setFont(FONT_UI);

        JButton btnOui = makeStyledButton("✅ Rejoindre", new Color(52,211,153,80), new Color(52,211,153), 110, 28);
        JButton btnNon = makeStyledButton("✖ Ignorer",   new Color(239,68,68,60),  new Color(239,68,68),  90, 28);

        btnOui.addActionListener(e -> {
            if (!modelCanaux.contains(canal)) modelCanaux.addElement(canal);
            listeCanaux.setSelectedValue(canal, true);
            changerCanal(canal);
            banner.setVisible(false);
            addMessage("✅ Vous avez rejoint " + canal, TEXT_ONLINE);
        });
        btnNon.addActionListener(e -> {
            banner.setVisible(false);
            addMessage("✖ Invitation ignorée.", TEXT_MUTED);
        });

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        boutons.setOpaque(false);
        boutons.add(btnOui); boutons.add(btnNon);

        banner.add(texte,   BorderLayout.CENTER);
        banner.add(boutons, BorderLayout.EAST);

        StyledDocument doc = zoneMessages.getStyledDocument();
        Style style = zoneMessages.addStyle("inv_" + System.currentTimeMillis(), null);
        StyleConstants.setComponent(style, banner);
        try {
            doc.insertString(doc.getLength(), " ", style);
            doc.insertString(doc.getLength(), "\n", null);
            zoneMessages.setCaretPosition(doc.getLength());
        } catch (Exception ex) { ex.printStackTrace(); }
        addMessage("", TEXT_MUTED);
    }

    private void addMessage(String text, Color color) {
        StyledDocument doc = zoneMessages.getStyledDocument();
        Style style = zoneMessages.addStyle("s", null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontFamily(style, "Consolas");
        StyleConstants.setFontSize(style, 13);
        try {
            doc.insertString(doc.getLength(), text + "\n", style);
            zoneMessages.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers UI
    // ══════════════════════════════════════════════════════════════════════
    private JLabel makeAvatar(String name, int size, Color bg) {
        JLabel lbl = new JLabel(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg); g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(Color.WHITE); g2.setFont(FONT_BOLD.deriveFont((float)(size/2.2)));
                FontMetrics fm = g2.getFontMetrics(); String t = getText();
                g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        lbl.setPreferredSize(new Dimension(size, size));
        lbl.setOpaque(false);
        return lbl;
    }

    private JButton makeStyledButton(String label, Color bg, Color fg, int w, int h) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(fg); g2.setFont(FONT_BOLD.deriveFont(12f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setPreferredSize(new Dimension(w,h));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createIconButton(String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(50,55,80) : new Color(35,38,55));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(ACCENT_HOVER); g2.setFont(FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setPreferredSize(new Dimension(24,20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private class CanalCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list,value,index,selected,focus);
            lbl.setBackground(selected ? new Color(50,55,80) : BG_SIDEBAR);
            lbl.setForeground(selected ? TEXT_PRIMARY : TEXT_MUTED);
            lbl.setFont(selected ? FONT_BOLD : FONT_UI);
            lbl.setBorder(new EmptyBorder(4,16,4,16));
            return lbl;
        }
    }

    private class UserCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel avt = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getText().isEmpty() ? 0 : getText().charAt(0);
                Color[] pal = {new Color(99,102,241),new Color(236,72,153),new Color(16,185,129),
                               new Color(245,158,11),new Color(59,130,246),new Color(239,68,68)};
                g2.setColor(pal[Math.abs(h)%pal.length]); g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(Color.WHITE); g2.setFont(FONT_SMALL.deriveFont(Font.BOLD));
                FontMetrics fm = g2.getFontMetrics(); String t = getText();
                g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        private final JLabel nameLbl = new JLabel();
        private final JLabel dot     = new JLabel("●");
        public UserCellRenderer() {
            setLayout(new BorderLayout(8,0)); setOpaque(true); setBorder(new EmptyBorder(5,12,5,12));
            avt.setPreferredSize(new Dimension(28,28)); avt.setOpaque(false);
            nameLbl.setFont(FONT_UI); dot.setFont(new Font("Segoe UI",Font.PLAIN,9)); dot.setForeground(TEXT_ONLINE);
            JPanel r = new JPanel(new BorderLayout(4,0)); r.setOpaque(false);
            r.add(nameLbl,BorderLayout.CENTER); r.add(dot,BorderLayout.EAST);
            add(avt,BorderLayout.WEST); add(r,BorderLayout.CENTER);
        }
        @Override public Component getListCellRendererComponent(JList<? extends String> list,String value,int index,boolean selected,boolean focus) {
            setBackground(selected ? new Color(40,44,64) : BG_SIDEBAR);
            avt.setText(value!=null&&!value.isEmpty() ? String.valueOf(value.charAt(0)).toUpperCase() : "?");
            nameLbl.setText(value);
            nameLbl.setForeground(selected ? TEXT_PRIMARY : new Color(190,195,220));
            nameLbl.setFont(value!=null&&value.equals(pseudo) ? FONT_BOLD : FONT_UI);
            return this;
        }
    }

    private static class RoundedBorder implements Border {
        private final int radius; private final Color color;
        RoundedBorder(int r,Color c){radius=r;color=c;}
        @Override public Insets getBorderInsets(Component c){return new Insets(radius,radius,radius,radius);}
        @Override public boolean isBorderOpaque(){return false;}
        @Override public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.drawRoundRect(x,y,w-1,h-1,radius,radius); g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf"); }
        catch (Exception e1) {
            try { for (UIManager.LookAndFeelInfo l:UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(l.getName())){UIManager.setLookAndFeel(l.getClassName());break;}
            } catch (Exception ignored) {}
        }
        SwingUtilities.invokeLater(() -> {
            try {
                JPanel p = new JPanel(new GridBagLayout()); p.setBackground(BG_DARK);
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(6,6,6,6); g.fill = GridBagConstraints.HORIZONTAL;
                JLabel t = new JLabel("💬 SOAP Chat",JLabel.CENTER); t.setFont(FONT_TITLE.deriveFont(20f)); t.setForeground(TEXT_PRIMARY);
                g.gridx=0;g.gridy=0;g.gridwidth=2; p.add(t,g);
                JLabel l = new JLabel("Votre pseudo :"); l.setFont(FONT_UI); l.setForeground(TEXT_MUTED);
                g.gridy=1; p.add(l,g);
                JTextField f = new JTextField(18); f.setFont(FONT_UI);
                g.gridy=2; p.add(f,g);
                int r = JOptionPane.showConfirmDialog(null,p,"Connexion",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
                String ps = f.getText().trim();
                if (r==JOptionPane.OK_OPTION&&!ps.isEmpty()) new ChatClientGUI(ps);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
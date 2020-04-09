package studio.ui;

import studio.kdb.Server;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ServerList extends EscapeDialog {

    private JTree tree;
    private JTextField filter;
    private DefaultMutableTreeNode root;

    private Server selectedServer;
    private Server activeServer;
    private Server[] servers;
    private java.util.List<String> filters = new ArrayList<>();

    public ServerList(JFrame parent, Server[] servers, Server active) {
        super(parent, "Server List");
        initComponents();

        this.servers = servers;
        this.activeServer = active;
        selectedServer = active;

        refreshServers();
    }

    private void refreshFilters(String filterSting) {
        filters.clear();
        StringTokenizer st = new StringTokenizer(filterSting," \t");
        while (st.hasMoreTokens()) {
            String word = st.nextToken().trim();
            if (word.length()>0) filters.add(word.toLowerCase());
        }
    }

    private boolean filterServer(Server server) {
        if (filters.size() == 0) return false;
        if (server.equals(activeServer)) return false;
        String name = server.getName().toLowerCase();
        for(String filter:filters) {
            if (! name.contains(filter)) return true;
        }
        return false;
    }

    private void refreshServers() {
        root.removeAllChildren();
        for (Server server:servers) {
            if (filterServer(server)) continue;
            ServerNode node = new ServerNode(server);
            if (activeServer != null && activeServer.equals(server)) {
                node.setActive(true);
            }
            root.add(new DefaultMutableTreeNode(node));
        }
        tree.expandPath(new TreePath(root.getPath()));
        ((DefaultTreeModel)tree.getModel()).reload();
        tree.invalidate();
    }

    public Server getSelectedServer() {
        return selectedServer;
    }

    private void filterChanged() {
        refreshFilters(filter.getText());
        refreshServers();
    }

    private void initComponents() {
        root = new DefaultMutableTreeNode("Servers");
        tree = new JTree(root);
        tree.setRootVisible(false);
        tree.expandPath(new TreePath(root.getPath()));
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                DefaultMutableTreeNode node  = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node == null) return; // no selection
                if (! node.isLeaf()) return;
                selectedServer = ((ServerNode) node.getUserObject()).getServer();
                dispose();
            }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
        filter = new JTextField();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterChanged();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterChanged();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterChanged();
            }
        });
        add(filter, BorderLayout.NORTH);
        setPreferredSize(new Dimension(300,400));
    }


    private static class ServerNode {
        private Server server;
        private boolean active = false;

        ServerNode(Server server) {
            this.server = server;
        }
        Server getServer() {return server;}
        void setActive(boolean active) {this.active = active;}

        @Override
        public String toString() {
            if (active) {
                return "<html><b>" + server.getName() + "</b></html>";
            } else {
                return server.getName();
            }
        }
    }
}

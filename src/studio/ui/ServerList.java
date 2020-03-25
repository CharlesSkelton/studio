package studio.ui;

import studio.kdb.Server;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerList extends EscapeDialog {

    private JTree tree;
    private DefaultMutableTreeNode root;
    private Server selectedServer;


    public ServerList(JFrame parent, Server[] servers, Server active) {
        super(parent, "Server List", true);
        initComponents();
        setServers(servers, active);

        tree.expandPath(new TreePath(root.getPath()));
        tree.invalidate();
        Util.centerChildOnParent(this, getParent());
        pack();
        setVisible(true);
    }

    private void setServers(Server[] servers, Server active) {
        root.removeAllChildren();
        for (Server server:servers) {
            ServerNode node = new ServerNode(server);
            if (active != null && active.equals(server)) {
                node.setActive(true);
            }
            root.add(new DefaultMutableTreeNode(node));
        }
        selectedServer = active;
    }

    public Server getSelectedServer() {
        return selectedServer;
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
                if (! node.isLeaf()) return;
                selectedServer = ((ServerNode) node.getUserObject()).getServer();
                dispose();
            }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
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

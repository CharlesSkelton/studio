package studio.ui;

import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.StringTokenizer;

public class ServerList extends EscapeDialog implements TreeExpansionListener  {

    private JPanel contentPane;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTextField filter;
    private JToggleButton tglBtnBoxTree;
    private boolean ignoreExpansionListener = false;
    private java.util.Set<TreePath> expandedPath = new HashSet<>();
    private java.util.Set<TreePath> collapsedPath = new HashSet<>();

    private Server selectedServer;
    private Server activeServer;
    private ServerTreeNode serverTree, root;

    private JPopupMenu popupMenu;
    private UserAction selectAction, removeAction,
                        insertFolderAction, insertServerAction,
                        addServerBeforeAction, addServerAfterAction,
                        addFolderBeforeAction, addFolderAfterAction;

    public final static int DEFAULT_WIDTH = 300;
    public final static int DEFAULT_HEIGHT = 400;

    private final static int menuShortcutKeyMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private final KeyStroke TREE_VIEW_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcutKeyMask);

    public ServerList(JFrame parent) {
        super(parent, "Server List");
        initComponents();
    }

    public void updateServerTree(ServerTreeNode serverTree, Server activeServer) {
        this.serverTree = serverTree;
        this.activeServer = activeServer;
        selectedServer = activeServer;
        refreshServers();
    }

    //Split filter text by spaces
    private java.util.List<String> getFilters() {
        java.util.List<String> filters = new ArrayList<>();
        filters.clear();
        StringTokenizer st = new StringTokenizer(filter.getText()," \t");
        while (st.hasMoreTokens()) {
            String word = st.nextToken().trim();
            if (word.length()>0) filters.add(word.toLowerCase());
        }
        return filters;
    }

    private void setRoot(ServerTreeNode newRoot) {
        if (newRoot == null) {
            root = new ServerTreeNode();
        } else if (isListView()) {
            root = new ServerTreeNode();
            for (Enumeration e = newRoot.depthFirstEnumeration(); e.hasMoreElements(); ) {
                ServerTreeNode node = (ServerTreeNode) e.nextElement();
                if (node.isFolder()) continue;
                root.add(node.getServer());
            }
        } else {
            root = newRoot;
        }
        treeModel.setRoot(root);
        treeModel.reload();
    }

    //Reload server tree
    private void refreshServers() {
        java.util.List<String> filters = getFilters();

        if (filters.size() > 0) {
            setRoot(filter(serverTree, filters));
            expandAll(); // expand all if we apply any filters
        } else {
            ignoreExpansionListener = true;
            setRoot(serverTree);
            //restore expanded state which was the last time (potentially was changed during filtering)
            for (TreePath path: expandedPath) {
                tree.expandPath(path);
            }
            for (TreePath path: collapsedPath) {
                tree.collapsePath(path);
            }
            //Make sure that active server is expanded and visible
            if (activeServer != null) {
                ServerTreeNode folder = activeServer.getFolder();
                if (folder != null) {
                    ServerTreeNode node = folder.findServerNode(activeServer);
                    if (node != null) {
                        TreePath path = new TreePath(node.getPath());
                        tree.expandPath(path.getParentPath());
                        //validate before scrollPathToVisible is needed to layout all nodes
                        tree.validate();
                        tree.scrollPathToVisible(path);
                    }
                }
            }
            ignoreExpansionListener = false;
        }
        tree.invalidate();
    }


    private void expandAll() {
        ServerTreeNode root = (ServerTreeNode)treeModel.getRoot();
        if (root == null) return;
        expandAll(root, new TreePath(root));
    }

    private void expandAll(ServerTreeNode parent, TreePath path) {
        for(ServerTreeNode child:parent.childNodes() ) {
            expandAll(child, path.pathByAddingChild(child));
        }
        tree.expandPath(path);
    }

    private ServerTreeNode filter(ServerTreeNode parent, java.util.List<String> filters) {
        String value = parent.isFolder() ? parent.getFolder() : parent.getServer().getDescription(false);
        value = value.toLowerCase();
        java.util.List<String> left = new ArrayList<>();
        for(String filter:filters) {
            if (! value.contains(filter)) {
                left.add(filter);
            }
        }

        if (left.size() ==0) return parent.copy();

        java.util.List<ServerTreeNode> children = new ArrayList<>();
        for (ServerTreeNode child: parent.childNodes()) {
            ServerTreeNode childFiltered = filter(child, left);
            if (childFiltered != null) {
                children.add(childFiltered);
            }
        }

        if (children.size() == 0) return null;

        ServerTreeNode result = new ServerTreeNode(parent.getFolder());
        for (ServerTreeNode child: children) {
            result.add(child);
        }
        return result;
    }

    public Server getSelectedServer() {
        return selectedServer;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filter.getText().trim().length() > 0) return;

        TreePath path = event.getPath();
        collapsedPath.remove(path);
        expandedPath.add(path);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filter.getText().trim().length() > 0) return;

        TreePath path = event.getPath();
        expandedPath.remove(path);
        collapsedPath.add(path);
    }

    private void selectTreeNode() {
        ServerTreeNode node  = (ServerTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return; // no selection
        if (node.isFolder()) return;
        selectedServer = node.getServer();
        accept();
    }

    private boolean isListView() {
        return tglBtnBoxTree.isSelected();
    }

    private void toggleTreeListView() {
        tglBtnBoxTree.setSelected(!tglBtnBoxTree.isSelected());
        refreshServers();
    }

    private void initComponents() {
        treeModel = new DefaultTreeModel(new ServerTreeNode(), true);
        tree = new JTree(treeModel) {
            @Override
            public String convertValueToText(Object nodeObj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                ServerTreeNode node = (ServerTreeNode) nodeObj;
                String value;
                if (node.isFolder()) {
                    value = node.getFolder();
                } else {
                    value = node.getServer().getDescription( isListView() );
                }
                if (!node.isFolder() && node.getServer().equals(activeServer)) {
                    value = "<html><b>" + value + "</b></html>";
                }
                return value;
            }
        };
        tree.setRootVisible(false);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeExpansionListener(this);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                } else if (e.getClickCount() == 2) {
                    selectTreeNode();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) handlePopup(e);
            }
        });
        filter = new JTextField();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshServers();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshServers();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshServers();
            }
        });
        tglBtnBoxTree = new JToggleButton(Util.TEXT_TREE_ICON);
        tglBtnBoxTree.setToolTipText("<html>Toggle tree/list <small>" + Util.getAcceleratorString(TREE_VIEW_KEYSTROKE) +"</small></html>");
        tglBtnBoxTree.setSelectedIcon(Util.TEXT_ICON);
        tglBtnBoxTree.setFocusable(false);
        tglBtnBoxTree.addActionListener(e->refreshServers());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(tglBtnBoxTree);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Filter: "));
        toolbar.add(filter);
        filter.requestFocus();

        contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(tree), BorderLayout.CENTER);
        setContentPane(contentPane);

        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        initActions();
        initPopupMenu();
    }

    private void initActions() {
        selectAction = UserAction.create("Select", "Select the node",
                KeyEvent.VK_S, e -> selectTreeNode());
        removeAction = UserAction.create("Remove", "Remove the node",
                KeyEvent.VK_DELETE, e -> removeNode());
        insertServerAction = UserAction.create("Insert Server", "Insert server into the folder",
                KeyEvent.VK_N, e -> addNode(false, AddNodeLocation.INSERT));
        insertFolderAction = UserAction.create("Insert Folder", "Insert folder into the folder",
                KeyEvent.VK_I, e -> addNode(true, AddNodeLocation.INSERT));
        addServerBeforeAction = UserAction.create("Add Server Before", "Add Server before selected node",
                KeyEvent.VK_R, e -> addNode(false, AddNodeLocation.BEFORE));
        addServerAfterAction = UserAction.create("Add Server After", "Add Server after selected node",
                KeyEvent.VK_E, e -> addNode(false, AddNodeLocation.AFTER));
        addFolderBeforeAction = UserAction.create("Add Folder Before", "Add Folder before selected node",
                KeyEvent.VK_B, e -> addNode(true, AddNodeLocation.BEFORE));
        addFolderAfterAction = UserAction.create("Add Folder After", "Add Folder after selected node",
                KeyEvent.VK_A, e -> addNode(true, AddNodeLocation.AFTER));

        UserAction toggleAction = UserAction.create("toggle", e-> toggleTreeListView());
        UserAction focusTreeAction = UserAction.create("focus tree", e-> tree.requestFocusInWindow());

        contentPane.getActionMap().put(toggleAction.getText(), toggleAction);
        tree.getActionMap().put(selectAction.getText(), selectAction);
        filter.getActionMap().put(focusTreeAction.getText(), focusTreeAction);

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(TREE_VIEW_KEYSTROKE, toggleAction.getText());
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), focusTreeAction.getText());
    }

    private void handlePopup(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        TreePath path = tree.getPathForLocation(x, y);
        if (path == null) {
            return;
        };

        tree.setSelectionPath(path);

        if (isListView()) {
            insertFolderAction.setEnabled(false);
            addFolderBeforeAction.setEnabled(false);
            addFolderAfterAction.setEnabled(false);
            insertServerAction.setEnabled(false);
            addServerBeforeAction.setEnabled(false);
            addServerAfterAction.setEnabled(false);
            removeAction.setEnabled(false);
        } else {
            boolean isFolder = ((ServerTreeNode) path.getLastPathComponent()).isFolder();
            insertServerAction.setEnabled(isFolder);
            insertFolderAction.setEnabled(isFolder);
        }

        popupMenu.show(tree, x, y);
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.add(selectAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(insertFolderAction);
        popupMenu.add(addFolderBeforeAction);
        popupMenu.add(addFolderAfterAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(insertServerAction);
        popupMenu.add(addServerBeforeAction);
        popupMenu.add(addServerAfterAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(removeAction);
    }

    private void removeNode() {
        ServerTreeNode selNode  = (ServerTreeNode) tree.getLastSelectedPathComponent();
        if (selNode == null) return;
        ServerTreeNode node = serverTree.findPath(selNode.getPath());
        if (node == null) {
            System.err.println("Ups... Something goes wrong");
            return;
        }

        String message = "Are you sure you want to remove " + (node.isRoot() ? "folder" : "server") + ": " + node.fullPath();
        int result = JOptionPane.showConfirmDialog(this, message, "Remove?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;

        TreeNode[] path = ((ServerTreeNode)selNode.getParent()).getPath();
        node.removeFromParent();
        Config.getInstance().setServerTree(serverTree);
        refreshServers();

        TreePath treePath = new TreePath(path);
        tree.scrollPathToVisible(treePath);
        tree.setSelectionPath(treePath);
    }

    private enum AddNodeLocation {INSERT, BEFORE, AFTER};

    private void addNode(boolean folder, AddNodeLocation location) {
        ServerTreeNode selNode  = (ServerTreeNode) tree.getLastSelectedPathComponent();
        if (selNode == null) return;

        ServerTreeNode node = serverTree.findPath(selNode.getPath());
        if (node == null) {
            System.err.println("Ups... Something goes wrong");
            return;
        }
        ServerTreeNode parent = location == AddNodeLocation.INSERT ? node : (ServerTreeNode)node.getParent();

        ServerTreeNode newNode;
        if (folder) {
            String name = JOptionPane.showInputDialog(this, "Enter folder name", "Folder Name", JOptionPane.QUESTION_MESSAGE);
            if (name == null || name.trim().length() == 0) return;
            newNode = new ServerTreeNode(name);
        } else {
            AddServerForm addServerForm = new AddServerForm(this);
            addServerForm.alignAndShow();
            if (addServerForm.getResult() == DialogResult.CANCELLED) return;
            Server server = addServerForm.getServer();
            server.setFolder(parent);
            newNode = new ServerTreeNode(server);
        }

        int index;
        if (location == AddNodeLocation.INSERT) {
            index = node.getChildCount();
        } else {
            index = parent.getIndex(node);
            if (location == AddNodeLocation.AFTER) index++;
        }

        parent.insert(newNode, index);
        try {
            Config.getInstance().setServerTree(serverTree);
        } catch (IllegalArgumentException exception) {
            serverTree = Config.getInstance().getServerTree();
            System.err.println("Error adding new node: " + exception);
            exception.printStackTrace(System.err);
            JOptionPane.showMessageDialog(this, "Error adding new node:\n" + exception.toString(), "Error", JOptionPane.ERROR_MESSAGE);

        }
        refreshServers();

        selNode = root.findPath(newNode.getPath());
        if (selNode != null) {
            TreePath treePath = new TreePath(selNode.getPath());
            tree.scrollPathToVisible(treePath);
            tree.setSelectionPath(treePath);
        }
    }

}

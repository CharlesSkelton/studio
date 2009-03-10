/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.windows;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.*;
import javax.swing.JFrame;
import org.openide.nodes.Node;
import org.openide.util.Utilities;

/**
 * Trivial window manager that just keeps track of "workspaces" and "modes"
 * according to contract but does not really use them, and just opens all
 * top components in their own frames.
 * Useful in case core-windows.jar is not installed, e.g. in standalone usage.
 * @author Jesse Glick
 * @see "#29933"
 */
final class DummyWindowManager extends WindowManager {
    
    private static final long serialVersionUID = 1L;
    
    private final Map workspaces;  // Map<String,Workspace>
    private transient Frame mw;
    private transient PropertyChangeSupport pcs;
    private transient R r;
    
    public DummyWindowManager() {
        System.err.println("OK");//XXX
        workspaces = new TreeMap();
        createWorkspace("default", null); // NOI18N
    }
    
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(l);
    }
    
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(l);
        }
    }
    
    protected TopComponent.Registry componentRegistry() {
        TopComponent.Registry reg = super.componentRegistry();
        if (reg != null) {
            return reg;
        } else {
            return registry();
        }
    }
    
    synchronized R registry() {
        if (r == null) {
            r = new R();
        }
        return r;
    }
    
    protected WindowManager.Component createTopComponentManager(TopComponent c) {
        return new TCM(c);
    }
    
    public synchronized Workspace createWorkspace(String name, String displayName) {
        Workspace w = new W(name);
        workspaces.put(name, w);
        if (pcs != null) {
            pcs.firePropertyChange(PROP_WORKSPACES, null, null);
            pcs.firePropertyChange(PROP_CURRENT_WORKSPACE, null, null);
        }
        return w;
    }
    
    synchronized void delete(Workspace w) {
        workspaces.remove(w.getName());
        if (workspaces.isEmpty()) {
            createWorkspace("default", null); // NOI18N
        }
        if (pcs != null) {
            pcs.firePropertyChange(PROP_WORKSPACES, null, null);
            pcs.firePropertyChange(PROP_CURRENT_WORKSPACE, null, null);
        }
    }
    
    public synchronized Workspace findWorkspace(String name) {
        return (Workspace)workspaces.get(name);
    }
    
    public synchronized Workspace getCurrentWorkspace() {
        return (Workspace)workspaces.values().iterator().next();
    }
    
    public synchronized Workspace[] getWorkspaces() {
        return (Workspace[])workspaces.values().toArray(new Workspace[0]);
    }
    
    public synchronized void setWorkspaces(Workspace[] ws) {
        if (ws.length == 0) throw new IllegalArgumentException();
        workspaces.clear();
        for (int i = 0; i < ws.length; i++) {
            workspaces.put(ws[i].getName(), ws[i]);
        }
        if (pcs != null) {
            pcs.firePropertyChange(PROP_WORKSPACES, null, null);
            pcs.firePropertyChange(PROP_CURRENT_WORKSPACE, null, null);
        }
    }
    
    public synchronized Frame getMainWindow() {
        if (mw == null) {
            mw = new JFrame("dummy"); // NOI18N
        }
        return mw;
    }
    
    public void updateUI() {}
    
    private final class TCM implements WindowManager.Component {
        
        private static final long serialVersionUID = 1L;
        
        private final TopComponent tc;
        private transient JFrame f;
        private Image icon;
        private final Set workspaces = new HashSet(); // Set<Workspace>
        private transient Node[] nodes;
        
        public TCM(TopComponent tc) {
            this.tc = tc;
        }
        
        public void open() {
            open(getCurrentWorkspace());
        }
        
        public synchronized void open(final Workspace workspace) {
            if (f == null) {
                f = new JFrame(tc.getName());
                if (icon != null) {
                    f.setIconImage(icon);
                }
                f.getContentPane().add(tc);
                f.pack();
                f.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent ev) {
                        close(workspace);
                    }
                });
            }
            f.show();
            workspaces.add(workspace);
            registry().open(tc);
        }
        
        public synchronized void close(Workspace workspace) {
            if (f != null) {
                f.setVisible(false);
            }
            workspaces.remove(workspace);
            registry().close(tc);
        }
        
        public synchronized Set whereOpened() {
            return workspaces;
        }
        
        public Node[] getActivatedNodes() {
            return nodes;
        }
        
        public void setActivatedNodes(Node[] nodes) {
            this.nodes = nodes;
            registry().setActivatedNodes(tc, nodes);
        }
        
        public synchronized void nameChanged() {
            if (f != null) {
                f.setTitle(tc.getName());
            }
        }
        
        public void requestFocus() {
            registry().setActive(tc);
        }
        
        public void requestVisible() {
            if (f != null) {
                f.show();
            }
        }
        
        public Image getIcon() {
            return icon;
        }
        
        public void setIcon(Image icon) {
            this.icon = icon;
            if (f != null && icon != null) {
                f.setIconImage(icon);
            }
        }
        
    }
    
    private final class W implements Workspace {
        
        private static final long serialVersionUID = 1L;
        
        private final String name;
        private final Map modes = new HashMap(); // Map<String,Mode>
        private final Map modesByComponent = new WeakHashMap(); // Map<TopComponent,Mode>
        private transient PropertyChangeSupport pcs;
        
        public W(String name) {
            this.name = name;
        }
        
        public void activate() {
        }
        
        public synchronized void addPropertyChangeListener(PropertyChangeListener list) {
            if (pcs == null) {
                pcs = new PropertyChangeSupport(this);
            }
            pcs.addPropertyChangeListener(list);
        }
        
        public synchronized void removePropertyChangeListener(PropertyChangeListener list) {
            if (pcs != null) {
                pcs.removePropertyChangeListener(list);
            }
        }
        
        public void remove() {
            DummyWindowManager.this.delete(this);
        }
        
        public synchronized Mode createMode(String name, String displayName, URL icon) {
            Mode m = new M(name);
            modes.put(name, m);
            if (pcs != null) {
                pcs.firePropertyChange(PROP_MODES, null, null);
            }
            return m;
        }
        
        public synchronized Set getModes() {
            return new HashSet(modes.values());
        }
        
        public synchronized Mode findMode(String name) {
            return (Mode)modes.get(name);
        }
        
        public synchronized Mode findMode(TopComponent c) {
            return (Mode)modesByComponent.get(c);
        }
        
        synchronized void dock(Mode m, TopComponent c) {
            modesByComponent.put(c, m);
        }
        
        public Rectangle getBounds() {
            return Utilities.getUsableScreenBounds();
        }
        
        public String getName() {
            return name;
        }
        
        public String getDisplayName() {
            return getName();
        }
        
        private final class M implements Mode {
            
            private static final long serialVersionUID = 1L;
            
            private final String name;
            private final Set components = new HashSet(); // Set<TopComponent>
            
            public M(String name) {
                this.name = name;
            }
            
            /* Not needed:
            private transient PropertyChangeSupport pcs;
            public synchronized void addPropertyChangeListener(PropertyChangeListener list) {
                if (pcs == null) {
                    pcs = new PropertyChangeSupport(this);
                }
                pcs.addPropertyChangeListener(list);
            }
            public synchronized void removePropertyChangeListener(PropertyChangeListener list) {
                if (pcs != null) {
                    pcs.removePropertyChangeListener(list);
                }
            }
             */
            public void addPropertyChangeListener(PropertyChangeListener l) {}
            public void removePropertyChangeListener(PropertyChangeListener l) {}
            
            public boolean canDock(TopComponent tc) {
                return true;
            }
            
            public synchronized boolean dockInto(TopComponent c) {
                if (components.add(c)) {
                    Mode old = findMode(c);
                    if (old != null && old != this && old instanceof M) {
                        synchronized (old) {
                            ((M)old).components.remove(c);
                        }
                    }
                    dock(this, c);
                }
                return true;
            }
            
            public String getName() {
                return name;
            }
            
            public String getDisplayName() {
                return getName();
            }
            
            public Image getIcon() {
                return null;
            }
            
            public synchronized TopComponent[] getTopComponents() {
                return (TopComponent[])components.toArray(new TopComponent[0]);
            }
            
            public Workspace getWorkspace() {
                return W.this;
            }
            
            public synchronized Rectangle getBounds() {
                return W.this.getBounds();
            }
            
            public void setBounds(Rectangle s) {
            }
            
        }
        
    }
    
    private final class R implements TopComponent.Registry {
        
        private TopComponent active;
        private final Set opened; // Set<TopComponent>
        private Node[] nodes;
    
        public R() {
            opened = new HashSet();
            nodes = new Node[0];
        }
        
        private PropertyChangeSupport pcs;
        
        public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
            if (pcs == null) {
                pcs = new PropertyChangeSupport(this);
            }
            pcs.addPropertyChangeListener(l);
        }
        
        public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
            if (pcs != null) {
                pcs.removePropertyChangeListener(l);
            }
        }
        
        synchronized void open(TopComponent tc) {
            opened.add(tc);
            if (pcs != null) {
                pcs.firePropertyChange(PROP_OPENED, null, null);
            }
        }

        synchronized void close(TopComponent tc) {
            opened.remove(tc);
            if (pcs != null) {
                pcs.firePropertyChange(PROP_OPENED, null, null);
            }
        }
        
        public synchronized Set getOpened() {
            return new HashSet(opened);
        }
        
        synchronized void setActive(TopComponent tc) {
            active = tc;
            Node[] _nodes = tc.getActivatedNodes();
            if (_nodes != null) {
                nodes = _nodes;
                if (pcs != null) {
                    pcs.firePropertyChange(PROP_ACTIVATED_NODES, null, null);
                }
            }
            if (pcs != null) {
                pcs.firePropertyChange(PROP_ACTIVATED, null, null);
                pcs.firePropertyChange(PROP_CURRENT_NODES, null, null);
            }
        }
        
        synchronized void setActivatedNodes(TopComponent tc, Node[] _nodes) {
            if (tc == active) {
                if (_nodes != null) {
                    nodes = _nodes;
                    if (pcs != null) {
                        pcs.firePropertyChange(PROP_ACTIVATED_NODES, null, null);
                    }
                }
                if (pcs != null) {
                    pcs.firePropertyChange(PROP_CURRENT_NODES, null, null);
                }
            }
        }

        public TopComponent getActivated() {
            return active;
        }
        
        public Node[] getActivatedNodes() {
            return nodes;
        }
        
        public synchronized Node[] getCurrentNodes() {
            if (active != null) {
                return active.getActivatedNodes();
            } else {
                return null;
            }
        }
        
    }
    
}

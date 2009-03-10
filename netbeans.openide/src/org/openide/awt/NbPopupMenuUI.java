/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.awt;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.MenuElement;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicPopupMenuUI;

/**
 * Controlls keys for PopupMenu - UP, DOWN, LEFT, RIGHT, ESCAPE, RETURN
 */
final class NbPopupMenuUI extends BasicPopupMenuUI {

    protected void installKeyboardActions() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        popupMenu.registerKeyboardAction(new CancelAction(), ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        popupMenu.registerKeyboardAction(new SelectNextItemAction(), ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        popupMenu.registerKeyboardAction(new SelectPreviousItemAction(), ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        popupMenu.registerKeyboardAction(new SelectChildItemAction(), ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        popupMenu.registerKeyboardAction(new SelectParentItemAction(), ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        Action retAction = new ReturnAction();
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        popupMenu.registerKeyboardAction(retAction, ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
        // #16189. Also for space the same action.
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        popupMenu.registerKeyboardAction(retAction, ks, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    protected void uninstallKeyboardActions() {
        KeyStroke ks = KeyStroke.getKeyStroke((char) KeyEvent.VK_ESCAPE);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        popupMenu.unregisterKeyboardAction(ks);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        popupMenu.unregisterKeyboardAction(ks);
    }
    
    final static class CancelAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		
	    MenuElement path[] = MenuSelectionManager.defaultManager().getSelectedPath();
            if (path[path.length - 1] instanceof JPopupMenu) {
		MenuElement newPath[] = new MenuElement[path.length - 1];
		System.arraycopy(path, 0, newPath, 0, path.length - 1);
		MenuSelectionManager.defaultManager().setSelectedPath(newPath);
            } else if (path.length > 2) {
		MenuElement newPath[] = new MenuElement[path.length - 2];
		System.arraycopy(path, 0, newPath, 0, path.length - 2);
		MenuSelectionManager.defaultManager().setSelectedPath(newPath);
	    } else {
		MenuSelectionManager.defaultManager().clearSelectedPath();
            }
	}
    }

    final static class ReturnAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
	    MenuElement path[] = MenuSelectionManager.defaultManager().getSelectedPath();
	    MenuElement lastElement;
	    if (path.length > 0) {
		lastElement = path[path.length - 1];
                if ((lastElement instanceof JMenuItem) &&
                    (!(lastElement instanceof JMenu) 
                    // XXX #11048: The Actions.SubMenu acts as pure JMenuItem
                    // for cases it has one (or zero) sub-item.
                    || (lastElement instanceof Actions.SubMenu 
                        && ((Actions.SubMenu)lastElement).getMenuComponentCount() <= 1))) {
                            
		    MenuSelectionManager.defaultManager().clearSelectedPath();
		    ((JMenuItem) lastElement).doClick(0);
		    ((JMenuItem) lastElement).setArmed(false);
		}
	    }
	}
    }

    static MenuElement nextEnabledChild(MenuElement e[], int fromIndex) {
	int i, c;
	for(i = fromIndex, c = e.length; i < c; i++) {
	    if (e[i] != null) {
		Component comp = e[i].getComponent();
		if(comp != null && comp.isEnabled()) {
		    return e[i];
                }
	    }
	}
	return null;
    }

    static MenuElement previousEnabledChild(MenuElement e[], int fromIndex) {
	int i;
	for(i = fromIndex; i >= 0; i--) {
	    if (e[i] != null) {
		Component comp = e[i].getComponent();
		if(comp != null && comp.isEnabled()) {
		    return e[i];
                }
	    }
	}
	return null;
    }

    final static class SelectNextItemAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {

	    MenuElement currentSelection[] = MenuSelectionManager.defaultManager().getSelectedPath();
	    if(currentSelection.length > 1) {
                boolean visiblePopup = (currentSelection[currentSelection.length - 1] instanceof JPopupMenu);
                int parentIdx = (visiblePopup ? 3 : 2);
                int childIdx = (visiblePopup ? 2 : 1); 
		MenuElement parent = currentSelection[currentSelection.length - parentIdx];
                MenuElement childs[] = parent.getSubElements();
                MenuElement nextChild;
                int i, c;
                for(i = 0, c = childs.length; i < c; i++) {
                    if(childs[i] == currentSelection[currentSelection.length - childIdx]) {
                        nextChild = nextEnabledChild(childs, i + 1);
                        if(nextChild == null)
                            nextChild = nextEnabledChild(childs, 0);
                        if(nextChild != null) {
                            JMenu childMenu = null;
                            if (nextChild instanceof JMenu 
                            // XXX 11048. See first XXX 11048 comment.
                            && (!(nextChild instanceof Actions.SubMenu)
                                || ((Actions.SubMenu)nextChild).getMenuComponentCount() > 1)) {
                                    
                                childMenu = (JMenu) nextChild;
                            }

                            if (visiblePopup != (childMenu != null)) {
                                if (visiblePopup) {
                                    MenuElement[] newSelection = new MenuElement[currentSelection.length - 1];
                                    System.arraycopy(currentSelection, 0, newSelection, 0, newSelection.length - 1);
                                    newSelection[newSelection.length - 1] = nextChild;
                                    currentSelection = newSelection;
                                } else {
                                    MenuElement[] newSelection = new MenuElement[currentSelection.length + 1];                                    
                                    System.arraycopy(currentSelection, 0, newSelection, 0, currentSelection.length);
                                    newSelection[newSelection.length - 2] = childMenu;
                                    JPopupMenu tmpPopup = childMenu.getPopupMenu();
                                    newSelection[newSelection.length - 1] = tmpPopup;
                                    changeTargetUI(tmpPopup);
                                    currentSelection = newSelection;
                                }
                            } else if (visiblePopup) {
                                currentSelection[currentSelection.length - 2] = nextChild;
                                JPopupMenu tmpPopup = childMenu.getPopupMenu();
                                currentSelection[currentSelection.length - 1] = tmpPopup;
                                changeTargetUI(tmpPopup);
                            } else {
                                currentSelection[currentSelection.length - 1] = nextChild;
                            }
                            
                            MenuSelectionManager.defaultManager().setSelectedPath(currentSelection);
                        }
                        break;
                    }
                }
	    }
	}
    }

    final static class SelectPreviousItemAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {

	    MenuElement currentSelection[] = MenuSelectionManager.defaultManager().getSelectedPath();
	    if (currentSelection.length > 1) {
                boolean visiblePopup = (currentSelection[currentSelection.length - 1] instanceof JPopupMenu);
                int parentIdx = (visiblePopup ? 3 : 2);
                int childIdx = (visiblePopup ? 2 : 1); 
		MenuElement parent = currentSelection[currentSelection.length - parentIdx];
                MenuElement childs[] = parent.getSubElements();
                MenuElement nextChild;
                int i, c;
                for (i = 0, c = childs.length; i < c; i++) {
                    if (childs[i] == currentSelection[currentSelection.length - childIdx]) {
                        nextChild = previousEnabledChild(childs, i - 1);
                        if (nextChild == null) {
                            nextChild = previousEnabledChild(childs, childs.length - 1);
                        }
                        if (nextChild != null) {
                            JMenu childMenu = null;
                            if (nextChild instanceof JMenu
                            // XXX 11048. See first XXX 11048 comment.
                            && (!(nextChild instanceof Actions.SubMenu)
                                || ((Actions.SubMenu)nextChild).getMenuComponentCount() > 1)) {

                                childMenu = (JMenu) nextChild;
                            }
                            
                            if (visiblePopup != (childMenu != null)) {
                                if (visiblePopup) {
                                    MenuElement[] newSelection = new MenuElement[currentSelection.length - 1];
                                    System.arraycopy(currentSelection, 0, newSelection, 0, newSelection.length - 1);
                                    newSelection[newSelection.length - 1] = nextChild;
                                    currentSelection = newSelection;
                                } else {
                                    MenuElement[] newSelection = new MenuElement[currentSelection.length + 1];                                    
                                    System.arraycopy(currentSelection, 0, newSelection, 0, currentSelection.length);
                                    newSelection[newSelection.length - 2] = childMenu;
                                    JPopupMenu tmpPopup = childMenu.getPopupMenu();
                                    newSelection[newSelection.length - 1] = tmpPopup;
                                    changeTargetUI(tmpPopup);
                                    currentSelection = newSelection;
                                }
                            } else if (visiblePopup) {
                                currentSelection[currentSelection.length - 2] = nextChild;
                                JPopupMenu tmpPopup = childMenu.getPopupMenu();
                                currentSelection[currentSelection.length - 1] = tmpPopup;
                                changeTargetUI(tmpPopup);
                            } else {
                                currentSelection[currentSelection.length - 1] = nextChild;
                            }
                            
                            MenuSelectionManager.defaultManager().setSelectedPath(currentSelection);
                        }
                        break;
                    }
                }
	    }
	}
    }

    final static class SelectChildItemAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {

	    MenuElement path[] = MenuSelectionManager.defaultManager().getSelectedPath();
                
	    if (path.length > 0) {
                Component compo = path[path.length - 1].getComponent();
                boolean visiblePopup = false;
                if (compo instanceof JPopupMenu) {
                    if (path.length == 1) {
                        return;
                    }
                    compo = path[path.length - 2].getComponent();
                    visiblePopup = true;
                }
                if (compo.isEnabled() && 
	            (compo instanceof JMenu) && 
                    // XXX 11048. See first XXX 11048 comment.
                    (!(compo instanceof Actions.SubMenu) 
                        || ((Actions.SubMenu)compo).getMenuComponentCount() > 1) &&
	            !((JMenu) compo).isTopLevelMenu()) {
                        JPopupMenu popup = ((JMenu) compo).getPopupMenu();
                        changeTargetUI(popup);
                        MenuElement subElements[] = popup.getSubElements();
                        
                        if (subElements.length > 0) {
                            int adder = (visiblePopup ? 1 : 2);
                            int relativePopupIdx = (visiblePopup ? 1 : 0);
                            MenuElement enabledChild = nextEnabledChild(subElements, 0);
                            if (enabledChild != null) {
                                //boolean setPopup = (enabledChild.getComponent() instanceof JMenu);
                                
                                java.awt.Component c = enabledChild.getComponent();
                                boolean setPopup = (c instanceof JMenu 
                                // XXX 11048. See first XXX 11048 comment.
                                && (!(c instanceof Actions.SubMenu)
                                    || ((Actions.SubMenu)c).getMenuComponentCount() > 1));
                                
                                if (setPopup) {
                                    adder++;
                                }
                                MenuElement newPath[] = new MenuElement[path.length + adder];
                                System.arraycopy(path, 0, newPath, 0, path.length);
                                newPath[path.length - relativePopupIdx] = popup;
                                newPath[path.length + 1 - relativePopupIdx] = enabledChild;
                                if (setPopup) {
                                    JMenu jmenu = (JMenu) enabledChild.getComponent();
                                    newPath[newPath.length - 1] = jmenu.getPopupMenu();
                                }
                                
                                MenuSelectionManager.defaultManager().setSelectedPath(newPath);
                            }
                        }
                }
	    }
	}
    }
    
    final static class SelectParentItemAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {

	    MenuElement path[] = MenuSelectionManager.defaultManager().getSelectedPath();
            if (path.length > 3) {
                if (path[path.length - 1].getComponent() instanceof JPopupMenu) {
                    MenuElement[] newPath = new MenuElement[path.length - 1];
                    System.arraycopy(path, 0, newPath, 0, newPath.length);
                    MenuSelectionManager.defaultManager().setSelectedPath(newPath);
                } else {
                    MenuElement[] newPath = new MenuElement[path.length - 2];
                    System.arraycopy(path, 0, newPath, 0, newPath.length);
                    MenuSelectionManager.defaultManager().setSelectedPath(newPath);
                }
            } else if (path.length > 2) {
                if (path[path.length - 1].getComponent() instanceof JPopupMenu) {
                    MenuElement[] newPath = new MenuElement[path.length - 1];
                    System.arraycopy(path, 0, newPath, 0, newPath.length);
                    MenuSelectionManager.defaultManager().setSelectedPath(newPath);
                }
            }
	}
    }
    
    
    static void changeTargetUI(JPopupMenu menu) {
        if (menu.getUI() instanceof NbPopupMenuUI) {
            return;
        }
        
        menu.setUI(new NbPopupMenuUI());
    }
}
package studio.qeditor;

import org.netbeans.editor.ext.*;

import javax.swing.*;
import java.awt.*;

public class QCompletion extends Completion
{

    public QCompletion(ExtEditorUI extEditorUI)
    {
        super(extEditorUI);
    }

    protected CompletionView createView()
    {
        return new ListCompletionView(new DelegatingCellRenderer());
    }

    protected CompletionQuery createQuery()
    {
        return new QCompletionQuery();
    }

    public synchronized boolean substituteText(boolean flag)
    {
        if (getLastResult() != null)
        {
            int index = getView().getSelectedIndex();
            if (index >= 0)
            {
                getLastResult().substituteText(index, flag);
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    public class DelegatingCellRenderer implements ListCellRenderer
    {
        ListCellRenderer defaultRenderer = new DefaultListCellRenderer();


        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value instanceof CompletionQuery.ResultItem)
            {
                return ((CompletionQuery.ResultItem) value).getPaintComponent(list, isSelected, cellHasFocus);
            }
            else
            {
                return defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    }

}

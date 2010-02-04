/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import studio.kdb.Lm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class HelpDialog extends JDialog
{
    JButton cancelButton = new JButton("Close");

    public HelpDialog(Frame owner)
    {
        super(owner);

        getContentPane().add( createContentPanel(), BorderLayout.CENTER);
     //   getContentPane().add( createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setResizable( false);
    }

    protected JRootPane createRootPane()
        {
           JRootPane pane= super.createRootPane();
            pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE,0), "escPressed");
            pane.getActionMap().put("escPressed",
                    new AbstractAction("escPressed")
                    {
                        public void actionPerformed( ActionEvent ae)
                        {
                            setVisible(false);
                            dispose();
                        }
                    }
                    );

            return pane;
        }




    public void setStartLocation(Component parent)
    {
        Point parentlocation = parent.getLocation();
        Dimension oursize = getPreferredSize();
        Dimension parentsize = parent.getSize();

        int x = parentlocation.x + (parentsize.width - oursize.width) / 2;
        int y = parentlocation.y + (parentsize.height - oursize.height) / 2;

        x = Math.max(0, x);  // keep the corner on the screen
        y = Math.max(0, y);  //

        setLocation(x, y);
    }

   public JComponent createContentPanel()
    {
    //   NumberFormat     numberFormatter= new DecimalFormat( "##.00");
  //     double d= Lm.getMajorVersion()+Lm.getMinorVersion()/100.0;
//       String version= numberFormatter.format(d);

        FormLayout layout = new FormLayout(
        "left:pref, 3dlu, 3dlu");//, // columns
//        "p, 3dlu, p, 9dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 9dlu, p, 3dlu,p, 3dlu, p, 18dlu, p,50dlu,50dlu");      // rows



        //
        // Specify that columns 1 & 5 as well as 3 & 7 have equal widths.
        //layout.setColumnGroups(new int[][]{{1, 5}, {3, 7}});

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        // Obtain a reusable constraints object to place components in the grid.
        CellConstraints cc = new CellConstraints();

        // Fill the grid with components; the builder can create
        // frequently used components, e.g. separators and labels.

        // Add a titled separator to cell (1, 1) that spans 7 columns.

        int row=1;

        layout.appendRow( new RowSpec( "p"));
        builder.addSeparator("Studio for kdb+",  cc.xyw(1,  row, 3));    // p
        row++;
        layout.appendRow( new RowSpec( "3dlu"));
        row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "p"));
        builder.addLabel( "Studio for kdb+ by Charles Skelton",
             cc.xy (1,  row));         // p
        row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "p"));
        builder.addLabel( "is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License",
             cc.xy (1,  row));         // p
        row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "p"));
        builder.addLabel( "http://creativecommons.org/licenses/by-nc-sa/3.0",
             cc.xy (1,  row));         // p
        row++;                                                             // 3dlu

        layout.appendRow( new RowSpec( "3dlu"));
        row++;
        layout.appendRow( new RowSpec( "p"));
        SimpleDateFormat f= new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone( TimeZone.getTimeZone("GMT"));
        builder.addLabel("Version: " + Lm.getVersionString() + "  built on " + f.format(Lm.buildDate),    cc.xy (1,  row));          // p
        row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "3dlu"));
        row++;                                                             // 3dlu

        layout.appendRow( new RowSpec( "p"));
        builder.addLabel( "Project hosted at http://code.kx.com",       cc.xy (1,  row));          // p
        row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "3dlu"));
        row++;                                                             // 18dlu
        layout.appendRow( new RowSpec( "p"));
 //       builder.addSeparator(" ",    cc.xyw(1,  row, 3));     // p
 //       row++;                                                             // 3dlu
        layout.appendRow( new RowSpec( "p"));
        builder.add(createButtonPanel(),        cc.xyw (1,  row,3));     // p
//        row++;                                                             // 3dlu
//        layout.appendRow( new RowSpec( "18dlu"));

        return builder.getPanel();
    }


    public JPanel createButtonPanel()
    {
        AbstractAction cancelAction = new AbstractAction("Close")
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        };

        cancelAction.putValue(AbstractAction.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));

        cancelButton.setAction(cancelAction);

        ButtonBarBuilder builder = new ButtonBarBuilder();
//        builder.addGridded(new JButton("Help"));
        builder.addGlue();
        builder.addUnrelatedGap();
           builder.addGriddedButtons(new JButton[] {
                   cancelButton
               });


        getRootPane().setDefaultButton(cancelButton);
        //setDefaultAction(affirmativeAction);


        //setDefaultCancelAction(cancelAction);

 //       button = new JButton("Help");
 //       button.setEnabled( false);
  //      buttonPanel.add(button);

     //   builder.setDefaultButtonBarGapBorder();
        //builder.setDefaultDialogBorder();

        JPanel p=builder.getPanel();

      //  p.validate();
     //   p.setMinimumSize( p.getPreferredSize());
        return p;
    }
}

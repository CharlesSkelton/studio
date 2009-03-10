/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.swing.*;
import java.awt.*;

public class ExceptionGroup extends ThreadGroup {
    public ExceptionGroup() {
        super("ExceptionGroup");
    }

    public void uncaughtException(Thread t,Throwable e) {
        CharArrayWriter caw = new CharArrayWriter();

        e.printStackTrace(new PrintWriter(caw));

        JOptionPane.showMessageDialog(findActiveFrame(),
                                      "An uncaught exception occurred\n\nDetails - \n\n" + caw.toString(),
                                      "Studio for kdb+",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private Frame findActiveFrame() {
        Frame[] frames = JFrame.getFrames();
        for (int i = 0;i < frames.length;i++) {
            Frame frame = frames[i];
            if (frame.isVisible())
                return frame;
        }
        return null;
    }
}

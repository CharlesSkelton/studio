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

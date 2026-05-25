/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.swing.JFrame;

/**
 * @author andreas kielkopf
 * 
 */
public class Persist implements Serializable {
   private static final long serialVersionUID=6844289973858676550L;
   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         private Persist load_me() {
            try {
               Path persistenceFile=Paths.get("Sammler.persist");
               if (Files.isReadable(persistenceFile)) {
                  try (ObjectInputStream ois=new ObjectInputStream(Files.newInputStream(persistenceFile))) {
                     if (ois.readObject() instanceof Persist sammler)
                        return sammler;
                  }
               }
            } catch (Exception ignore) {
               ignore.printStackTrace();
            }
            return new Persist();
         }
         void save_me(Persist me) {
            try (OutputStream out=Files.newOutputStream(Paths.get("Sammler.persist"),
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                     ObjectOutputStream oos=new ObjectOutputStream(out)) {
               oos.writeObject(me);
               oos.flush();
               out.flush();
               oos.close();
               out.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         @Override
         public void run() {
            try {
               final Persist window=load_me();
               window.frame.addWindowListener(new WindowListener() {
                  @Override
                  public void windowActivated(WindowEvent e) {/* */}
                  @Override
                  public void windowClosed(WindowEvent e) {/* */}
                  @Override
                  public void windowClosing(WindowEvent e) {
                     save_me(window);
                  }
                  @Override
                  public void windowDeactivated(WindowEvent e) {/* */}
                  @Override
                  public void windowDeiconified(WindowEvent e) {/* */}
                  @Override
                  public void windowIconified(WindowEvent e) {/* */}
                  @Override
                  public void windowOpened(WindowEvent e) {/* */}
               });
               window.frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   JFrame frame;
}

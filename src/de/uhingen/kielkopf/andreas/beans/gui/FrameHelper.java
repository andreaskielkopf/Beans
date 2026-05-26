/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Positionioere den Frame immer da wo er beim letzten Mal war
 * 
 * @author Andreas Kielkopf
 *
 */
public class FrameHelper {
   private static Preferences node;
   private static String      fullName;
   private static String      simpleName;
   private static Path        jarPath;
   private static String      FRAME_X="frame_X";
   private static String      FRAME_Y="frame_Y";
   private static String      FRAME_W="frame_W";
   private static String      FRAME_H="frame_H";
   // static public void getNode(Class<? extends Object> o) {
   // // if (node == null && !frame.getClass().getSimpleName().equals("JFrame"))
   // // node=Preferences.userNodeForPackage(frame.getClass());
   // if (node == null)
   // node=Preferences.userNodeForPackage(o);
   // }
   /**
    * @return node Preferences die mit genutzt werden dürfen. (Bei Programmende werden sie automatisch gespeichert)
    */
   static public Preferences getNode() {
      if (node == null)
         node=Preferences.userRoot().node(getFullName().replace('.', '/'));
      return node;
   }
   /**
    * @return Name der Main-Klasse
    */
   static public String getFullName() {
      if (fullName == null) {
         final String first=FrameHelper.class.getName().split("\\.")[0] + ".";
         for (StackTraceElement e:List.of(Thread.currentThread().getStackTrace()))
            if (e.getClassName() instanceof String s && s.startsWith(first))
               fullName=s;
      }
      return fullName;
   }
   /**
    * @return SimpleName der Main-Klasse
    */
   static public String getSimpleName() {
      if (simpleName == null)
         simpleName=List.of(fullName.split("\\.")).getLast();
      return simpleName;
   }
   /**
    * Sichert den Platz den der Frame zueletzt benutzt hat, und stellt ihn beim neustart dort wieder her
    * @param frame
    *           der in seinem angestammten Platz erscheinen soll
    */
   static public void restore(JFrame frame) {
      getNode();
      if (node == null && !frame.getClass().getSimpleName().equals("JFrame"))
         node=Preferences.userNodeForPackage(frame.getClass());
      // Titel erzeugen
      StringBuilder sb=new StringBuilder(List.of(fullName.split("\\.")).getLast()).append(" ");
      sb.append((getDateTime() instanceof String dt && !dt.isBlank()) ? dt : "durch Eclipse gestartet");
      frame.setTitle(sb.toString());
      GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
      // Erstmal Fullscreen
      //frame.setBounds(ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds());
      // Position versuchen wiederherzustellen
      int x=node.getInt(FRAME_X, frame.getX());
      int y=node.getInt(FRAME_Y, frame.getY());
      int w=node.getInt(FRAME_W, frame.getWidth());
      int h=node.getInt(FRAME_H, frame.getHeight());
      for (GraphicsDevice gd:ge.getScreenDevices()) {
         Rectangle r1=new Rectangle(x, y, w, h);
         if (gd.getDefaultConfiguration().getBounds().contains(r1))
            frame.setBounds(r1);
         else {
            Rectangle r0=new Rectangle(x + 2, y, w - 4, h - 2);
            if (gd.getDefaultConfiguration().getBounds().contains(r0))
               frame.setBounds(r0);
         }
      }
//      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            try {
               node.putInt(FRAME_X, frame.getX());
               node.putInt(FRAME_Y, frame.getY());
               node.putInt(FRAME_W, frame.getWidth());
               node.putInt(FRAME_H, frame.getHeight());
               node.sync();
            } catch (BackingStoreException _) { /* ignore */ } // super.windowClosing(e);
            frame.dispose();
         }
      });
   }
   /**
    * @return DateTime zu der das Programm in die JAR-Datei verpackt wurde
    */
   public static String getDateTime() {
      return (getJarEntry() instanceof JarEntry j) ? getDateTime(j.getTimeLocal(), 16) : "";
   }
   private static String getDateTime(LocalDateTime ldt, int n) {
      return ldt.toString().replace('T', ' ').substring(0, n);
   }
   private static JarEntry getJarEntry() {
      if (getJarPath() instanceof Path jp && jp.toString().endsWith(".jar"))
         try (JarFile f=new JarFile(jp.toFile())) {
            return f.getJarEntry("META-INF/MANIFEST.MF");
         } catch (NullPointerException | IOException e) {
            System.err.println(e);
         }
      return null;
   }
   private static Path getJarPath() {
      if (jarPath == null) {
         Class<@NonNull FrameHelper> fhc=FrameHelper.class;
         try {
            jarPath=Paths.get(fhc.getProtectionDomain().getCodeSource().getLocation().toURI());
         } catch (NullPointerException | URISyntaxException e) {
            System.err.println(e);
            if (fhc.getResource('/' + fhc.getName().replace('.', '/') + ".class") instanceof URL url) {
               String s=url.toString();
               // System.out.println(url);
               // Beispiel: "jar:file:/path/to/app.jar!/com/example/Main.class" oder "file:/.../classes/com/..."
               if (s.startsWith("jar:"))
                  jarPath=Paths.get(URI.create(s.substring(4).split("!")[0]));
               // else
               // if (s.startsWith("file:")) {
               // läuft aus Klassenverzeichnis (IDE); evtl. Pfad zum Klassenordner verwenden
               // }
            }
         }
      }
      return jarPath;
   }
}

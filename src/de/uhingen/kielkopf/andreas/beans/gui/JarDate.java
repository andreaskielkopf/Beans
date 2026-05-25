/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Andreas Kielkopf
 *
 */
public class JarDate {
   // public static String getDatetime(Class<?> c) {
   // return (gerJarEntry(c) instanceof JarEntry j) ? j.getTimeLocal().toString().replace('T', ' ').substring(0, 16)
   // : "";
   // }
   public static String getDatetime() {
      return (gerJarEntry(JarDate.class) instanceof JarEntry j) ? j.getTimeLocal().toString().replace('T', ' ') : "";
   }
   /**
    * @param c
    * @return
    */
   public static JarEntry gerJarEntry(Class<?> c) {
      Path jarPath=null;
      try {
         jarPath=Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI());
      } catch (NullPointerException | URISyntaxException e) {
         System.err.println(e);
         if (c.getResource('/' + c.getName().replace('.', '/') + ".class") instanceof URL url) {
            String s=url.toString();
            System.out.println(url);
            // Beispiel: "jar:file:/path/to/app.jar!/com/example/Main.class" oder "file:/.../classes/com/..."
            if (s.startsWith("jar:"))
               jarPath=Paths.get(URI.create(s.substring(4).split("!")[0]));
            // else
            // if (s.startsWith("file:")) {
            // läuft aus Klassenverzeichnis (IDE); evtl. Pfad zum Klassenordner verwenden
            // }
         }
      }
      if (jarPath instanceof Path jp)
         try {
            if (jp.toString().endsWith(".jar"))
               try (JarFile f=new JarFile(jp.toFile())) {
                  return f.getJarEntry("META-INF/MANIFEST.MF");
               }
         } catch (NullPointerException | IOException e) {
            System.err.println(e);
         }
      return null;
   }
}

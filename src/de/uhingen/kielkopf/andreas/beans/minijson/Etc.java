/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.minijson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public class Etc {
   static final String                                    CONF=".conf";
   /** Eine Map mit dem Pfad und dem Inhalt alle Configfiles */
   public final ConcurrentSkipListMap<Path, List<String>> confFiles;
   /**
    * Erzeugt ein Etc-Objekt mit den Inhalten aller configdateien
    * 
    * @param conf0
    *           map mit allen configdateien
    */
   public Etc(@NonNull ConcurrentSkipListMap<Path, List<String>> conf0) {
      confFiles=conf0;
   }
   /**
    * Ergibt den Pfad auf das Dir mit den Configfiles
    * 
    * @param directory
    *           name des configdir
    * 
    * @return path oder null
    * @throws IOException
    *            gib exception weiter
    */
   public static Path hasConfigDir(String directory) throws IOException {
      if (directory instanceof String d && d.matches("[a-zA-Z0-9]{3,80}")) {
         Path p=Paths.get("/etc", directory + ".d");
         if (Files.isDirectory(p))
            return p;
      }
      return null;
   }
   /**
    * Erzeugt das Configdir wenn es fehlt
    * 
    * @param directory
    *           wo die configs hingehören
    * @return path oder null
    * @throws IOException
    *            gib exceptions weiter
    */
   public static Path createConfigDir(String directory) throws IOException {
      if (directory instanceof String d && d.matches("[a-zA-Z0-9]{3,80}")) {
         Path p=Paths.get("/etc", directory + ".d");
         if (Files.notExists(p))
            Files.createDirectory(p);
      }
      return hasConfigDir(directory);
   }
   // public static boolean hasConfig(String directory) {
   // if
   // if(Files.isDirectory(p))return p;}return Files.list(p).anyMatch(f->f.getFileName().toString().endsWith(".conf"));}
   /**
    * Holt alle configurationen und erzeugt ein Objekt Etc
    * 
    * @param directory
    *           name des dir mit den configdateien
    * 
    * @return Etc-Objekt mit allen Dateien
    * @throws IOException
    *            gib exceptions weiter
    */
   public static Etc getConfig(@NonNull String directory) throws IOException {
      ConcurrentSkipListMap<Path, List<String>> map=new ConcurrentSkipListMap<>();
      if (hasConfigDir(directory) instanceof Path p)
         try (Stream<Path> s=Files.list(p)) {
            for (Path path:s.filter(f -> f.getFileName().toString().endsWith(".conf")).toList())
               // Map mit dem Pfad der Datei und dessen Inhalt
               map.put(path, Files.readAllLines(path));
            return new Etc(map);
         }
      return null;
   }
   /**
    * Speichert die im Etc-objekt gehaltenen configdateien
    * 
    * @throws IOException
    *            gibt die exception weitzer
    * 
    */
   public void save() throws IOException {
      // Path p=Paths.get("/etc", directory + ".d");
      for (Entry<Path, List<String>> entry:confFiles.entrySet())
         if (entry.getKey() instanceof Path path)
            if (entry.getValue() instanceof List<String> ls)
               Files.write(path, ls, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
   }
}

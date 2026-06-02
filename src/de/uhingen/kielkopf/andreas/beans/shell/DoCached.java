/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andreas Kielkopf
 *
 */
public class DoCached implements Runnable {
   final private ArrayList<String>      list;
   private long                         timeout;
   private Process                      process;
   final private ProcessQueuesC<String> first=new ProcessQueuesC<>();
   private ProcessQueueC<String>        c1   =new ProcessQueueC<>();
   private CopyOnWriteArrayList<String> c2;                          // erstmal null
   /**
    * @param cmds
    * @param l
    */
   private DoCached(ArrayList<String> cmds_, long ms, boolean async_) {
      if (cmds_.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      timeout=System.currentTimeMillis() + ms;
      first.asynchron=async_;
      list=cmds_;
      if (cleaner == null)
         cleaner=aktiviereCleaner();
   }
   @SuppressWarnings("resource")
   @Override
   public void run() {
      if (builder instanceof ProcessBuilder) {
         synchronized (builder) {
            try {
               if (list.size() == 1 && !list.getFirst().matches("[_.\\p{Alnum}]+")) { // kein einfacher Befehl mit Parametern // nur eine einzige
                  // Befehlszeile
                  if (!list.getFirst().matches(".+[ ;|&].+")) // Das ist unklar
                     throw new UnsupportedOperationException(
                              "Kann den Befehl (" + list.getFirst() + ") nicht verstehen");
                  list.addFirst("-c");
               }
               if (list.getFirst().equals("-c")) // shell angefordert
                  list.addFirst(SHELL);
               process=builder.command(list).start(); // System.out.println("l:" + list);
            } catch (IOException | UnsupportedOperationException e1) {
               final var err=e1.getMessage();
               c1.add(err);
               first.err.add(err);
               first.setFertig();
               timeout=System.currentTimeMillis();
               System.err.println(err); // e1.printStackTrace();
            }
         }
         if (process instanceof final Process p) {
            Thread.currentThread().setName(getClass().getSimpleName());
            /// Der ganze Empfang geschieht jetzt im Hintergrund und wandert in die queues
            Thread.startVirtualThread(() -> {
               try (var inp=p.inputReader()) {
                  while (inp.readLine() instanceof final String line) {
                     first.out.add(line); // direkt übergeben
                     c1.add(line); // Cache füllen
                  }
               } catch (final IOException e) {
                  // System.err.println(e.getMessage() + " 1");
                  if (!e.getMessage().equals("Stream closed"))
                     e.printStackTrace();
               } finally {
                  first.setFertig();
                  c2=new CopyOnWriteArrayList<>(c1);
                  c1=null; // Das ist das Zeichen dafür dass alles kopiert ist ;-)
               }
            });
            Thread.startVirtualThread(() -> {
               try (var err=p.errorReader()) {
                  while (err.readLine() instanceof final String line)
                     first.err.add(line); // direkt übergeben
               } catch (final IOException e) {
                  // System.err.println(e.getMessage() + " 2");
                  if (!e.getMessage().equals("Stream closed"))
                     e.printStackTrace();
               } finally {
                  first.setFertig();
                  if (!first.err.isEmpty()) // remove from cache
                     timeout=System.currentTimeMillis();
               }
            });
            try {
               p.waitFor();
            } catch (final InterruptedException e) {
               e.printStackTrace();
            }
         }
      }
   }
   static final public String                                   ASYNC           ="ASYNCHRON";
   static final public String                                   SYNC            ="SYNCRON";
   static final public String                                   DONT_CACHE      ="DONT_CACHE";
   static final public String                                   REFRESH         ="REFRESH";
   static final public String                                   CACHE_MS        ="CACHE_MS=";
   static final private ProcessBuilder                          builder         =new ProcessBuilder();
   static final private Map<String, String>                     env             =builder.environment();
   /// Welche Shell wird verwendet
   static final public String                                   SHELL           =env.get("SHELL") instanceof final String sh
            ? sh
            : "/bin/sh";
   /// Welcher Benutzer agiert
   static final public String                                   USERNAME        =env.get("USER") instanceof final String us
            ? us
            : "noBody";
   static final private ConcurrentSkipListMap<String, DoCached> cache           =new ConcurrentSkipListMap<>();
   static private Thread                                        cleaner;
   static final public AtomicBoolean                            defaultAsynchron=new AtomicBoolean(true);
   static final public AtomicLong                               defaultCacheMs  =new AtomicLong(5 * 60 * 1000);       // 5 Minuten
   // public static boolean LOG_ALL_COMMANDS=false;
   /** Beim Programmstart klären wie die Umgebung aussieht */
   static {
      // System.out.print("static: ");
      env.putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      // System.out.println("User " + USERNAME + " with " + SHELL);
   }
   /**
    * Ermittle den Benutzernamen ?
    *
    * @return isRoot
    */
   final static public boolean isRoot() {
      return USERNAME.equals("root"); // System.out.println(SHELL);
   }
   static private Thread aktiviereCleaner() {
      return Thread.startVirtualThread(() -> {
         do {
            final var now=System.currentTimeMillis();
            for (final Entry<String, DoCached> e:cache.entrySet())
               if (e.getValue().timeout <= now)
                  // System.out.println("cache(" + cache.size() + ") remove-> " + e.getKey());
                  cache.remove(e.getKey());
            try {
               Thread.sleep(defaultCacheMs.get() / 60); // ca. alle 5 Sekunden
            } catch (final InterruptedException _) { /* */ }
         } while (!cache.isEmpty());
         // System.out.println("cache empty");
         cleaner=null; // später erneut starten
      });
   }
   /**
    * Liefert die erste Zeile die diese Commands ausspuckten, oder den Ersatz dafür
    *
    * @param cmd_
    *           Commands
    * @param or
    *           Ersatz
    * @return
    */
   public static String getFirstOr(List<String> cmds_, String or) {
      final var queue=getQueue(cmds_); // while (!queue.isFertig()) // Thread.onSpinWait();
      return queue.getFirst() instanceof final String erg ? erg : or;
   }
   public static String getFirstOr(String... cmd) {
      final ArrayList<String> l=new ArrayList<>(List.of(cmd));
      final var or=l.removeLast();
      return getFirstOr(l, or);
   }
   /// Führt den Befehl aus
   ///
   /// Fehler landen automatisch auf der Konsole
   ///
   /// @return Queue mit den Ergebnissen (asynchron möglich)
   @SuppressWarnings("resource")
   static public ProcessQueueC<String> getQueue(List<String> cmds_) {
      return getQueues(cmds_).toErr().out;
   }
   /// Führt den Befehl aus
   ///
   /// @return Die Queues für Ergebnisse und Fehler (asynchron möglich)
   static public ProcessQueuesC<String> getQueues(List<String> cmds_) {
      final ArrayList<String> cmds=new ArrayList<>(cmds_); // temporäre liste ohne rückwirkung
      // FLAGS vorher verarbeiten und entfernen
      var ms=defaultCacheMs.get();
      final var dont_cache=cmds.remove(DONT_CACHE);
      final var as=cmds.remove(ASYNC) ? true : cmds.remove(SYNC) ? false : defaultAsynchron.get();
      final var refresh=cmds.remove(REFRESH);
      for (final String s:cmds_)
         if (s.startsWith(CACHE_MS))
            try {
               if (cmds.remove(s))
                  ms=Long.parseLong(s.substring(CACHE_MS.length()).replaceAll("_", ""));
            } catch (final NumberFormatException e) {
               System.err.println(e.getMessage());
            }
      final var key2=String.join(" ", cmds);
      if (!refresh && cache.containsKey(key2))
         return cache.get(key2).get2Queues();
      // System.out.println(key2);
      final DoCached dc=new DoCached(cmds, ms, as);
      if (ms > 0 && !dont_cache)
         cache.put(key2, dc);
      Thread.ofVirtual().start(dc);
      return dc.first;
   }
   @SuppressWarnings("resource")
   private ProcessQueuesC<String> get2Queues() {
      // System.err.println("Aus dem cache: " + String.join(" ", list));
      final ProcessQueuesC<String> next=new ProcessQueuesC<>();
      // warte bis der erste Thread wirklich fertig ist
      while (c1 != null)
         Thread.onSpinWait();
      next.out.addAll(c2);
      next.setFertig();
      return next;
   }
   /// @author Andreas Kielkopf
   ///
   ///         Queue die so angepasst ist, dass sie bei poll() wartet, bis Daten vom Prozess kommen
   static public class ProcessQueueC<E> extends LinkedBlockingQueue<E> {
      private static final long serialVersionUID=-2442288599254555538L;
      private final AtomicBoolean     alive           =new AtomicBoolean(true);
      private boolean isFertig() {
         return isEmpty() && !alive.get();
      }
      /// @return First line of result
      public E getFirst() {
         while (isEmpty() && alive.get())
            Thread.onSpinWait();
         return peek();
      }
      /// @return für Methodchaining
      private ProcessQueueC<E> setFertig() {
         alive.set(false);
         return this;
      }
      /// Angepasst, so dass poll() nie schiefgeht solange der Prozess noch läuft.
      ///
      /// * Wenn der Prozess läuft wird gewartet bis er Zeilen produziert.
      ///
      /// * Spätestens nach 100ms wird null zurückgeliefert, wenn der Prozess beendet ist.
      @SuppressWarnings("null")
      @Override
      public E poll() {
         while (!isFertig())
            try {
               if (poll(100, TimeUnit.MILLISECONDS) instanceof final E i)
                  return i;
            } catch (final InterruptedException _) { /* */ }
         return null;
      }
      /// warte bis der Befehl abgearbeitet ist
      ///
      /// @return für Methodchaining
      public ProcessQueueC<E> waitFor() {
         while (!isFertig())
            Thread.onSpinWait();
         return this;
      }
   }

   /**
    * Der komplette Output eines Prozesses zum Verarbeiten oder Wegschmeißen
    *
    * @author Andreas Kielkopf
    *
    * @param <E>
    *           typischerweise komplette Zeilen
    */
   static public class ProcessQueuesC<E> implements AutoCloseable {
      public final ProcessQueueC<E> out;
      public final ProcessQueueC<E> err;
      private boolean               asynchron;
      private ProcessQueuesC(ProcessQueueC<E> output, ProcessQueueC<E> error) {
         out=output;
         err=error;
         asynchron=defaultAsynchron.get();
      }
      private ProcessQueuesC() {
         this(new ProcessQueueC<>(), new ProcessQueueC<>());
      }
      /// @return für Methodchaining
      private ProcessQueuesC<E> setFertig() {
         out.setFertig();
         err.setFertig();
         return this;
      }
      @Override
      public void close() throws Exception {
         Thread.sleep(199);
         setFertig();
      }
      /// Gibt das Ergebnis auf der Konsole aus
      ///
      /// @return für Methodchaining
      public ProcessQueuesC<E> toOut() {
         Thread.startVirtualThread(() -> {
            while (out.poll() instanceof final String line)
               System.out.println(line);
         });
         return this;
      }
      /// Gibt die Fehler auf der Konsole aus
      ///
      /// @return für Methodchaining
      public ProcessQueuesC<E> toErr() {
         Thread.startVirtualThread(() -> {
            while (err.poll() instanceof final String line)
               System.err.println(line);
         });
         return this;
      }
      /// warte bis der Befehl abgearbeitet ist
      ///
      /// @return für Methodchaining
      public ProcessQueuesC<E> waitFor() {
         out.waitFor();
         return this;
      }
   }
   /// Führe den Befehl aus, Optionen können als String übergeben werden
   public static void doCmd(String... string) {
      doCmd(List.of(string));
   }
   /// Führe den Befehl aus, Optionen können als List<String> übergeben werden
   @SuppressWarnings("resource")
   public static void doCmd(List<String> list) { // System.out.println(list);
      final var qs=getQueues(list).toOut().toErr();// .waitFor();
      if (!qs.asynchron)
         qs.waitFor();
   }
}

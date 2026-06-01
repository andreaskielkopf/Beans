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
   private ProcessQueues2<String>       first=new ProcessQueues2<>();
   private ProcessQueue2<String>        c1   =new ProcessQueue2<>();
   Process                              process;
   private CopyOnWriteArrayList<String> c2;                          // erstmal null
   private ArrayList<String>            list;
   private long                         timeout;
   public DoCached(List<String> cmds_) {
      this(cmds_, defaultCacheMs.get());
   }
   /**
    * @param cmds
    * @param l
    */
   public DoCached(List<String> cmds_, long ms) {
      if (cmds_.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      timeout=System.currentTimeMillis() + ms;
      if (cleaner == null)
         cleaner=aktiviereCleaner();
      list=cmds_ instanceof ArrayList<String> l ? l : new ArrayList<>(cmds_); // temporäre liste ohne rückwirkung
   }
   @Override
   public void run() {
      if (builder instanceof ProcessBuilder) {
         synchronized (builder) {
            try {
               if ((list.size() == 1) && !list.getFirst().matches("[_.\\p{Alnum}]+")) { // kein einfacher Befehl mit Parametern // nur eine einzige
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
               String err=e1.getMessage();
               first.err.add(err);
               c1.add(err);
               first.setFertig();
               timeout=System.currentTimeMillis();
               System.err.println(err); // e1.printStackTrace();
            }
         }
         if (process instanceof Process p) {
            Thread.currentThread().setName(getClass().getSimpleName());
            /// Der ganze Empfang geschieht jetzt im Hintergrund und wandert in die queues
            Thread.startVirtualThread(() -> {
               try (var inp=p.inputReader()) {
                  while (inp.readLine() instanceof final String line) {
                     first.out.add(line); // direkt übergeben
                     c1.add(line); // Cache füllen
                  }
               } catch (final IOException e) {
                  System.err.println(e.getMessage() + " 1");
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
                  System.err.println(e.getMessage() + " 2");
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
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      }
   }
   static final private ProcessBuilder                          builder       =new ProcessBuilder();
   static final private Map<String, String>                     env           =builder.environment();
   /// Welche Shell wird verwendet
   static final public String                                   SHELL         =(env.get("SHELL") instanceof String sh)
            ? sh
            : "/bin/sh";
   /// Welcher Benutzer agiert
   static final public String                                   USERNAME      =(env.get("USER") instanceof String us)
            ? us
            : "noBody";
   static final private ConcurrentSkipListMap<String, DoCached> cache         =new ConcurrentSkipListMap<>();
   static private Thread                                        cleaner;
   static final public AtomicBoolean                            asynchron     =new AtomicBoolean(true);
   static final public AtomicLong                               defaultCacheMs=new AtomicLong(5 * 60 * 1000);         // 5 Minuten
   // public static boolean LOG_ALL_COMMANDS=false;
   /** Beim Programmstart klären wie die Umgebung aussieht */
   static {
      System.out.print("static: ");
      env.putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      System.out.println("User " + USERNAME + " with " + SHELL);
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
            long now=System.currentTimeMillis();
            for (Entry<String, DoCached> e:cache.entrySet())
               if (e.getValue().timeout < now) {
                  System.out.println("cache(" + cache.size() + ") remove-> " + e.getKey());
                  cache.remove(e.getKey());
               }
            try {
               Thread.sleep(defaultCacheMs.get() / 60); // ca. alle 5 Sekunden
            } catch (InterruptedException _) { /* */ }
         } while (!cache.isEmpty());
         System.out.println("cache empty");
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
      return (queue.getFirst() instanceof String erg) ? erg : or;
   }
   public static String getFirstOr(String... cmd) {
      ArrayList<String> l=new ArrayList<>(List.of(cmd));
      String or=l.removeLast();
      return getFirstOr(l, or);
   }
   /**
    * @param cmds_
    * @return
    */
   @SuppressWarnings("resource")
   private static ProcessQueue2<String> getQueue(List<String> cmds_) {
      return getQueues(cmds_).out;
   }
   static public ProcessQueues2<String> getQueues(List<String> cmds) {
      return getQueues(cmds, defaultCacheMs.get());
   }
   static public ProcessQueues2<String> getQueues(List<String> cmds, long ms) {
      final var key2=String.join(" ", cmds);
      if (cache.containsKey(key2)) // if( im cache) return aus dem cache
         return cache.get(key2).get2Queues();
      System.out.println(key2);
      final DoCached dc=new DoCached(cmds, ms);
      cache.put(key2, dc);
      Thread.ofVirtual().start(dc);
      return dc.first;
   }
   @SuppressWarnings("resource")
   private ProcessQueues2<String> get2Queues() {
      System.err.println("Aus dem cache: " + String.join(" ", list));
      final ProcessQueues2<String> next=new ProcessQueues2<>();
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
   static public class ProcessQueue2<E> extends LinkedBlockingQueue<E> {
      private static final long serialVersionUID=-2442288599254555538L;
      AtomicBoolean             alive           =new AtomicBoolean(true);
      private boolean isFertig() {
         return isEmpty() && !alive.get();
      }
      /**
       * @return
       */
      public E getFirst() {
         while (alive.get() && isEmpty())
            Thread.onSpinWait();
         return peek();
      }
      void setFertig() {
         alive.set(false);
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
   }

   /**
    * Der komplette Output eines Prozesses zum Verarbeiten oder Wegschmeißen
    *
    * @author Andreas Kielkopf
    *
    * @param <E>
    *           typischerweise komplette Zeilen
    */
   static public class ProcessQueues2<E> implements AutoCloseable {
      public final ProcessQueue2<E> out;
      public final ProcessQueue2<E> err;
      public ProcessQueues2(ProcessQueue2<E> output, ProcessQueue2<E> error) {
         out=output;
         err=error;
      }
      /**
       *
       */
      public ProcessQueues2() {
         this(new ProcessQueue2<>(), new ProcessQueue2<>());
      }
      ProcessQueues2<E> setFertig() {
         out.setFertig();
         err.setFertig();
         return this;
      }
      // public void printErr() {
      // Thread.startVirtualThread(() -> {
      // while (err.poll() instanceof final E e)
      // System.err.println(e);
      // });
      // }
      @Override
      public void close() throws Exception {
         Thread.sleep(199);
         setFertig();
      }
      public ProcessQueues2<E> toOut() {
         Thread.startVirtualThread(() -> {
            while (out.poll() instanceof String line)
               System.out.println(line);
         });
         return this;
      };
      public ProcessQueues2<E> toErr() {
         Thread.startVirtualThread(() -> {
            while (err.poll() instanceof String line)
               System.err.println(line);
         });
         return this;
      }
      /**
       * 
       */
      public void waitFor() {
         while (!out.isFertig())
            Thread.onSpinWait();
      };
   }
   /**
    * @param string
    */
   public static void doCmd(String... string) {
      doCmd(List.of(string));
   }
   /**
    * @param of
    */
   @SuppressWarnings("resource")
   private static void doCmd(List<String> list) { // System.out.println(list);
      ProcessQueues2<String> qs=getQueues(list).toOut().toErr();// .waitFor();
      if (!asynchron.get())
         qs.waitFor();
   }
}

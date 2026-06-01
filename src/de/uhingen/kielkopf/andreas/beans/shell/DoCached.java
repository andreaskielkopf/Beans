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

import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.beans.shell.DoCached.ProcessQueues2;

/**
 * @author Andreas Kielkopf
 *
 */
public class DoCached implements Runnable {
   static final private ProcessBuilder                          builder =new ProcessBuilder();
   static final private Map<String, String>                     env     =builder.environment();
   /** Welche Shell wird verwendet */
   final public static String                                   SHELL   =(env.get("SHELL") instanceof String sh) ? sh
            : "/bin/sh";
   /** Welcher Benutzer agiert */
   final public static String                                   USERNAME=(env.get("USER") instanceof String us) ? us
            : "noBody";
   final private static ConcurrentSkipListMap<String, DoCached> cache   =new ConcurrentSkipListMap<>();
   Process                                                      process;
   static private Thread                                        cleaner;
   private ProcessQueues2<String>                               first   =new ProcessQueues2<>();
   private ProcessQueue2<String>                                c1      =new ProcessQueue2<>();
   // public static boolean USE_SHELL=true;
   // public static boolean LOG_ALL_COMMANDS=false;
   /** Beim Programmstart klären wie die Umgebung aussieht */
   static {
      System.out.print("static: ");
      env.putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      System.out.println("User " + USERNAME + " with " + SHELL);
   }
   /**
    * Ermittle welche Shell benutzt wird
    *
    * @return
    */
   // final static String getShell() {
   // if (SHELL.isBlank()) {
   // // try {
   // // Thread.sleep(500);
   // // } catch (InterruptedException e) {}
   // SHELL="/bin/sh"; // sonst schlägt der erste Test-Befehl fehl
   // SHELL=getFirstOr(List.of(SHELL, "-c", "echo ${SHELL}"), SHELL);
   // System.out.print("1 $SHELL=" + SHELL);
   // SHELL=getFirstOr(List.of(SHELL, "-c", "echo ${SHELL}"), SHELL);
   // System.out.print("2 $SHELL=" + SHELL);
   // }
   // return SHELL;
   // }
   /**
    * Ermittle den Benutzernamen ?
    *
    * @return isRoot
    */
   final static public boolean isRoot() {
//      if (USERNAME.isBlank()) {
//         USERNAME=getFirstOr(List.of("whoami"), "noOne");
//         System.out.print(", user=" + USERNAME);
//      }
      return USERNAME.equals("root"); // System.out.println(SHELL);
   }
   private CopyOnWriteArrayList<String> c2;     // erstmal null
   // private String key;
   private ArrayList<String>            list;
   private long                         timeout;
   public DoCached(List<String> cmds_) {
      this(cmds_, 60 * 60L); // 1 Stunde = 60 Sec* 60 Min
   }
   /**
    * @param cmds
    * @param l
    */
   public DoCached(List<String> cmds_, long sec) {
      timeout=System.currentTimeMillis() + sec * 1000L;
      if (cleaner == null)
         cleaner=Thread.startVirtualThread(() -> {
            long now;
            do
               try {
                  now=System.currentTimeMillis();
                  for (Entry<String, DoCached> e:cache.entrySet())
                     if (e.getValue().timeout < now)
                        cache.remove(e.getKey());
                  Thread.sleep(60000); // alle 5 Sekunden
               } catch (InterruptedException _) { /* */ }
            while (!cache.isEmpty());
            cleaner=null; // später erneut anloegen
         });
      if (cmds_.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      // builder=new ProcessBuilder(cmds); // alles soweit vorbereiten ohne zu starten
      // builder.environment().putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      list=new ArrayList<>(cmds_); // temporäre liste ohne rückwirkung
      if ((list.size() == 1) && !list.getFirst().matches("[_.\\p{Alnum}]+")) { // kein einfacher Befehl mit Parametern // nur eine einzige
                                                                               // Befehlszeile
         if (!list.getFirst().matches(".+[ ;|&].+")) { // Das ist unklar
            System.err.println(list.getFirst());
            throw new UnsupportedOperationException("Kann den Befehl nicht verstehen");
         }
         list.addFirst("-c");
      }
      if (list.getFirst().equals("-c")) // shell angefordert
         list.addFirst(SHELL);
      // cmds=cmds_;
   }
   @Override
   public void run() {
      if (builder instanceof ProcessBuilder) {
         // Process p=null;
         synchronized (builder) {
            try { // builder.command(list);
               process=builder.command(list).start();
               System.out.println("l:" + list);
            } catch (IOException e1) {
               e1.printStackTrace();
            }
         }
         if (process instanceof Process p) {
            // try {
            // System.out.println("run2");
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
                  if (!e.getMessage().equals("Stream closed")) {
                     e.printStackTrace();
                  }
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
                  if (!e.getMessage().equals("Stream closed")) {
                     e.printStackTrace();
                  }
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
         // } catch (final InterruptedException e) { // TODO Auto-generated catch block
         // e.printStackTrace();
         // }
      }
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
      final var queue=getQueue(cmds_);
      // while (!queue.isFertig())
      // Thread.onSpinWait();
      return (queue.getFirst() instanceof String erg) ? erg : or;
   }
   /**
    * @param cmds_
    * @return
    */
   @SuppressWarnings("resource")
   private static ProcessQueue2<String> getQueue(List<String> cmds_) {
      ProcessQueues2<String> qs=getQueues(cmds_);
      return qs.out;
   }
   static public ProcessQueues2<String> getQueues(List<String> cmds) {
      return getQueues(cmds, 60 * 60L);
   }
   static public ProcessQueues2<String> getQueues(List<String> cmds, long sec) {
      final var key2=String.join(";", cmds);
      System.out.println(key2);
      if (cache.containsKey(key2)) {
         // if( im cache) return aus dem cache
         final var dc=cache.get(key2);
         return dc.get2Queues();
      }
      final DoCached dc=new DoCached(cmds, sec);
      // dc.key=key2;
      // add to cache mit sec
      cache.put(key2, dc);
      // start in virtual thread
      // Thread.startVirtualThread(dc);
      Thread.ofPlatform().start(dc);
      return dc.first;
   }
   private ProcessQueues2<String> get2Queues() {
      final ProcessQueues2<String> next=new ProcessQueues2<>();
      // warte bis der erste Thread wirklich fertig ist
      while (c1 != null)
         Thread.onSpinWait();
      next.out.addAll(c2);
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
         // while (alive.get()) {
         // try {
         // Thread.sleep(1);
         // Thread.onSpinWait();
         // } catch (InterruptedException _) {}
         // }
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
      void setFertig() {
         out.setFertig();
         err.setFertig();
      }
      public void printErr() {
         Thread.startVirtualThread(() -> {
            while (err.poll() instanceof final E e)
               System.err.println(e);
         });
      }
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
      // public void waitFor() {
      // while (!out.isFertig())
      // Thread.onSpinWait();
      // };
   }
   /**
    * @param string
    * @param string2
    * @param string3
    */
   public static void doCmd(String... string) {
      doCmd(List.of(string));
   }
   /**
    * @param of
    */
   @SuppressWarnings("resource")
   private static void doCmd(List<String> list) {
      System.out.println(list);
      getQueues(list).toOut().toErr();// .waitFor();
   }
}

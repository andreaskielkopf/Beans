/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public class DoCached implements Runnable {
   private static ConcurrentSkipListMap<String, DoCached> cache=new ConcurrentSkipListMap<>();
   private final ProcessQueues2<String>                   first=new ProcessQueues2<>();
   private ProcessQueue2<String>                          c1   =new ProcessQueue2<>();
   private CopyOnWriteArrayList<String>                   c2;                                 // erstmal null
   private String                                         key;
   private long                                           timeout;
   final private @NonNull ProcessBuilder                  builder;
   static private Thread                                  cleaner;
   public DoCached(List<String> cmds) {
      this(cmds, 60 * 60L); // 1 Stunde = 60 Sec* 60 Min
   }
   /**
    * @param cmds
    * @param l
    */
   public DoCached(List<String> cmds, long sec) {
      timeout=System.currentTimeMillis() + sec * 1000L;
      if (cleaner == null)
         cleaner=Thread.startVirtualThread(() -> {
            long now;
            do
               try {
                  now=System.currentTimeMillis();
                  for (DoCached dc:cache.values())
                     if (dc.timeout < now)
                        cache.remove(dc.key);
                  Thread.sleep(5000); // alle 5 Sekunden
               } catch (InterruptedException _) { /* */ }
            while (!cache.isEmpty());
            cleaner=null; // später erneut anloegen
         });
      if (cmds.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      builder=new ProcessBuilder(cmds); // alles soweit vorbereiten ohne zu starten
   }
   @Override
   public void run() {
      System.out.println("run");
      System.out.println(builder.environment());
      if (builder instanceof final ProcessBuilder b)
         try (var process=b.start()) {
            System.out.println("run2");
            Thread.currentThread().setName(getClass().getSimpleName());
            /// Der ganze Empfang geschieht jetzt im Hintergrund und wandert in die queues
            Thread.startVirtualThread(() -> {
               try (var inp=process.inputReader()) {
                  while (inp.readLine() instanceof final String line) {
                     first.out.add(line); // direkt übergeben
                     c1.add(line); // Cache füllen
                  }
               } catch (final IOException e) {
                  System.err.println(e.getMessage());
                  e.printStackTrace();
               } finally {
                  first.setFertig();
                  c2=new CopyOnWriteArrayList<>(c1);
                  c1=null; // Das ist das Zeichen dafür dass alles kopiert ist ;-)
               }
            });
            Thread.startVirtualThread(() -> {
               try (var err=process.errorReader()) {
                  while (err.readLine() instanceof final String line)
                     first.err.add(line); // direkt übergeben
               } catch (final IOException e) {
                  System.err.println(e.getMessage());
                  e.printStackTrace();
               } finally {
                  if (!first.err.isEmpty()) // remove from cache
                     timeout=System.currentTimeMillis();
               }
            });
            process.waitFor();
         } catch (final IOException | InterruptedException e) { // TODO Auto-generated catch block
            e.printStackTrace();
         }
   }
   static public ProcessQueues2<String> getQueuesP(List<String> cmds) {
      return getQueuesP(cmds, 60 * 60L);
   }
   static public ProcessQueues2<String> getQueuesP(List<String> cmds, long sec) {
      final var key2=String.join(";", cmds);
      System.out.println(key2);
      if (cache.containsKey(key2)) {
         // if( im cache) return aus dem cache
         final var dc=cache.get(key2);
         return dc.get2Queues();
      }
      final DoCached dc=new DoCached(cmds, sec);
      dc.key=key2;
      // add to cache mit sec
      cache.put(key2, dc);
      // start in virtual thread
      Thread.startVirtualThread(dc);
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
//      public void waitFor() {
//         while (!out.isFertig())
//            Thread.onSpinWait();
//      };
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
      getQueuesP(list).toOut().toErr();//.waitFor();
   }
}

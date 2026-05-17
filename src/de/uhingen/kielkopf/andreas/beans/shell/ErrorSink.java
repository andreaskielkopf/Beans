/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public class ErrorSink {
   /** In dieser Queue werden Fehlermeldungen aller Streams gesammelt */
   final public @NonNull LinkedBlockingQueue<String> errorQueue;
   /** Der Counter gibt an wie viele streams gerade noch offen sind */
   final public @NonNull AtomicInteger               openStreamsCount;
   /** Ein ErrorSink der mehrfach genutzt werden kann um Fehlermeldungen einzufangen */
   public ErrorSink() {
      errorQueue=new LinkedBlockingQueue<>();
      openStreamsCount=new AtomicInteger();
   }
   /**
    * Fügt dem ErrorSink einen errorReader hinzu.
    * 
    * Alle Fehler werden in einer gemeinsamen Queue abgelegt, aus der sie von extern geholt werden können
    *
    * @param errorReader
    *
    */
   public void add(BufferedReader errorReader) {
      Thread.startVirtualThread(() -> {
         try (var bufferedReader=errorReader;) {
            Thread.currentThread().setName("ErrorSink-" + Integer.toString(openStreamsCount.incrementAndGet()));
            while (bufferedReader.readLine() instanceof final String line) // readLine blockiert
               errorQueue.put(line);
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
         } finally {
            openStreamsCount.decrementAndGet();
         }
      });
   }
}

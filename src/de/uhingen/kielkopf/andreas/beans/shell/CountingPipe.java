/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Eine CountingPipe die einen Inputstream mit einem Outputstream verbindet.
 *
 * und dabei die Datenpakete zählt. Die eigentliche CountingPipe läuft in einem virual Thread
 *
 *
 * @author Andreas Kielkopf
 * @since 20260517
 *
 */
public class CountingPipe {
   /** Die aktuelle Leseposition */
   final public @NonNull AtomicLong rPos  =new AtomicLong();
   /** Die aktuelle Schreibposition */
   final public @NonNull AtomicLong wPos  =new AtomicLong();
   /** Die zum Lesen eingesetzte Zeit */
   final public @NonNull AtomicLong rTime =new AtomicLong();
   /** Die zum Schreiben eingesetzte Zeit */
   final public @NonNull AtomicLong wTime =new AtomicLong();
   /** Der virtuelle Thread, der das macht */
   final public Thread              t;
   /** Flaf für die Fertigmeldung */
   final public AtomicBoolean       fertig=new AtomicBoolean();
   // public Info infoIn;
   // public Info infoOut;
   /**
    * @param in
    * @param out
    * @param sink
    * @param kBlocksize
    * @throws IOException
    *
    */
   @SuppressWarnings("resource")
   public CountingPipe(Process in, Process out, ErrorSink sink) throws IOException {
      sink.add(in.errorReader(StandardCharsets.UTF_8));
      // infoIn=in.info();
      sink.add(out.errorReader(StandardCharsets.UTF_8));
      // infoOut=out.info();
      this(in.getInputStream(), out.getOutputStream(), 1024); // 1MB
   }
   /**
    * @param in
    * @param out
    * @param kBlocksize
    */
   public CountingPipe(InputStream in, OutputStream out, int kBlocksize) {
      final var blockSize=1024 * Math.max(1, kBlocksize);
      t=Thread.startVirtualThread(() -> {
         Thread.currentThread().setName("CountingPipe");
         try (var i=in;
                  var o=out;
                  var bufferedIn=new BufferedInputStream(i);
                  var bufferedOut=new BufferedOutputStream(o)) {
            final var buffer=new byte[blockSize];
            int r;
            var a=System.nanoTime();
            var b=a;
            while ((r=bufferedIn.read(buffer)) >= 0) { // read blockiert
               a=System.nanoTime();
               rPos.addAndGet(r); // Positionen sind unterschiedlich
               rTime.addAndGet(a - b); // Lesezeit
               bufferedOut.write(buffer, 0, r); // write blockiert ...
               b=System.nanoTime();
               wPos.addAndGet(r); // Positionen sind gleich
               wTime.addAndGet(b - a); // Schreibzeit
               // Thread.sleep(1);
            }
            out.flush();
            wTime.addAndGet(System.nanoTime() - b); // Schreibzeit
            fertig.set(true);
         } catch (final IOException e) {
            e.printStackTrace();
            // } catch (InterruptedException e) {
            // e.printStackTrace();
         }
      });
   }
}

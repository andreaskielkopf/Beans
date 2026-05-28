/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ausführen beliebiger Commands aus java heraus durch Benutzung der SHell ($SHELL)
 *
 * @author Andreas Kielkopf
 *
 */
public class Do implements Runnable {
   /** Welche Shell wird verwendet */
   public static String  SHELL           ="";
   /** Welcher Benutzer agiert */
   public static String  USERNAME        ="";
   // public static boolean USE_SHELL=true;
   public static boolean LOG_ALL_COMMANDS=false;
   /** Beim Programmstart klären wie die Umgebung aussieht */
   static {
      System.out.print("static: ");
      getShell();
      isRoot();
      System.out.println();
   }
   /**
    * Ermittle welche Shell benutzt wird
    *
    * @return
    */
   final static String getShell() {
      if (SHELL.isBlank()) {
         SHELL="/bin/sh"; // sonst schlägt der erste Test-Befehl fehl
         SHELL=doGetFirstOr(List.of("-c", "echo ${SHELL}"), SHELL);
         System.out.print("$SHELL=" + SHELL);
      }
      return SHELL;
   }
   /**
    * Ermittle den Benutzernamen ?
    *
    * @return isRoot
    */
   final static public boolean isRoot() {
      if (USERNAME.isBlank()) {
         USERNAME=doGetFirstOr(List.of("whoami"), "noOne");
         System.out.print(", user=" + USERNAME);
      }
      return USERNAME.equals("root"); // System.out.println(SHELL);
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
   private static String doGetFirstOr(List<String> cmds_, String or) {
      final var queue=doGetList(cmds_);
      return queue.isEmpty() ? or : queue.getFirst();
   }
   /**
    * Liefert die erste Zeile die diese Commands ausspucken, oder den Ersatz dafür
    *
    * @param cmd_or
    *           Commands, und als letzten String den Ersatz
    * @return
    */
   public static String doGetFirstOr(String... cmd_or) {
      if (cmd_or.length < 2)
         throw new UnsupportedOperationException("Es müssen mindestens 2 Paramter übergeben werden");
      final ArrayList<String> cmds=new ArrayList<>(Arrays.asList(cmd_or)); // bearbeitbare Liste
      final var or=cmds.removeLast();
      return doGetFirstOr(cmds, or);
   }
   /**
    * Führt diese Commands aus. Meldungen sind alle auf der Konsole
    *
    * @param cmds_
    *           Liste mit den Commands
    */
   static void doCmd(List<String> cmds_) {
      new Do(cmds_, Worker.stdOut(), Worker.stdErr()).executePlatform();
   }
   /**
    * @param cmds_
    *           Liste mit den Commands
    */
   static public void doCmd(String... cmds_) {
      doCmd(Arrays.asList(cmds_));
   }
   /**
    * @param cmds_
    *           Liste mit den Commands
    */
   static public void sudoCmd(String... cmds_) {
      doCmd(insertSudo(Arrays.asList(cmds_)));
   }
   static private List<String> insertSudo(List<String> cmds_) {
      if (isRoot())
         return cmds_;
      System.out.print("we need sudo to: ");
      if (!LOG_ALL_COMMANDS) { // Kontrollausgabe vor der Abfrage des Passworts
         for (final String s:cmds_)
            System.out.println(" " + s);
         System.out.println();
      }
      final ArrayList<String> b=new ArrayList<>(cmds_);
      b.addFirst("sudo " + b.removeFirst()); // ergänze erste Zeile
      return b;
   }
   static ProcessOutputQueue<String> doGetList(List<String> cmds_) {
      final ProcessOutputQueue<String> erg=new ProcessOutputQueue<>();
      try {
         final Do d=new Do(cmds_, new Worker() {
            @Override
            public void processLine(String line) {
               erg.add(line);
            }
         }) {
            @Override
            public ProcessOutputQueue<String> get() {
               executePlatform();
               erg.setFertig();
               return erg;
            }
         };// .get();
         Thread.startVirtualThread(() -> d.get());
      } catch (final UnsupportedOperationException e) {
         e.printStackTrace();
      }
      return erg;
   }
   /**
    * Ergibt den vollständigen Output als Liste
    *
    * @param cmds_
    *           Liste mit den Commands
    * @return queue
    */
   public static ProcessOutputQueue<String> sudoGetList(String... cmds_) {
      return doGetList(insertSudo(Arrays.asList(cmds_)));
   }
   /**
    * Ergibt den vollständigen Output als Liste
    *
    * @param cmds_
    *           Liste mit den Commands
    * @return queue
    */
   public static ProcessOutputQueue<String> doGetList(String... cmds_) {
      return doGetList(Arrays.asList(cmds_));
   }
   // public Do(String... s) { this(Arrays.asList(s)); }
   public Do(String... list1) {
      this(Worker.collectInp(), Worker.stdErr(), list1);
   }
   public Do(Worker oWorker, String... list1) {
      this(oWorker, Worker.stdErr(), list1);
   }
   public Do(List<String> list1) {
      this(list1, Worker.collectInp(), Worker.stdErr());
   }
   public Do(List<String> list1, Worker oWorker) {
      this(list1, oWorker, Worker.stdErr());
   }
   Worker               inpWorker;
   Worker               errWorker;
   final ProcessBuilder builder;
   Process              process;
   AtomicBoolean        done=new AtomicBoolean(false);
   String               name;
   ArrayList<String>    list;
   public Do(Worker iWorker, Worker eWorker, String... list1) {
      this(new ArrayList<>(Arrays.asList(list1)), iWorker, eWorker);
   }
   /**
    * Führe den Befehl aus
    *
    * @param cmd_
    *           Befehlsliste
    * @param inpWorker_
    *           Worker für Output-Stream
    * @param errWorker_
    *           Worker für Error-Stream
    */
   public Do(List<String> cmd_, Worker inpWorker_, Worker errWorker_) {
      if (cmd_.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      inpWorker=inpWorker_; // (iWorker != null) ? iWorker : Worker.collectInp();
      errWorker=errWorker_;// (eWorker != null) ? eWorker : Worker.stdErr;
      list=new ArrayList<>(cmd_); // temporäre liste ohne rückwirkung
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
      if (LOG_ALL_COMMANDS) {
         final StringBuilder sb=new StringBuilder(">");
         for (final String s:cmd_)
            sb.append(" ").append(s);
         System.out.println(sb.toString());
      }
      builder=new ProcessBuilder(list);
   }
   @Override
   public void run() {
      try {
         if (done.get())
            return;
         if (process == null)
            process=builder.start();
         try (var err=process.errorReader(); var inp=process.inputReader()) {
            while (process.isAlive())
               readAll(err, inp);
            readAll(err, inp);
            final var x=process.exitValue();
            if (x != 0) {
               System.err.print(list);
               System.err.println(" exit with " + x);
            }
         }
      } catch (final IOException e) {
         System.err.println(e);
      } finally {
         done.set(true);
      }
   }
   public void runPiped() {
      try {
         if (done.get())
            return;
         try (var err=process.errorReader()) {
            System.err.println("CountingPipe alive=" + process.isAlive());
            System.err.println("BR=" + err.hashCode());
            while (process.isAlive()) {
               System.err.print(":");
               while (err.readLine() instanceof final String line) {
                  System.err.print(".");
                  errWorker.processLine(line);
                  System.err.print(",");
                  System.err.println(line);
               }
            }
         }
      } catch (final IOException e) {
         e.printStackTrace();
      } finally {
         done.set(true);
      }
   }
   void readAll(BufferedReader err, BufferedReader inp) throws IOException {
      try {
         Thread.sleep(1L);
         if (inpWorker instanceof Worker) // bei null ignorieren
            while (inp.ready())
               inpWorker.processLine(inp.readLine());
         if (errWorker instanceof Worker) // bei null ignorieren
            while (err.ready())
               errWorker.processLine(err.readLine());
      } catch (final InterruptedException ignoree) { /* */ }
   }
   void readVirtual(BufferedReader err, BufferedReader inp) throws IOException {
      if (inpWorker instanceof final Worker iw)
         iw.withVirtual(inp);
      if (errWorker instanceof final Worker ew)
         ew.withVirtual(err);
   }
   /** Warte bis der Prozess zu ende ist, aber verschwende keine Leistung dabei */
   private void waitFor() {
      try {
         if (inpWorker instanceof final Worker iw)
            iw.waitFor();
         if (errWorker instanceof final Worker ew)
            ew.waitFor();
         while (!done.get()) {
            Thread.onSpinWait();
            Thread.sleep(1L);
         }
      } catch (final InterruptedException ignore) {/* */ }
   }
   Worker getInpWorker() {
      return inpWorker;
   }
   Worker getErrWorker() {
      return errWorker;
   }
   public Do executePlatform() {
      if (process == null)
         Thread.ofPlatform().start(this);
      waitFor();
      return this;
   }
   static public Do executePlatform(List<String> l) {
      return new Do(l).executePlatform();
   }
   public ProcessOutputQueue<String> get() {
      executePlatform();
      if (inpWorker instanceof final Worker w)
         return w.get();
      return null;
   }
   static public Do toPipe(String... list) {
      return toPipe(new ArrayList<>(Arrays.asList(list)));
   }
   static public Do toPipe(List<String> l) {
      return new Do(l, null, Worker.stdErr()) {
         @Override
         public void run() {
            runPiped();
            /* wird automatisch an die CountingPipe angehängt */}
      };
   }
   static public Do toWorker(String... list) {
      return toWorker(new ArrayList<>(Arrays.asList(list)));
   }
   static public Do toWorker(List<String> l) {
      return new Do(l, Worker.collectInp(), Worker.collectErr());
   }
   static public Do toConsole(List<String> l) {
      return new Do(l, Worker.stdOut(), Worker.stdErr());
   }
   public Do clean() {
      done.set(false);
      process=null;
      if (inpWorker instanceof final Worker w)
         w.clean();
      if (inpWorker instanceof final Worker w)
         w.clean();
      return this;
   }
   @Override
   public String toString() {
      return new StringBuilder(" Do").append(list).append(" ").append(inpWorker).append(" ").append(errWorker)
               .append(" ").toString();
   }
   /**
    * @author Andreas Kielkopf
    *
    * @param <E>
    */
   static public class ProcessOutputQueue<E> extends LinkedBlockingQueue<E> {
      private static final long serialVersionUID=-6329627411403388008L;
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
      /**
       * Angepasst, so dass Poll nicht schiefgeht solange der Prozess noch läuft
       */
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
}

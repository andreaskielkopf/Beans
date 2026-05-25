/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Ausführen beliebiger Commands aus java heraus durch Benutzung der SHell ($SHELL)
 * 
 * @author Andreas Kielkopf
 *
 */
public class Do implements Runnable {
   /** Welche Shell wird verwendet */
   public static String SHELL   ="";
   /** Welcher Benutzer agiert */
   public static String USERNAME="";
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
      ConcurrentLinkedDeque<String> queue=doGetList(cmds_);
      return (queue.isEmpty()) ? or : queue.getFirst();
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
      ArrayList<String> cmds=new ArrayList<>(Arrays.asList(cmd_or)); // bearbeitbare Liste
      String or=cmds.removeLast();
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
         for (String s:cmds_)
            System.out.println(" " + s);
         System.out.println();
      }
      ArrayList<String> b=new ArrayList<>(cmds_);
      b.addFirst("sudo " + b.removeFirst()); // ergänze erste Zeile
      return b;
   }
   static ConcurrentLinkedDeque<String> doGetList(List<String> cmds_) {
      ConcurrentLinkedDeque<String> erg=new ConcurrentLinkedDeque<>();
      try {
         return new Do(cmds_, new Worker() {
            @Override
            public void processLine(String line) {
               erg.add(line);
            }
         }) {
            @Override
            public ConcurrentLinkedDeque<String> get() {
               executePlatform();
               return erg;
            }
         }.get();
      } catch (UnsupportedOperationException e) {
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
   public static ConcurrentLinkedDeque<String> sudoGetList(String... cmds_) {
      return doGetList(insertSudo(Arrays.asList(cmds_)));
   }
   /**
    * Ergibt den vollständigen Output als Liste
    * 
    * @param cmds_
    *           Liste mit den Commands
    * @return queue
    */
   public static ConcurrentLinkedDeque<String> doGetList(String... cmds_) {
      return doGetList(Arrays.asList(cmds_));
   }
   /**
    * main als Programmtest
    * 
    * @param args
    */
   public static void main(String[] args) {
      System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
      System.out.println();
      // Das folgende sind alles Musterzeilen für mögliche Nutzung von Do
      System.out.println(doGetFirstOr("whoami", "niemand (kein Fehler)"));
      System.out.println(doGetFirstOr("whoami", "5", "Fehlermeldung von whoami ;-)"));
      System.out.println(doGetFirstOr("whoa%i", "Ein solches Programm gibt es nicht"));
      System.out.println(doGetFirstOr("whoa_i", "Ein solches Programm gibt es nicht"));
      System.out.println(doGetFirstOr("whoa i", "Ein solches Programm gibt es nicht"));
      System.out.println(doGetFirstOr("-c", "whoami", "kein Fehler ;-)")); // mit shell
      doCmd("ls", "-lA", "/home"); // cmd + parameter
      doCmd("ls -lA /home"); // cmd + parameter
      doCmd("ls -lA /hom*"); // cmd + parameter mit glob OK
      doCmd("ls", "-lA", "/hom*"); // cmd + parameter mit glob geht nicht !!!
      doCmd("-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      doCmd(SHELL, "-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      doCmd("ls", "-lA", "/home", "/home/andreas"); // cmd + 3 parameter
      doCmd("ls -lA /home /home/andreas"); // cmd + 3 parameter
      doCmd("ls -lA /;ls -lA /home"); //
      doCmd("ls", "-lA", "s"); // alles über sterr ausgeben
      doCmd("ls -lA /home|grep -E ^drwx|sort -nk 5");
      /// die folgende Zeile braucht die java-shell-programme ~/bin/src und ~/bin/dst um zu funktionieren
      // doCmd("time ~/bin/src|pv -pteabfW -i 1|~/bin/dst");
      System.out.println();
      System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
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
   boolean              done=false;
   String               name;
   ArrayList<String>    list;
   public Do(Worker iWorker, Worker eWorker, String... list1) {
      this(new ArrayList<String>(Arrays.asList(list1)), iWorker, eWorker);
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
      if (list.size() == 1) { // nur eine einzige Befehlszeile
         if (!list.getFirst().matches("[_.\\p{Alnum}]+")) { // kein einfacher Befehl mit Parametern
            if (list.getFirst().matches(".+[ ;|&].+")) { // mehrere Befehle in einer Zeile brauchen die shell
               list.addFirst("-c");
            } else { // Das ist unklar
               System.err.println(list.getFirst());
               throw new UnsupportedOperationException("Kann den Befehl nicht verstehen");
            }
         }
      }
      if (list.getFirst().equals("-c")) // shell angefordert
         list.addFirst(SHELL);
      if (LOG_ALL_COMMANDS) {
         StringBuilder sb=new StringBuilder(">");
         for (String s:cmd_)
            sb.append(" ").append(s);
         System.out.println(sb.toString());
      }
      builder=new ProcessBuilder(list);
   }
   @Override
   public void run() {
      try {
         if (done)
            return;
         if (process == null)
            process=builder.start();
         try (BufferedReader err=process.errorReader(); BufferedReader inp=process.inputReader()) {
            while (process.isAlive())
               readAll(err, inp);
            readAll(err, inp);
            int x=process.exitValue();
            if (x != 0) {
               System.err.print(list);
               System.err.println(" exit with " + x);
            }
         }
      } catch (IOException e) {
         System.err.println(e);
         // e.printStackTrace();
      } finally {
         done=true;
      }
   }
   public void runPiped() {
      try {
         if (done)
            return;
         // if (process == null)
         // process=builder.start();
         try (BufferedReader err=process.errorReader()) {
            System.err.println("CountingPipe alive=" + process.isAlive());
            System.err.println("BR=" + err.hashCode());
            while (process.isAlive()) {
               System.err.print(":");
               while (err.readLine() instanceof String line) {
                  System.err.print(".");
                  errWorker.processLine(line);
                  System.err.print(",");
                  System.err.println(line);
               }
            }
            // System.err.println("CountingPipe not alive");
            // readAll(err, null);
            // int x=process.exitValue();
            // if (x != 0) {
            // System.err.print(list);
            // System.err.println(" exit with " + x);
            // }
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         done=true;
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
      } catch (InterruptedException ignoree) { /* */ }
   }
   void readVirtual(BufferedReader err, BufferedReader inp) throws IOException {
      if (inpWorker instanceof Worker iw)
         iw.withVirtual(inp);
      if (errWorker instanceof Worker ew)
         ew.withVirtual(err);
   }
   /** Warte bis der Prozess zu ende ist, aber verschwende keine Leistung dabei */
   private void waitFor() {
      try {
         if (inpWorker instanceof Worker iw)
            iw.waitFor();
         if (errWorker instanceof Worker ew)
            ew.waitFor();
         while (done == false) {
            Thread.onSpinWait();
            Thread.sleep(1L);
         }
      } catch (InterruptedException ignore) {/* */ }
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
      return (Do) this;
   }
   static public Do executePlatform(List<String> l) {
      return new Do(l).executePlatform();
   }
   public ConcurrentLinkedDeque<String> get() {
      executePlatform();
      if (inpWorker instanceof Worker w)
         return w.get();
      return null;
   }
   static public Do toPipe(String... list) {
      return toPipe(new ArrayList<String>(Arrays.asList(list)));
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
      return toWorker(new ArrayList<String>(Arrays.asList(list)));
   }
   static public Do toWorker(List<String> l) {
      return new Do(l, Worker.collectInp(), Worker.collectErr());
   }
   static public Do toConsole(List<String> l) {
      return new Do(l, Worker.stdOut(), Worker.stdErr());
   }
   public Do clean() {
      done=false;
      process=null;
      if (inpWorker instanceof Worker w)
         w.clean();
      if (inpWorker instanceof Worker w)
         w.clean();
      return this;
   }
   @Override
   public String toString() {
      return new StringBuilder(" Do").append(list).append(" ").append(inpWorker).append(" ").append(errWorker)
               .append(" ").toString();
   }
}

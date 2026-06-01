/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.util.List;

import de.uhingen.kielkopf.andreas.beans.shell.Do;
import de.uhingen.kielkopf.andreas.beans.shell.DoCached;

/**
 * @author Andreas Kielkopf
 *
 */
public class TestDoCached {
   /**
    * main als Programmtest
    *
    * @param args
    * @throws InterruptedException
    */
   public static void main(String[] args) throws InterruptedException {
      // System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
      // System.out.println();
      // // Das folgende sind alles Musterzeilen für mögliche Nutzung von Do
      // System.out.println(Do.doGetFirstOr("whoami", "niemand (kein Fehler)"));
      // System.out.println(Do.doGetFirstOr("whoami", "5", "Fehlermeldung von whoami 5 ;-)"));
      // System.out.println(Do.doGetFirstOr("whoa%i", "Ein solches Programm gibt es nicht"));
      // System.out.println(Do.doGetFirstOr("whoa_i", "Ein solches Programm gibt es nicht"));
      // System.out.println(Do.doGetFirstOr("whoa i", "Ein solches Programm gibt es nicht"));
      // System.out.println(Do.doGetFirstOr("-c", "whoami", "kein Fehler ;-)")); // mit shell
      // Do.doCmd("ls", "-lA", "/home"); // cmd + parameter
//      Thread.sleep(1000);
      DoCached.doCmd("ls", "-lA", "/home"); // cmd + parameter
      // DoCached.doCmd("/bin/sh", "-c", "echo \"$SHELL\""); // cmd + parameter
      // DoCached.doCmd("/bin/sh", "-c", "echo ${SHELL}"); // cmd + parameter
      System.out.println("---------------");
      DoCached.doCmd("ls", "-lA", "/home"); // cmd + parameter
      // String n=DoCached.getFirstOr(List.of("/bin/sh", "-c", "echo ${SHELL}"), "nothing");
      // System.out.println("n: "+n);
      // DoCached.doCmd("ls", "-lA", "/home"); // cmd + parameter
      // Do.doCmd("ls -lA /home"); // cmd + parameter
      // Do.doCmd("ls -lA /hom*"); // cmd + parameter mit glob OK
      // Do.doCmd("ls", "-lA", "/hom*"); // cmd + parameter mit glob geht nicht !!!
      // Do.doCmd("-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      // Do.doCmd(Do.SHELL, "-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      // Do.doCmd("ls", "-lA", "/home", "/home/andreas"); // cmd + 3 parameter
      // Do.doCmd("ls -lA /home /home/andreas"); // cmd + 3 parameter
      // Do.doCmd("ls -lA /;ls -lA /home"); //
      // Do.doCmd("ls", "-lA", "s"); // alles über sterr ausgeben
      // Do.doCmd("ls -lA /home|grep -E ^drwx|sort -nk 5");
      // /// die folgende Zeile braucht die java-shell-programme ~/bin/src und ~/bin/dst um zu funktionieren
      // // doCmd("time ~/bin/src|pv -pteabfW -i 1|~/bin/dst");
      // Do.doCmd("ls -lA /home"); // cmd + parameter
      // System.out.println("++++++++++++");
      // final var q2=Do.doGetQueues("ls -lA /sys;sleep 10;ls -lA /root;ls -lA /home|grep -o '^[drwx-]*'");
      // Thread.startVirtualThread(() -> {
      // System.out.println("start 1");
      // while (q2.out.poll() instanceof final String line)
      // System.out.println(line);
      // System.out.println("end 1");
      // });
      // Thread.startVirtualThread(() -> {
      // System.out.println("start 2");
      // while (q2.err.poll() instanceof final String line)
      // System.err.println(line);
      // System.out.println("end 2");
      // });
      // final var q=Do.doGetQueues("ls -lA /home;sleep 3;Fehler;sleep 1;echo 'weiter nach Fehler';sleep 15;ls -lA /home");
      // System.out.println("start 3");
      // q.printErr(); // fortlaufend virtuell
      // while (q.out.poll() instanceof final String line)
      // System.out.println(line);
      // System.out.println("end 3");
      // // ProcessOutputQueue<String> q2=Do.doGetList("ls -lA /home", "sleep 10", "ls -lA /home");
      // // while (q2.poll() instanceof String line)
      // // System.out.println(line);
      // System.out.println("++++++++++++");
      // System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
      Thread.sleep(5000);
   }
}

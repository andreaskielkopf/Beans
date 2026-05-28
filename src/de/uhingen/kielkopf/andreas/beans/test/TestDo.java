/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.test;

import de.uhingen.kielkopf.andreas.beans.shell.Do;

/**
 * @author Andreas Kielkopf
 *
 */
public class TestDo {
   /**
    * main als Programmtest
    *
    * @param args
    */
   public static void main(String[] args) {
      System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
      System.out.println();
      // Das folgende sind alles Musterzeilen für mögliche Nutzung von Do
      System.out.println(Do.doGetFirstOr("whoami", "niemand (kein Fehler)"));
      System.out.println(Do.doGetFirstOr("whoami", "5", "Fehlermeldung von whoami ;-)"));
      System.out.println(Do.doGetFirstOr("whoa%i", "Ein solches Programm gibt es nicht"));
      System.out.println(Do.doGetFirstOr("whoa_i", "Ein solches Programm gibt es nicht"));
      System.out.println(Do.doGetFirstOr("whoa i", "Ein solches Programm gibt es nicht"));
      System.out.println(Do.doGetFirstOr("-c", "whoami", "kein Fehler ;-)")); // mit shell
      Do.doCmd("ls", "-lA", "/home"); // cmd + parameter
      Do.doCmd("ls -lA /home"); // cmd + parameter
      Do.doCmd("ls -lA /hom*"); // cmd + parameter mit glob OK
      Do.doCmd("ls", "-lA", "/hom*"); // cmd + parameter mit glob geht nicht !!!
      Do.doCmd("-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      Do.doCmd(Do.SHELL, "-c", "ls -lA /hom*"); // cmd + parameter mit glob per shell "-c"
      Do.doCmd("ls", "-lA", "/home", "/home/andreas"); // cmd + 3 parameter
      Do.doCmd("ls -lA /home /home/andreas"); // cmd + 3 parameter
      Do.doCmd("ls -lA /;ls -lA /home"); //
      Do.doCmd("ls", "-lA", "s"); // alles über sterr ausgeben
      Do.doCmd("ls -lA /home|grep -E ^drwx|sort -nk 5");
      /// die folgende Zeile braucht die java-shell-programme ~/bin/src und ~/bin/dst um zu funktionieren
      // doCmd("time ~/bin/src|pv -pteabfW -i 1|~/bin/dst");
      Do.doCmd("ls -lA /home"); // cmd + parameter
      final var q=Do.doGetList("ls -lA /home;sleep 10;ls -lA /home");
      while (q.poll() instanceof final String line)
         System.out.println(line);
      // ProcessOutputQueue<String> q2=Do.doGetList("ls -lA /home", "sleep 10", "ls -lA /home");
      // while (q2.poll() instanceof String line)
      // System.out.println(line);
      System.out.println("++++++++++++");
      System.out.println("Das ist ein Testprogramm das teils absichtlich Fehler provoziert");
   }
}

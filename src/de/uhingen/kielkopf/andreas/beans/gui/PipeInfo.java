/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.uhingen.kielkopf.andreas.beans.shell.CountingPipe;

/**
 * @author Andreas Kielkopf
 *
 */
public class PipeInfo extends JPanel {
   private static final long serialVersionUID=1L;
   private JPanel            panel;
   private JLabel            lblReadPos;
   private JLabel            lblWritePos;
   private JLabel            lblReadTime;
   private JLabel            lblWriteTime;
   private boolean           changed;
   /**
    * Create the panel.
    */
   public PipeInfo() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(0, 4, 10, 10));
         panel.add(getLblReadPos());
         panel.add(getLblWritePos());
         panel.add(getLblReadTime());
         panel.add(getLblWriteTime());
      }
      return panel;
   }
   private JLabel getLblReadPos() {
      if (lblReadPos == null) {
         lblReadPos=new JLabel("readPos");
      }
      return lblReadPos;
   }
   private JLabel getLblWritePos() {
      if (lblWritePos == null) {
         lblWritePos=new JLabel("writePos");
      }
      return lblWritePos;
   }
   private JLabel getLblReadTime() {
      if (lblReadTime == null) {
         lblReadTime=new JLabel("readTime");
      }
      return lblReadTime;
   }
   private JLabel getLblWriteTime() {
      if (lblWriteTime == null) {
         lblWriteTime=new JLabel("writeTime");
      }
      return lblWriteTime;
   }
   /**
    * Überwache die CountingPipe und zeige die Ergebnise an
    * 
    * @param p
    */
   public void watch(CountingPipe p) {
      if (p instanceof CountingPipe)
         Thread.startVirtualThread(() -> {
            Thread.currentThread().setName("PipeInfo");
            long a=0, b=0, c=0, d=0;
            while (p instanceof final CountingPipe cp/* && !cp2.fertig.get() */) {
               changed=false;
               a=show(a, cp.rPos, getLblReadPos(), false);
               b=show(b, cp.rTime, getLblReadTime(), true);
               c=show(c, cp.wPos, getLblWritePos(), false);
               d=show(d, cp.wTime, getLblWriteTime(), true);
               if (changed)
                  SwingUtilities.invokeLater(() -> repaint(500));
               try {
                  Thread.sleep(500);
               } catch (final InterruptedException e) {
                  e.printStackTrace();
               }
            }
            System.out.println("PipeInfo: watch() beendet");
         });
   }
   /**
    * zeige Veränderungen der Werte in der Gui an
    */
   private long show(long x, AtomicLong y, JLabel z, boolean zeit) {
      final var tmp=y.get();
      if (x != tmp) {
         StringBuilder txt=new StringBuilder();
         if (zeit) {
            txt.append(Long.toString(tmp / 1_000_000L)); // Milli-Sekunden
            if (txt.length() > 3)
               for (var i=txt.length() - 3; i > 0; i-=3)
                  txt.insert(i, ".");
            txt.append(" ms");
         } else {
            txt.append(Long.toString(tmp / (1024 * 1024L))); // Megabyte
            if (txt.length() > 3)
               for (var i=txt.length() - 3; i > 0; i-=3)
                  txt.insert(i, ".");
            txt.append(" MByte");
         }
         if (txt.toString() instanceof String s && !s.equals(z.getText())) {
            changed=true;
            SwingUtilities.invokeLater(() -> z.setText(s));
         }
      }
      return tmp;
   }
}

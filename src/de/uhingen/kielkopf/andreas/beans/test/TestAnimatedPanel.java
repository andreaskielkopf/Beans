/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.awt.EventQueue;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.uhingen.kielkopf.andreas.beans.backsnap.hasColor;
import de.uhingen.kielkopf.andreas.beans.gui.AnimatedPanel;
import de.uhingen.kielkopf.andreas.beans.gui.FrameHelper;

import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Test-Application für AnimatedPanel
 * 
 * @author Andreas Kielkopf
 *
 */
public class TestAnimatedPanel {
   private JFrame                      frame;
   private JPanel                      panel;
   private AnimatedPanel<ColorInteger> animatedPanel;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            TestAnimatedPanel window=new TestAnimatedPanel();
            window.frame.setVisible(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }
   /**
    * Create the application.
    */
   private TestAnimatedPanel() {
      initialize();
      getAnimatedPanel().getDelegate().setBackground(Color.YELLOW.brighter());
      Thread.startVirtualThread(() -> {
         try {
            Thread.currentThread().setName("Insert Objects");
            SecureRandom sr=SecureRandom.getInstanceStrong();
            for (int i=0; i < 5000; i++) {
               int j=sr.nextInt() % 4000 + i;
               if (getAnimatedPanel() instanceof AnimatedPanel<TestAnimatedPanel.ColorInteger> ap)
                  SwingUtilities.invokeLater(() -> ap.add(new ColorInteger(j)));
               Thread.sleep(i / 10);
            }
         } catch (InterruptedException | NoSuchAlgorithmException e) {
            e.printStackTrace();
         }
      });
      Thread.startVirtualThread(() -> {
         try {
            Thread.currentThread().setName("Delete Objects");
            SecureRandom sr=SecureRandom.getInstanceStrong();
            for (int i=0; i < 10000; i++) {
               int j2=sr.nextInt() % 4000;
               if (getAnimatedPanel() instanceof AnimatedPanel<TestAnimatedPanel.ColorInteger> ap)
                  SwingUtilities.invokeLater(() -> ap.delete(new ColorInteger(j2)));
               Thread.sleep(i / 10 + 5);
            }
         } catch (InterruptedException | NoSuchAlgorithmException e) {
            e.printStackTrace();
         }
      });
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.CENTER);
      FrameHelper.restore(frame);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getAnimatedPanel(), BorderLayout.CENTER);
      }
      return panel;
   }
   private AnimatedPanel<ColorInteger> getAnimatedPanel() {
      if (animatedPanel == null) {
         animatedPanel=new AnimatedPanel<ColorInteger>();
         animatedPanel.setMsDelete(55000);// 55 Sekunden
      }
      return animatedPanel;
   }
   private class ColorInteger implements hasColor, Comparable<ColorInteger> {
      final int i;
      /**
       * Farbiger Integer, für Tests
       */
      public ColorInteger(int i_) {
         i=i_;
      }
      @Override
      public Color getForeground() {
         return i < 0 ? Color.RED : Color.BLACK;
      }
      @Override
      public Color getBackground() {
         return (i % 2 == 0) ? Color.YELLOW : Color.ORANGE;
      }
      @Override
      public String toString() {
         return Integer.toString(i);
      }
      @Override
      public int compareTo(ColorInteger o) {
         return Integer.compare(i, o.i);
      }
   }
}

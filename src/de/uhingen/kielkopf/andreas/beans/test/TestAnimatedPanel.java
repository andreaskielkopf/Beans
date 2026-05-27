/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;


import de.uhingen.kielkopf.andreas.beans.gui.AnimatedPanel;
import de.uhingen.kielkopf.andreas.beans.gui.AnimatedPanel.hasColors;
import de.uhingen.kielkopf.andreas.beans.gui.AnimatedPanel.hasName;
import de.uhingen.kielkopf.andreas.beans.gui.FrameHelper;

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
   private AnimatedPanel<ColorInteger> animatedPanel_1;
   private AnimatedPanel<ColorInteger> animatedPanel_2;
   private AnimatedPanel<ColorInteger> animatedPanel_3;
   private JPanel                      panel_1;
   private JPanel                      panel_2;
   private JPanel                      panel_3;
   private JPanel                      panel_4;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            final TestAnimatedPanel window=new TestAnimatedPanel();
            window.frame.setVisible(true);
         } catch (final Exception e) {
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
            Thread.currentThread().setName(getClass().getSimpleName()+" Insert");
            final var sr=SecureRandom.getInstanceStrong();
            for (var i=0; i < 1000; i++) {
               final var j=i;
               SwingUtilities.invokeLater(() -> {
                  getAnimatedPanel().add(new ColorInteger(sr.nextInt() % 4000 + j));
                  getAnimatedPanel_1().add(new ColorInteger(sr.nextInt() % 2000 + j));
                  getAnimatedPanel_2().add(new ColorInteger(sr.nextInt() % 400 + j));
                  getAnimatedPanel_3().add(new ColorInteger(sr.nextInt() % 1000 + j));
               });
               Thread.sleep(i / 5 + 10);
            }
         } catch (InterruptedException | NoSuchAlgorithmException e) {
            e.printStackTrace();
         }
      });
      Thread.startVirtualThread(() -> {
         try {
            Thread.currentThread().setName(getClass().getSimpleName()+" Delete");
            final var sr=SecureRandom.getInstanceStrong();
            for (var i=0; i < 5000; i++) {
               SwingUtilities.invokeLater(() -> {
                  getAnimatedPanel().delete(new ColorInteger(sr.nextInt() % 4000));
                  getAnimatedPanel_1().delete(new ColorInteger(sr.nextInt() % 2000));
                  getAnimatedPanel_2().delete(new ColorInteger(sr.nextInt() % 400));
                  getAnimatedPanel_3().delete(new ColorInteger(sr.nextInt() % 1000));
               });
               Thread.sleep(i / 5 + 25);
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
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.CENTER);
      FrameHelper.restore(frame);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(2, 2, 0, 0));
         panel.add(getPanel_1());
         panel.add(getPanel_2());
         panel.add(getPanel_3());
         panel.add(getPanel_4());
      }
      return panel;
   }
   private AnimatedPanel<ColorInteger> getAnimatedPanel() {
      if (animatedPanel == null) {
         animatedPanel=new AnimatedPanel<>();
         animatedPanel.setMsDelete(55000);// 55 Sekunden
      }
      return animatedPanel;
   }
   private static class ColorInteger implements hasName, hasColors, Comparable<ColorInteger> {
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
         return i % 2 == 0 ? Color.YELLOW : Color.ORANGE;
      }
      @Override
      public String toString() {
         return Integer.toString(i);
      }
      @Override
      public int compareTo(ColorInteger o) {
         return Integer.compareUnsigned(i, o.i);
      }
      @Override
      public String getName() {
         return "0x" + Integer.toHexString(i).toUpperCase();
      }
   }
   private AnimatedPanel<ColorInteger> getAnimatedPanel_1() {
      if (animatedPanel_1 == null) {
         animatedPanel_1=new AnimatedPanel<>();
         animatedPanel_1.setMsDelete(15000);
         animatedPanel_1.getDelegate().setBorder(
                  new TitledBorder(null, "Title", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         animatedPanel_1.getShadow().setBorder(
                  new TitledBorder(null, "Title", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      }
      return animatedPanel_1;
   }
   private AnimatedPanel<ColorInteger> getAnimatedPanel_2() {
      if (animatedPanel_2 == null) {
         animatedPanel_2=new AnimatedPanel<>();
         animatedPanel_2.setMsDelete(60000);
         animatedPanel_2.setMsAnimation(50);
      }
      return animatedPanel_2;
   }
   private AnimatedPanel<ColorInteger> getAnimatedPanel_3() {
      if (animatedPanel_3 == null) {
         animatedPanel_3=new AnimatedPanel<>();
         animatedPanel_3.setMsDelete(5000);
         animatedPanel_3.setMsAnimation(10);
         animatedPanel_3.getDelegate().setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
         animatedPanel_3.getShadow().setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
      }
      return animatedPanel_3;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setBorder(
                  new TitledBorder(null, "AnimatedPanel 1", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_1.setLayout(new GridLayout(1, 0, 0, 0));
         panel_1.add(getAnimatedPanel());
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
         panel_2.setLayout(new GridLayout(1, 0, 0, 0));
         panel_2.add(getAnimatedPanel_1());
      }
      return panel_2;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JPanel();
         panel_3.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
         panel_3.setLayout(new GridLayout(1, 0, 0, 0));
         panel_3.add(getAnimatedPanel_2());
      }
      return panel_3;
   }
   private JPanel getPanel_4() {
      if (panel_4 == null) {
         panel_4=new JPanel();
         panel_4.setBorder(UIManager.getBorder("TitledBorder.border"));
         panel_4.setLayout(new GridLayout(1, 0, 0, 0));
         panel_4.add(getAnimatedPanel_3());
      }
      return panel_4;
   }
}

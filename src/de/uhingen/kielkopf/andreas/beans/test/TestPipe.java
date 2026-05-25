/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.uhingen.kielkopf.andreas.beans.gui.AskPath;
import de.uhingen.kielkopf.andreas.beans.gui.FrameHelper;

import java.awt.BorderLayout;
import de.uhingen.kielkopf.andreas.beans.gui.JScrollList;
import de.uhingen.kielkopf.andreas.beans.gui.PipeInfo;
import de.uhingen.kielkopf.andreas.beans.shell.ErrorSink;
import de.uhingen.kielkopf.andreas.beans.shell.CountingPipe;

import javax.swing.JButton;

/**
 * @author Andreas Kielkopf
 *
 */
public class TestPipe {
   private JFrame              frame;
   private JPanel              panel;
   private JPanel              panel_1;
   private JPanel              panel_2;
   private JScrollList<String> panel_3;
   private JPanel              panel_4;
   private PipeInfo            panel_5;
   private CountingPipe        p5;
   private JButton             btnNewButton;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            TestPipe window=new TestPipe();
            window.frame.setVisible(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }
   /**
    * Create the application.
    */
   public TestPipe() {
      initialize();
   }
   /**
    * @throws IOException
    * 
    */
   private void test() {
      try {
         ProcessBuilder pb=new ProcessBuilder("cat", "/home/andreas/Bilder/P1100062.xcf");
         ProcessBuilder pc=new ProcessBuilder("wc", "-c");
         ErrorSink es=new ErrorSink();
         Process in=pb.start();
         Process out=pc.start();
         p5=new CountingPipe(in, out, es, 1024); // 1MB blocks
         getPipeInfo().watch(p5);
         Thread.startVirtualThread(() -> {
            Thread.currentThread().setName("TestPipe1");
            try (InputStream q=out.getInputStream()) {
               q.transferTo(System.out);
            } catch (IOException e) {
               e.printStackTrace();
            }
         });
         Thread.startVirtualThread(() -> {
            Thread.currentThread().setName("TestPipe2");
            try {
               while (es.errorQueue.take() instanceof String s)
                  System.err.println(s);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         });
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.NORTH);
      FrameHelper.restore(frame);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanel_1(), BorderLayout.NORTH);
         panel.add(getPanel_2(), BorderLayout.CENTER);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanel_4(), BorderLayout.NORTH);
         panel_1.add(getPipeInfo(), BorderLayout.SOUTH);
         panel_1.add(getBtnNewButton(), BorderLayout.WEST);
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
         panel_2.add(getPanel_3(), BorderLayout.CENTER);
      }
      return panel_2;
   }
   private JScrollList<String> getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JScrollList<>();
      }
      return panel_3;
   }
   private JPanel getPanel_4() {
      if (panel_4 == null) {
         panel_4=new AskPath();
      }
      return panel_4;
   }
   private PipeInfo getPipeInfo() {
      if (panel_5 == null) {
         panel_5=new PipeInfo();
      }
      return panel_5;
   }
   private JButton getBtnNewButton() {
      if (btnNewButton == null) {
         btnNewButton=new JButton("start");
         btnNewButton.addActionListener(_ -> test());
      }
      return btnNewButton;
   }
}

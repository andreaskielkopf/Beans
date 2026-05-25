/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

/**
 * @author Andreas Kielkopf
 *
 */
public class TestZeile {
   private JFrame frame;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               TestZeile window=new TestZeile(Arrays.asList(args));
               // window.frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   /**
    * Create the application.
    * 
    * @param list
    */
   public TestZeile(List<String> list) {
      initialize();
      Path p=Path.of(list.getFirst());
      for (String line:getSource(p)) {
         System.out.println(line);
      }
   }
   ArrayList<String> source=new ArrayList<>();
   private ArrayList<String> getSource(Path p) {
      if (source.isEmpty())
         try {
            source.addAll(Files.readAllLines(p, StandardCharsets.UTF_8));
         } catch (final IOException e) {
            System.err.println(e.toString());
            e.printStackTrace();
         }
      return source;
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }
}

/**
 *
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.EnumSet;

import javax.swing.JLabel;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author andreas
 *
 */
// @NonNullByDefault
public class Passer extends JLabel {
   public enum LINIE {
      LINKS, RECHTS, OBEN, UNTEN
   }
   static private final long    serialVersionUID=3873831417886360468L;
   private final EnumSet<LINIE> linien;
   /**
    * Create the panel.
    */
   public Passer() {
      this(LINIE.OBEN);
   }
   /**
    * @param linie1
    *           rest
    * @param rest
    */
   public Passer(LINIE linie1, LINIE... rest) {
      linien=EnumSet.of(linie1, rest);
      setPreferredSize(new Dimension(50, 50));
      setForeground(Color.RED);
      setFont(new Font("DejaVu Sans", Font.PLAIN, 14));
   }
   /**
    * @param string
    */
   public Passer(String string) {
      this(LINIE.OBEN);
      setText(string);
   }
   @Override
   public void paintComponent(@Nullable Graphics g2) {
      if (g2 == null)
         return;
      if (!isPaintingForPrint()) {
         super.paintComponent(g2);
         g2.setColor(getForeground());
         if (linien.contains(LINIE.OBEN))
            g2.drawLine(1, 0, getWidth() - 1, 0);
         if (linien.contains(LINIE.UNTEN))
            g2.drawLine(1, getHeight() - 1, getWidth() - 1, getHeight() - 1);
         if (linien.contains(LINIE.LINKS))
            g2.drawLine(1, 0, 1, getHeight());
         if (linien.contains(LINIE.RECHTS))
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
      }
   }
}

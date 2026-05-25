/**
 *
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * @author andreas
 *
 */
public class PaperPanel extends JPanel {
   static private final long serialVersionUID=7460300266942119303L;
   private final String      name;
   private int               x;
   private int               y;
   private final String      vers;
   public PaperPanel(String name1, String vers1) {
      setBorder(new LineBorder(new Color(0, 0, 0)));
      setSize(new Dimension(210, 297));
      setMinimumSize(new Dimension(210, 297));
      setBackground(Color.WHITE);
      setMaximumSize(new Dimension(210, 297));
      setPreferredSize(new Dimension(210, 297));
      name=name1;
      vers=vers1;
   }
   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      final Graphics2D      g2 =(Graphics2D) g;
      final AffineTransform tmp=g2.getTransform();
      g2.translate(getWidth() / 2d, getHeight() / 2d);
      g2.rotate(Math.PI / -2);
      final int X=x * ((getHeight() - 82 - 8) / -2);
      final int Y=y * ((getWidth() - 52 - 8) / 2);
      g2.translate(X, Y);
      g2.setColor(Color.YELLOW);
      g2.fillRect(-41, -26, 82, 52);
      g2.setColor(Color.BLACK);
      g2.drawRect(-41, -26, 82, 52);
      g2.scale(0.5d, 0.5d);
      g2.drawString(name, -55, 30);
      g2.drawString(vers, -30, 45);
      g2.setTransform(tmp);
   }
   /**
    * @param x1
    *           links=-1, mitte=0, rechts=1
    * @param y1
    *           oben=-1, mitte=0, unten=1
    */
   public void setPos(int x1, int y1) {
      x=(x1 == 0) ? 0 : (x1 > 0) ? 1 : -1;
      y=(y1 == 0) ? 0 : (y1 > 0) ? 1 : -1;
      repaint(1000);
   }
}

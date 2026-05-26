/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.backsnap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import de.uhingen.kielkopf.andreas.beans.data.SimplePair;

/**
 * Ein JPanel, das eine Reihe von Objekten ohne Layoutmanager darstellt.
 * 
 * Die Objekte werden durch einen JLabel repräsentiert. Sie können einen Text übergeben, und eine Farbe für Hintergrund und Text
 * 
 * @author Andreas Kielkopf
 * @param <T>
 *
 */
public class AnimatedPanel<T> extends JPanel {
   private static final long                                       serialVersionUID=-5558790745879049923L;
   /// JLabel zum eigentlichen Zeichnen
   private JLabel                                                  delegate;
   /// JLabel zum Berechnen der Größe
   private JLabel                                                  shadow;
   /// Liste der zu zeichnenden Objekte mit ihrer Position als int
   private final ConcurrentSkipListMap<T, SimplePair<Long, Point>> componenten     =new ConcurrentSkipListMap<>();
   /// Die Animation soll nur laufen wenn nötig, und nur einmalig
   private final AtomicBoolean                                     animationRunning=new AtomicBoolean(false);
   private int                                                     lh              =100;
   private final int                                               w               =500;
   private final int                                               h               =500;
   private final int                                               hgap            =5;
   private final int                                               vgap            =5;
   /// Bisherige Breite
   private int                                                     oWidth          =w;
   /// Breite des anzeigbaren Bereichs
   private int                                                     vWidth          =w;
   /**
    * Create the panel.
    */
   public AnimatedPanel() {
      init();
   }
   /**
   *
   */
   private void init() {
      setPreferredSize(new Dimension(w, h));
   }
   /**
    * Berechne die genaue Position für jedes Element anhand seiner Breite
    *
    * @param pwn
    * @return
    *
    */
   private void recalculateChildren() {
      var lpos=0L;
      if (getShadow() instanceof final JLabel lbl)
         for (final Entry<T, SimplePair<Long, Point>> e:componenten.entrySet())
            if (e.getKey() instanceof final T key) {
               lbl.setText(key.toString());
               var lWidth=lbl.getPreferredSize().width;
               final var r=(int) (lpos % vWidth);
               if (r + lWidth >= vWidth) {// Umbruch noch in diesem Label
                  lpos+=vWidth - r; // an den Anfang der nächsten Zeile
                  e.getValue().setA(lpos);
                  lpos+=lWidth + hgap;
               } else {
                  e.getValue().setA(lpos);
                  lpos=r + lWidth >= vWidth ? // Umbruch danach
                           vWidth * (lpos / vWidth + 1) : //
                           lpos + lWidth + hgap;
               }
            }
      animate();
   }
   /**
    * Berene die nächste animierte Position die gewünscht ist, und bewege das Objekt auch
    * 
    * @param value
    * @return wurde es bewegt ?
    */
   private boolean move(SimplePair<Long, Point> value) {
      final var lpos=value.getA();
      var changed=false;
      var x=value.getB().x;
      final var dx=speed((int) (lpos % vWidth) + hgap, x);
      if (dx != 0) {
         x+=dx;
         changed=true;
      }
      var y=value.getB().y;
      final var dy=speed((int) (lpos / vWidth * lh + vgap + 1), y);
      if (dy != 0) {
         y+=dy;
         changed=true;
      }
      if (changed)
         value.getB().setLocation(x, y);
      return changed;
   }
   /**
    * Berechne eine akzeptable Bewegungsrate
    * 
    * @param a
    * @param b
    * @return
    */
   private static int speed(int a, int b) {
      if (a == b)
         return 0;
      final var c=a > b;
      final var d=c ? a - b : b - a;
      final var e=32 - Integer.numberOfLeadingZeros(d);
      return d < 4 ? c ? 1 : -1 : c ? e * 2 : -e * 2; // Schritte je nach Abstand
   }
   /**
    * animiere die Labels in einem virtuellen Thread.
    * 
    * Aber nur einen Thread starten, und nur enden, wenn alles am Ziel ist
    */
   private void animate() {
      if (animationRunning.compareAndSet(false, true))
         Thread.startVirtualThread(() -> {
            try {
               Thread.currentThread().setName("Animate Objects");
               var count=0;
               do {
                  Thread.sleep(25);
                  count=componenten.size();
                  for (final Entry<T, SimplePair<Long, Point>> e:componenten.entrySet())
                     if (e.getValue() instanceof final SimplePair<Long, Point> value)
                        if (!move(value))
                           count--;
                  repaint(100);
               } while (count > 0);
            } catch (final InterruptedException e1) {
               System.err.println(e1);
            }
            animationRunning.set(false); // Jetzt darf ein neuer Thread gestartet werden
            repaint(1000);
         });
   }
   /**
    * Zeichne alle Objekte durch das Delegate an ihrer vorgesehen Position
    */
   @Override
   protected void paintChildren(Graphics g) {
      if (g instanceof final Graphics2D g2d) {
         final var j=getInsets();
         /// Änderungen der Breite führt zur Neuberechnung
         vWidth=getWidth() - hgap - hgap - j.left - j.right;
         if (oWidth != vWidth)
            recalculateChildren();
         oWidth=vWidth;// nur merken damit wir nicht ständig alles neu berechnen
         final var vHeight=getHeight() - vgap - vgap + j.top - j.bottom;
         final var d=getDelegate();
         var di=d.getPreferredSize();
         lh=di.height + hgap;
         for (final Entry<T, SimplePair<Long, Point>> e:componenten.entrySet()) {
            if (e.getKey() instanceof final T k) {
               d.setText(k.toString()); /// Text setzen
               if (k instanceof final hasColor kc) {
                  d.setForeground(kc.getForeground() instanceof final Color c ? c : Color.BLACK);
                  d.setBackground(kc.getBackground() instanceof final Color c ? c : Color.WHITE);
               }
               final var point=e.getValue().getB();
               final var x=point.x;
               final var y=point.y;
               di=d.getPreferredSize();
               if ((x > 0 && x + di.width < vWidth) && (y > 0 && y + di.height < vHeight)) {
                  SwingUtilities.paintComponent(g2d, d, this, x, y, di.width, di.height);
               }
            }
         }
      }
      super.paintChildren(g);
   }
   /**
    * Dieses JLabel wird zum Zeichnen verwendet
    * 
    * @return jLabel
    */
   public JLabel getDelegate() {
      if (delegate == null) {
         delegate=new JLabel("Delegate");
         delegate.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
         delegate.setFont(new Font("Noto Sans", Font.PLAIN, 15));
         delegate.setOpaque(true);
      }
      return delegate;
   }
   /**
    * Dieses JLabel wird zur Größenberechnung im Hintergrund verwendet
    * 
    * @return jLabel
    */
   public JLabel getShadow() {
      if (shadow == null) {
         shadow=new JLabel(getDelegate().getText());
         shadow.setBorder(getDelegate().getBorder());
         shadow.setFont(getDelegate().getFont());
      }
      return shadow;
   }
   /**
    * @param t
    *           Element to add
    */
   public void add(T t) {
      if (!componenten.containsKey(t)
               && (new SimplePair<>(1L, new Point(10, 10)) instanceof final SimplePair<Long, Point> pair)) {
         componenten.put(t, pair);
         recalculateChildren();
      }
   }
   /**
    * @param t
    *           Element to remove
    */
   public void delete(T t) {
      if (componenten.containsKey(t)) {
         componenten.remove(t);
         recalculateChildren();
      }
   }
}

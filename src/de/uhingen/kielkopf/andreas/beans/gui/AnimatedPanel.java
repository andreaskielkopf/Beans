/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import de.uhingen.kielkopf.andreas.beans.backsnap.hasColor;

/**
 * Ein JPanel, das eine Reihe von Objekten ohne Layoutmanager darstellt.
 * 
 * Die Objekte werden durch einen JLabel repräsentiert. Sie können einen Text übergeben, und eine Farbe für Hintergrund und Text
 * 
 * @author Andreas Kielkopf
 * @param <T>
 *           Objekte die Dargestellt werden
 *
 */
public class AnimatedPanel<T> extends JPanel {
   private static final long                                 serialVersionUID=6254515647851855442L;
   /// JLabel zum eigentlichen Zeichnen
   private JLabel                                            delegate;
   /// JLabel zum Berechnen der Größe
   private JLabel                                            shadow;
   /// Liste der zu zeichnenden Objekte mit ihrer Position als int
   private final ConcurrentSkipListMap<T, Pair<Long, Point>> componenten     =new ConcurrentSkipListMap<>();
   /// Währed der Animation zum löschen
   private final ConcurrentSkipListMap<T, Pair<Long, Point>> inDeletion      =new ConcurrentSkipListMap<>();
   private final LinkedBlockingQueue<Pair<Long, T>>          deleteQueue     =new LinkedBlockingQueue<>();
   /// Die Animation soll nur laufen wenn nötig, und nur einmalig
   private final AtomicBoolean                               animationRunning=new AtomicBoolean(false);
   private final AtomicBoolean                               deletionRunning =new AtomicBoolean(false);
   private int                                               lh              =100;
   private final int                                         w               =500;
   private final int                                         h               =500;
   private final int                                         hgap            =5;
   private final int                                         vgap            =5;
   /// Bisherige Breite
   private int                                               oWidth          =w;
   /// Breite des anzeigbaren Bereichs
   private int                                               vWidth          =w;
   private int                                               msAnimation     =25;
   private int                                               msDelete        =25000;
   private int                                               vHeight         =h;
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
    * Berechne die relative Position für jedes Element anhand seiner Breite
    *
    * @param pwn
    * @return
    *
    */
   private void recalculateChildren() {
      var lpos=0L;
      if (getShadow() instanceof JLabel) {
         for (final Entry<T, Pair<Long, Point>> e:componenten.entrySet())
            lpos=calculate(lpos, e);
         for (final Entry<T, Pair<Long, Point>> e:inDeletion.entrySet())
            lpos=calculate(lpos, e);
      }
      animate();
   }
   private long calculate(long lpos_, final Entry<T, Pair<Long, Point>> e) {
      long lp=lpos_;
      if (e.getKey() instanceof final T key) {
         getShadow().setText(key instanceof hasName hn ? hn.getName() : key.toString());
         var lWidth=getShadow().getPreferredSize().width;
         final var r=(int) (lp % vWidth);
         if (r + lWidth >= vWidth) {// Umbruch noch in diesem Label
            lp+=vWidth - r; // an den Anfang der nächsten Zeile
            e.getValue().setA(lp);
            lp+=lWidth + hgap;
         } else {
            e.getValue().setA(lp);
            lp=r + lWidth >= vWidth ? // Umbruch danach
                     vWidth * (lp / vWidth + 1) : //
                     lp + lWidth + hgap;
         }
      }
      return lp;
   }
   /**
    * Berechne die nächste animierte Position die gewünscht ist, und bewege das Objekt auch dorthin
    * 
    * @param value
    * @return wurde es bewegt ?
    */
   private boolean move(Pair<Long, Point> value) {
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
               Thread.currentThread().setName(getClass().getSimpleName()+" Animate");
               var count=0;
               do {
                  Thread.sleep(getMsAnimation());
                  count=componenten.size() + inDeletion.size();
                  for (final Entry<T, Pair<Long, Point>> e:componenten.entrySet())
                     if (e.getValue() instanceof final Pair<Long, Point> value)
                        if (!move(value))
                           count--;
                  for (final Entry<T, Pair<Long, Point>> e:inDeletion.entrySet())
                     if (e.getValue() instanceof final Pair<Long, Point> value)
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
         vHeight=getHeight() - vgap - vgap + j.top - j.bottom;
         final var d=getDelegate();
         var di=d.getPreferredSize();
         lh=di.height + hgap;
         for (final Entry<T, Pair<Long, Point>> e:componenten.entrySet())
            paintChild(g2d, e, false); // aktive Elemente
         for (final Entry<T, Pair<Long, Point>> e:inDeletion.entrySet())
            paintChild(g2d, e, true); // gelöschte Elemente invers
      }
      super.paintChildren(g);
   }
   /**
    * 
    * @param g2d
    * @param e
    * @param deleted
    */
   private void paintChild(Graphics2D g2d, final Entry<T, Pair<Long, Point>> e, final boolean deleted) {
      if (e.getKey() instanceof final T k) {
         JLabel d=getDelegate();
         d.setText(k instanceof hasName hn ? hn.getName() : k.toString()); /// Text setzen
         if (k instanceof final hasColor kc) {
            Color f=(kc.getForeground() instanceof final Color c ? c : Color.BLACK);
            Color b=(kc.getBackground() instanceof final Color c ? c : Color.WHITE);
            d.setForeground(deleted ? b : f);
            d.setBackground(deleted ? f : b);
         }
         final var point=e.getValue().getB();
         final var x=point.x;
         final var y=point.y;
         Dimension di=d.getPreferredSize();
         if ((x >= 0 && x + di.width <= vWidth) && (y >= 0 && y <= vHeight)) {
            SwingUtilities.paintComponent(g2d, d, this, x, y, di.width, di.height);
         }
      }
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
      if (!componenten.containsKey(t) && (new Pair<>(1L, new Point(10, 10)) instanceof final Pair<Long, Point> pair)) {
         componenten.put(t, pair);
         recalculateChildren();
      }
   }
   /**
    * @param t
    *           Element to remove
    */
   public void delete(T t) {
      if (t instanceof T && componenten.containsKey(t)) {
         inDeletion.put(t, componenten.remove(t));
         deleteQueue.offer(new Pair<Long, T>(System.currentTimeMillis() + getMsDelete(), t));
         recalculateChildren();
         if (deletionRunning.compareAndSet(false, true))
            Thread.startVirtualThread(() -> {
               Thread.currentThread().setName(getClass().getSimpleName()+" Deleter");
               try {
                  while (!deleteQueue.isEmpty()) {
                     AnimatedPanel<T>.Pair<Long, T> i=deleteQueue.take();
                     long a=i.a - System.currentTimeMillis();
                     if (a > 0)
                        Thread.sleep(a);
                     T b=i.b;
                     if (inDeletion.containsKey(b))
                        inDeletion.remove(b);
                     recalculateChildren();
                  }
               } catch (InterruptedException e) { /* */ }
               deletionRunning.set(false);
            });
      }
   }
   /**
    * @return msDelet
    */
   public int getMsDelete() {
      return msDelete;
   }
   /**
    * @param msDelete_
    *           Zeit nachdem das Objekt verschwindet
    */
   public void setMsDelete(int msDelete_) {
      this.msDelete=msDelete_;
   }
   /**
    * @return msAnimation
    */
   public int getMsAnimation() {
      return msAnimation;
   }
   /**
    * @param msAnimation_
    *           Zeit für jeden Bewegungsschritt
    */
   public void setMsAnimation(int msAnimation_) {
      this.msAnimation=msAnimation_;
   }
   /**
    * Das Objekt kann einen kurzen Namen für die Anzeige liefern
    * 
    * @author Andreas Kielkopf
    *
    */
   public interface hasName {
      public String getName();
   }

   /**
    * Enfaches Paar von 2 Objekten eines vorgegebenen Typs
    *
    * @author Andreas Kielkopf
    * @param <T1>
    *           a
    * @param <T2>
    *           b
    *
    */
   private class Pair<T1, T2> {
      @SuppressWarnings("null") private T1 a=null;
      @SuppressWarnings("null") private T2 b=null;
      Pair(T1 a_, T2 b_) {
         setA(a_);
         setB(b_);
      }
      T1 getA() {
         return a;
      }
      void setA(T1 a_) {
         if (a_ instanceof final T1 t1)
            a=t1;
      }
      T2 getB() {
         return b;
      }
      void setB(T2 b_) {
         if (b_ instanceof final T2 t2)
            b=t2;
      }
   }
}

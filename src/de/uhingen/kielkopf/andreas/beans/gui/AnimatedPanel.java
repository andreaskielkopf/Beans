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
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

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
   /// Die Animation soll nur laufen wenn nötig, und nur einmalig
   private final AtomicBoolean                               animationRunning=new AtomicBoolean(false);
   /// Liste der zu zeichnenden Objekte mit ihrer Position als int
   final ConcurrentSkipListMap<T, Pair<Long, Point>>         allItems        =new ConcurrentSkipListMap<>();
   /// JLabel zum eigentlichen Zeichnen
   private JLabel                                            delegate;
   private final LinkedBlockingQueue<Pair<Long, T>>          deleteQueue     =new LinkedBlockingQueue<>();
   ConcurrentSkipListSet<T>                                  selectedItems   =null;
   private final AtomicBoolean                               deletionRunning =new AtomicBoolean(false);
   private final int                                         h               =500;
   final int                                                 hgap            =5;
   /// Währed der Animation zum löschen
   private final ConcurrentSkipListMap<T, Pair<Long, Point>> deletedItems    =new ConcurrentSkipListMap<>();
   int                                                       rowHeight       =100;
   private int                                               msAnimation     =25;
   private int                                               msDelete        =25000;
   /// JLabel zum Berechnen der Größe
   private JLabel                                            shadow;
   final int                                                 vgap            =5;
   private int                                               vHeight         =h;
   private final int                                         w               =500;
   /// Bisherige Breite
   int                                                       oWidth          =w;
   /// Breite des anzeigbaren Bereichs
   int                                                       vWidth          =w;
   /**
    * Create the panel.
    */
   public AnimatedPanel() {
      init();
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
   class Pair<T1, T2> {
      @SuppressWarnings("null") private T1 a=null;
      @SuppressWarnings("null") private T2 b=null;
      Pair(T1 a_, T2 b_) {
         setA(a_);
         setB(b_);
      }
      T1 getA() {
         return a;
      }
      T2 getB() {
         return b;
      }
      void setA(T1 a_) {
         if (a_ instanceof final T1 t1)
            a=t1;
      }
      void setB(T2 b_) {
         if (b_ instanceof final T2 t2)
            b=t2;
      }
   }

   /**
    * Das Objekt kann einen kurzen Namen für die Anzeige liefern
    *
    * @author Andreas Kielkopf
    *
    */
   public interface hasName {
      /**
       * @return short Name of the Object
       */
      String getName();
   }

   /**
    * Das Objekt kann einen kurzen Namen für die Anzeige liefern
    *
    * @author Andreas Kielkopf
    *
    */
   public interface hasColors {
      /**
       * @return
       */
      Color getForeground(boolean isSelected);
      /**
       * @return
       */
      Color getBackground(boolean isSelected);
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
               Thread.currentThread().setName(getClass().getSimpleName() + " Animate");
               var count=0;
               do {
                  Thread.sleep(getMsAnimation());
                  count=allItems.size() + deletedItems.size();
                  for (final Entry<T, Pair<Long, Point>> e:allItems.entrySet())
                     if (e.getValue() instanceof final Pair<Long, Point> value)
                        if (!move(value))
                           count--;
                  for (final Entry<T, Pair<Long, Point>> e:deletedItems.entrySet())
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
   private long calculate(JLabel sdw2, long lpos_, final Entry<T, Pair<Long, Point>> e) {
      var lp=lpos_;
      if (e.getKey() instanceof final T key) {
         sdw2.setText(key instanceof final hasName hn ? hn.getName() : key.toString());
         final var lWidth=sdw2.getPreferredSize().width;
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
   *
   */
   private void init() {
      setPreferredSize(new Dimension(w, h));
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
      final var dy=speed((int) (lpos / vWidth * rowHeight + vgap + 1), y);
      if (dy != 0) {
         y+=dy;
         changed=true;
      }
      if (changed)
         value.getB().setLocation(x, y);
      return changed;
   }
   /**
    *
    * @param g2d
    * @param e
    * @param deleted
    */
   private void paintChild(Graphics2D g2d, final Entry<T, Pair<Long, Point>> e, final boolean deleted) {
      if (e.getKey() instanceof final T k) {
         final var d=getDelegate();
         d.setText(k instanceof final hasName hn ? hn.getName() : k.toString()); /// Text setzen
         if (k instanceof final hasColors kc) {
            final var s=selectedItems == null ? false : selectedItems.contains(k);
            final var f=kc.getForeground(s) instanceof final Color c ? c : Color.BLACK;
            final var b=kc.getBackground(s) instanceof final Color c ? c : Color.WHITE;
            d.setForeground(deleted ? b : f);
            d.setBackground(deleted ? f : b);
         }
         final var point=e.getValue().getB();
         final var x=point.x;
         final var y=point.y;
         final var di=d.getPreferredSize();
         if (x >= 0 && x + di.width <= vWidth && y >= 0 && y <= vHeight)
            SwingUtilities.paintComponent(g2d, d, this, x, y, di.width, di.height);
      }
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
      if (getShadow() instanceof final JLabel sdw)
         synchronized (sdw) {
            for (final Entry<T, Pair<Long, Point>> e:allItems.entrySet())
               lpos=calculate(sdw, lpos, e);
            for (final Entry<T, Pair<Long, Point>> e:deletedItems.entrySet())
               lpos=calculate(sdw, lpos, e);
         }
      animate();
   }
   /**
    * @param t
    *           Element to add
    */
   public void add(T t) {
      final var change=!allItems.containsKey(t);
      if (change && new Pair<>(1L, new Point(10, 10)) instanceof final Pair<Long, Point> pair) {
         allItems.put(t, pair);
         invalidate();
      }
   }
   public void addAll(Collection<T> values) {
      var changed=false;
      for (final T t:values)
         if (!allItems.containsKey(t) && new Pair<>(1L, new Point(10, 10)) instanceof final Pair<Long, Point> pair) {
            allItems.put(t, pair);
            changed=true;
         }
      if (changed)
         invalidate();
   }
   public Set<T> getAll() {
      return Collections.unmodifiableSet(allItems.keySet());
   }
   /**
    * @param t
    *           Element to remove
    */
   public void delete(T t) {
      if (t instanceof T && allItems.containsKey(t)) {
         deletedItems.put(t, allItems.remove(t));
         deleteQueue.offer(new Pair<>(System.currentTimeMillis() + getMsDelete(), t));
         invalidate();
         if (deletionRunning.compareAndSet(false, true))
            Thread.startVirtualThread(() -> {
               Thread.currentThread().setName(getClass().getSimpleName() + " Deleter");
               try {
                  while (!deleteQueue.isEmpty()) {
                     final var i=deleteQueue.take();
                     final var a=i.a - System.currentTimeMillis();
                     if (a > 0)
                        Thread.sleep(a);
                     if (deletedItems.containsKey(i.b))
                        deletedItems.remove(i.b);
                     if (selectedItems != null && selectedItems.contains(i.b))
                        selectedItems.remove(i.b);
                     invalidate();
                  }
               } catch (final InterruptedException _) { /* */ }
               deletionRunning.set(false);
            });
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
    * @return msAnimation
    */
   public int getMsAnimation() {
      return msAnimation;
   }
   /**
    * @return msDelet
    */
   public int getMsDelete() {
      return msDelete;
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
    * @param msAnimation_
    *           Zeit für jeden Bewegungsschritt
    */
   public void setMsAnimation(int msAnimation_) {
      msAnimation=msAnimation_;
   }
   /**
    * @param msDelete_
    *           Zeit nachdem das Objekt verschwindet
    */
   public void setMsDelete(int msDelete_) {
      msDelete=msDelete_;
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
         final var di=d.getPreferredSize();
         rowHeight=di.height + hgap;
         for (final Entry<T, Pair<Long, Point>> e:allItems.entrySet())
            paintChild(g2d, e, false); // aktive Elemente
         for (final Entry<T, Pair<Long, Point>> e:deletedItems.entrySet())
            paintChild(g2d, e, true); // gelöschte Elemente invers
      }
      // super.paintChildren(g);
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
   @Override
   public void invalidate() {
      oWidth=0; // Ändert die oWidth um ein recalculateChildren() beim neuzeichnen zu erzwingen
      super.invalidate();
   }
}

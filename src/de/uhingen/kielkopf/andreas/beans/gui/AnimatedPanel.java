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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

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
   private static final long                         serialVersionUID =6254515647851855442L;
   /// Die Animation soll nur laufen wenn nötig, und nur einmalig
   private final AtomicBoolean                       animationRunning =new AtomicBoolean(false);
   /// Liste der zu zeichnenden Objekte mit ihrer Position als int
   final ConcurrentSkipListMap<T, Pair<Long, Point>> allItems         =new ConcurrentSkipListMap<>();
   /// JLabel zum eigentlichen Zeichnen
   private JLabel                                    delegate;
   private final LinkedBlockingQueue<Pair<Long, T>>  deleteQueue      =new LinkedBlockingQueue<>();
   ConcurrentSkipListSet<T>                          selectedItems    =null;
   private final AtomicBoolean                       deletionRunning  =new AtomicBoolean(false);
   private final int                                 h                =500;
   final int                                         hgap             =5;                            // <->
   /// Währed der Animation zum löschen
   final ConcurrentSkipListMap<T, Pair<Long, Point>> deletedItems     =new ConcurrentSkipListMap<>();
   int                                               rowHeight        =100;
   private AtomicLong                                msAnimation      =new AtomicLong(25);
   private int                                       msDelete         =25000;
   /// JLabel zum Berechnen der Größe
   private JLabel                                    shadow;
   final int                                         vgap             =5;
   private int                                       vHeight          =h;
   private final int                                 w                =500;
   /// Bisherige Breite
   int                                               oWidth           =w;
   /// Breite des anzeigbaren Bereichs
   int                                               vWidth           =w;
   private JLabel                                    lblNewLabel;
   // public Boolean wrap_X =false;
   public Color                                      outsideBackground=null;                         // Color.lightGray;
   public Color                                      outsideForeground=null;                         // Color.darkGray;
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
      @SuppressWarnings("null") private T1 a=(T1) null;
      @SuppressWarnings("null") private T2 b=(T2) null;
      /**
       * @param a_
       *           1.Objekt
       * @param b_
       *           2.Objekt
       */
      Pair(T1 a_, T2 b_) {
         setA(a_);
         setB(b_);
      }
      /**
       * @return
       */
      T1 getA() {
         return a;
      }
      /**
       * @return
       */
      T2 getB() {
         return b;
      }
      /**
       * @param a_
       */
      void setA(T1 a_) {
         if (a_ instanceof final T1 t1)
            a=t1;
      }
      /**
       * @param b_
       */
      void setB(T2 b_) {
         if (b_ instanceof final T2 t2)
            b=t2;
      }
      @Override
      public String toString() {
         StringBuilder sb=new StringBuilder();
         sb.append(a).append(" ").append(b);
         return sb.toString();
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
       * @param isSelected
       *           boolean
       * @return Color für Schrift
       */
      Color getForeground(boolean isSelected);
      /**
       * @param isSelected
       *           boolean
       * @return Color für Hintergrund
       */
      Color getBackground(boolean isSelected);
   }
   /// animiere die Labels in einem virtuellen Thread.
   ///
   /// Aber nur einen Thread starten, und nur enden, wenn alles am Ziel ist
   /// * Entry<T, Pair<Long, Point>>
   /// * T=Objekt das bewegt werden soll
   /// * Long beschreibt die Zielposition
   /// * Point(x,y) ist die aktuelle Position
   private void animate() {
      if (animationRunning.compareAndSet(false, true))
         Thread.ofVirtual().name(getClass().getSimpleName() + " Animate").start(() -> {
            var count=0;// System.out.println("Animate started");
            do {
               LockSupport.parkNanos(msAnimation.get() * 1_000_000);
               count=0;
               for (final Entry<T, Pair<Long, Point>> e:allItems.entrySet())
                  if (e.getValue() instanceof final Pair<Long, Point> value && move(value))
                     count++;
               for (final Entry<T, Pair<Long, Point>> e:deletedItems.entrySet())
                  if (e.getValue() instanceof final Pair<Long, Point> value && move(value))
                     count++;
               if (count > 0)
                  repaint(getMsAnimation());
            } while (count > 0); // System.out.println("Animate stopped");
            animationRunning.set(false); // Jetzt darf ein neuer Thread gestartet werden
         });
   }
   /// berechne die Zielposition als long (aber nur ween sich etwas an den Daten geändert hat)
   ///
   /// * Entry<T, Pair<Long, Point>>
   /// * T=Objekt das bewegt werden soll
   /// * Long beschreibt die Zielposition
   /// * Point(x,y) ist die aktuelle Position
   private long calculate(JLabel sdw2, long pos_start, final Entry<T, Pair<Long, Point>> e) {
      var pos=pos_start;
      if (e.getKey() instanceof final T key) {
         // probeweise den Text setzen
         sdw2.setText(key instanceof final hasName k ? k.getName() : key.toString());
         // um die Breite zu erfahren
         final var lWidth=sdw2.getPreferredSize().width;
         final var rest=(int) (pos % vWidth);
         if (rest + lWidth >= vWidth) {// Umbruch noch in diesem Label
            pos+=vWidth - rest; // an den Anfang der nächsten Zeile
            e.getValue().setA(pos);
            pos+=lWidth + hgap;
         } else {
            e.getValue().setA(pos);
            pos=rest + lWidth >= vWidth ? // Umbruch danach
                     vWidth * (pos / vWidth + 1) : //
                     pos + lWidth + hgap;
         }
      }
      return pos;
   }
   /**
   *
   */
   private void init() {
      setPreferredSize(new Dimension(w, h));
      add(getLblNewLabel());
   }
   /**
    * Berechne die nächste animierte Position die gewünscht ist, und bewege das Objekt auch dorthin
    *
    * @param value
    * @return wurde es bewegt ?
    *
    *         /// * Long beschreibt die Zielposition /// * Point(x,y) ist die aktuelle Position
    */
   private boolean move(Pair<Long, Point> value) {
      long lpos=value.getA();
      final var p=value.getB();
      var spalte=(int) (lpos % vWidth + hgap);
      var ax=spalte - p.x;
      var zeile=(int) (lpos / vWidth * rowHeight + vgap);
      var ay=zeile - p.y;
      if (ax == 0 && ay == 0) // unbewegt
         return false;
      var vwh=vWidth * 7 / 16;// Abkürzung möglich?
      if (ax > vwh)
         ax-=vWidth;
      else
         if (ax < -vwh)
            ax+=vWidth;
      if (speed2(ax, ay) instanceof Point p2)
         p.translate(p2.x, p2.y);
      if (p.x < 0)
         p.translate(vWidth, 0);
      else
         if (p.x > vWidth)
            p.translate(-vWidth, 0);
      return true;
   }
   /**
    * Berechne eine akzeptable Bewegungsrate
    *
    * @param x
    * @param y
    * @return Point
    */
   private static Point speed2(int x, int y) {
      var px=x >= 0;
      var py=y >= 0;
      var ax=px ? x : -x;
      var ay=py ? y : -y;
      var s=ax + ay;
      if (s == 0)
         return new Point(0, 0);
      var e=32 - Integer.numberOfLeadingZeros(s);
      var ey=(int) ((ay + ay + 1L) * e / (s + s));
      var ex=e - ey;
      return new Point(px ? ex : -ex, py ? ey : -ey);
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
         if (y >= 0 && y <= vHeight) {
            if (x >= hgap && x + di.width <= hgap + vWidth) // ganz im Fenster
               SwingUtilities.paintComponent(g2d, d, this, x, y, di.width, di.height);
            else {
               if (outsideBackground instanceof Color bg)
                  d.setBackground(bg);
               if (outsideForeground instanceof Color fg)
                  d.setForeground(fg);
               if (x + di.width > hgap + vWidth && x < vWidth) // rechts aussen
                  SwingUtilities.paintComponent(g2d, d, this, x, y, di.width, di.height);
               if (x - vWidth + di.width >= hgap)// und linksaussen (geht gleichzeitig)
                  SwingUtilities.paintComponent(g2d, d, this, x - vWidth, y, di.width, di.height);
            }
         }
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
      Long lpos=0L;
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
      if (allItems.get(t) instanceof AnimatedPanel<T>.Pair<Long, Point> e) // Position beibehalten, aber T austauschen
         allItems.put(t, e);
      else
         if (new Pair<>(1L, new Point(hgap + vWidth, vgap)) instanceof final Pair<Long, Point> pair) {
            allItems.put(t, pair);
            invalidate();
         }
   }
   public void addAll(Collection<T> values) {
      var changed=false;
      for (final T t:values)
         if (!allItems.containsKey(t)
                  && new Pair<>(1L, new Point(hgap + vWidth, vgap)) instanceof final Pair<Long, Point> pair) {
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
            Thread.ofVirtual().name(getClass().getSimpleName() + " Deleter").start(() -> {
               try {
                  while (!deleteQueue.isEmpty()) {
                     final var i=deleteQueue.take();
                     final var a=i.a - System.currentTimeMillis();
                     if (a > 0)
                        LockSupport.parkNanos(a * 1_000_000);
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
   public void deleteAll() {
      for (T t:getAll())
         delete(t);
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
         delegate.setBorder(new EmptyBorder(0, 5, 0, 5));
         delegate.setOpaque(true);
      }
      return delegate;
   }
   /**
    * @return msAnimation
    */
   public long getMsAnimation() {
      return msAnimation.get();
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
      msAnimation.set(msAnimation_);
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
         rowHeight=di.height + vgap;
         for (final Entry<T, Pair<Long, Point>> e:allItems.entrySet())
            paintChild(g2d, e, false); // aktive Elemente
         for (final Entry<T, Pair<Long, Point>> e:deletedItems.entrySet())
            paintChild(g2d, e, true); // gelöschte Elemente invers
      }
      // super.paintChildren(g);
   }
   @Override
   public void invalidate() {
      oWidth=0; // Ändert die oWidth um ein recalculateChildren() beim neuzeichnen zu erzwingen
      super.invalidate();
   }
   /**
    * @author Andreas Kielkopf
    *
    */
   public interface AnimatedObject extends hasColors, hasName {}
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("New label");
         lblNewLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
      }
      return lblNewLabel;
   }
}

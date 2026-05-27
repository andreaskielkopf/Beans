/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Andreas Kielkopf
 *
 */
public class SelectableAnimatedPanel<T> extends AnimatedPanel<T> {
   private static final long serialVersionUID=-4599832467502738974L;
   public SelectableAnimatedPanel() {
      selectedItems=new ConcurrentSkipListSet<>();
      addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            mP(e);
            super.mousePressed(e);
         }
         private void mP(MouseEvent e) {
            e.translatePoint(-getInsets().left - hgap, -getInsets().top - vgap);
            final var p=e.getPoint();
            Entry<T, AnimatedPanel<T>.Pair<Long, Point>> treffer=null;
            for (final Entry<T, AnimatedPanel<T>.Pair<Long, Point>> entry:allItems.entrySet()) {
               final var b=entry.getValue().getB();
               if (p.y >= b.y && p.y <= b.y + rowHeight && p.x >= b.x)
                  if (!(treffer instanceof final Entry<T, AnimatedPanel<T>.Pair<Long, Point>> tr)
                           || tr.getValue() instanceof final Pair<Long, Point> tp
                                    && tp.getB() instanceof final Point tpb && b.x >= tpb.x)
                     treffer=entry;
            }
            if (treffer instanceof final Entry<T, AnimatedPanel<T>.Pair<Long, Point>> tr
                     && tr.getKey() instanceof final T key) {
               if (!selectedItems.add(key)) // hinzufügen oder entfernen
                  selectedItems.remove(key);
               repaint(100);
               e.consume();
               System.out.println(
                        (key instanceof final hasName hn ? hn.getName() : key.toString()) + " @ " + p.x + ":" + p.y);
            }
         }
      });
   }
   /**
    * Ergibt das Set mit allen selected Values
    *
    * @return
    */
   public NavigableSet<T> getSelectedValues() {
      return Collections.unmodifiableNavigableSet(selectedItems);
   }
   /**
    * Setzt das Set mit allen selected Values
    *
    * @return
    */
   public void setSelectedValues(Set<T> values) {
      selectedItems.clear();
      selectedItems.addAll(values);
   }
}

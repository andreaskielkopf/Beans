/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.util.Enumeration;

import javax.swing.DefaultListModel;

/**
 * @author Andreas Kielkopf
 *
 */
public class SortedListModel<T extends Comparable<T>> extends DefaultListModel<T> {
   private static final long serialVersionUID=-2346208274724734544L;
   @Override
   public void addElement(T element) {
      Enumeration<T> e=elements();
      int index=0;
      while (e.hasMoreElements()) {
         T t=(T) e.nextElement();
         if (t instanceof Comparable<T> ct) {
            if (ct.compareTo(element) > 0) {
               super.add(index, element);
               return;
            }
         }
         index++;
      }
      super.addElement(element);
   }
}

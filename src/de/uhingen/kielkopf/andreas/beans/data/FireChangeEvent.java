package de.uhingen.kielkopf.andreas.beans.data;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public interface FireChangeEvent {
   /**
    * <pre>
    * Muster: {@code}
    * private ChangeEvent changeEvent;
    * &#64;Override
    * public ChangeEvent getChangeEvent() {
    *    if (changeEvent == null)
    *       changeEvent=new ChangeEvent(this);
    *    return changeEvent;
    * }
    * </pre>
    * 
    * @return ChangeEvent(this)
    */
   abstract ChangeEvent getChangeEvent();
   /**
    * <pre>
    * Muster: {@code}
    * &#64;Override
    * public EventListenerList getListenerList() {
    *    return listenerList;
    * }
    * </pre>
    * 
    * @return EventListenerList
    */
   abstract EventListenerList getListenerList();
   default void addChangeListener(ChangeListener l) {
      if (l instanceof ChangeListener cl)
      getListenerList().add(ChangeListener.class, cl);
   }
   default void removeChangeListener(ChangeListener l) {
      if (l instanceof ChangeListener cl)
      getListenerList().remove(ChangeListener.class, cl);
   }
   // Ein Change-Event muß immer abgeschickt werden, wenn sich die Daten in der GUI geändert haben
   default void fireChangeEvent() {
      for (ChangeListener cl:getListenerList().getListeners(ChangeListener.class))
         cl.stateChanged(getChangeEvent());
   }
}

package de.uhingen.kielkopf.andreas.beans.data;

import java.io.IOException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public interface DatenSatz extends ChangeListener {
   boolean setWert(Key key, String value) throws IOException;
   String getWertOrDefault(Key key, String vorgabe) throws IOException;
   /** Der Wert in einem der GUI-Elemente hat sich aus irgendeinem Grund geändert */
   @Override
   default void stateChanged(ChangeEvent e) {
      if (e.getSource() instanceof DatenVerbindung dv)
         try {
            setWert(dv.getDataKey(), dv.getData());
         } catch (IOException ignore) { /* ignored */ }
   }
}

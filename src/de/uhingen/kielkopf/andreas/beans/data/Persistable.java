package de.uhingen.kielkopf.andreas.beans.data;

public interface Persistable extends FireChangeEvent {
   /**
    * setzt einen vom Titel abweichenden Key für die Persistenz
    * 
    * @param kennung
    */
   abstract void setPersistenceKey(Key key);
   /**
    * Füllt das Feld mit der Vorgabe dieser Persistenz(key) ohne eine Verbindung herzustellen
    * 
    * @param kennung
    */
   abstract void fromPreference(Key key);
   abstract String getTitle();
   /**
    * Verbindet dieses Feld mit dem im Titel angegebenen Key in der Persistenz Änderungen werden übertragen sobald das
    * Feld einen Changeevent schickt
    */
   default void setPersistenceKey() {
      String title=getTitle();
      if ((title != null) && !title.isEmpty())
         setPersistenceKey(() -> title);
   }
}

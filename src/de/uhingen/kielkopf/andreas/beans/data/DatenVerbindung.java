package de.uhingen.kielkopf.andreas.beans.data;

/**
 * Eine Verbindung zwischen einem Gui-Element und einem Datensatz
 * 
 * @author Andreas Kielkopf
 */
public interface DatenVerbindung extends FireChangeEvent {
   // private String key; // muß die Instanz zur Verfügung stellen
   abstract Key getDataKey();
   // abstract void setDataKey(Key key);
   // Dieser Wert wird in der GUI dargestellt
   abstract String getData();
   abstract void setData(String text);
   /**
    * <pre>
    * Muster: {@code}
    * private DatenSatz          datenSatz;
    * &#64;Override
    * public DatenSatz getVerbindung() {
    *    return datenSatz;
    * }
    * </pre>
    * 
    * @return
    */
   abstract DatenSatz getVerbindung();
   /**
    * <pre>
    * Muster: {@code}
    * private DatenSatz          datenSatz;
    * &#64;Override
    * public void setVerbindung(DatenSatz verbindung) {
    *    datenSatz=verbindung;
    * }
    * </pre>
    * 
    * @param verbindung
    */
   abstract void setVerbindung(DatenSatz verbindung);
   /* Legt den Datensatz als Changelistener an */
   default void setDatenSatz(DatenSatz ds) {
      removeChangeListener(getVerbindung());
      setVerbindung(ds);
      addChangeListener(getVerbindung());
   }
}

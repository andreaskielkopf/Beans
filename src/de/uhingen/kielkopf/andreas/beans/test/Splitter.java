/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.util.List;

/**
 * @author Andreas Kielkopf
 *
 */
public interface Splitter {
   public String getKey();
   /**
    * @param zeile
    * @return
    */
   Object getValue(List<String> zeile);
}

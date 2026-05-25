/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.Color;

import javax.swing.UIManager;

/**
 * @author Andreas Kielkopf
 *
 */
public interface hasBackground {
   /**
    * @param isSelected
    * @return
    */
   default public Color getBackground(boolean isSelected) {
      return isSelected ? UIManager.getColor("List.background") : UIManager.getColor("List.selectionForeground");
   }
}

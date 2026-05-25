/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.test;

import static de.uhingen.kielkopf.andreas.beans.test.SplitTyp.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andreas Kielkopf
 *
 */
public enum btrfsTexte implements Splitter {
   ID(INT_),
   gen(INT_),
   cgen(INT_),
   parent(INT_),
   top$level(INT_),
   otime(OTIME_),
   parent_uuid(UUID_),
   received_uuid(UUID_),
   uuid(UUID_),
   path(PATH_);
   private SplitTyp typ;
   private String   key;
   private int      offset;
   /**
    * @param i
    */
   btrfsTexte(SplitTyp st) {
      typ=st;
      offset=(int) (1 + name().chars().filter(c -> c == 'b').count());
      key=name().replaceAll("$", " ");
   }
   @Override
   public String getKey() {
      return key;
   }
   @Override
   public Object getValue(List<String> zeile) {
      int pos=-1;
      if (offset == 1) {
         pos=zeile.indexOf(getKey());
      } else {
         String[] keys=key.split(" ");
         List<String> kl=List.of(keys);
         pos=Collections.indexOfSubList(zeile, kl);
      }
      if ((pos == -1) || (pos + offset > zeile.size()))
         return "";
      return typ.extract(zeile.subList(pos, pos + offset - 1));
   }
   static public Map<String, Object> getMap(String zeile) {
      HashMap<String, Object> map=new HashMap<>();
      List<String> sl=List.of(zeile.split(" "));
      for (btrfsTexte b:btrfsTexte.values())
         if (b.getValue(sl) instanceof Object o)
            map.put(b.getKey(), o);
      return map;
   }
}

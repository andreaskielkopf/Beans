/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.data;

import java.lang.ref.SoftReference;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Andreas Kielkopf
 */
public class Link<T> {
   private SoftReference<T> sr=null;
   final private String     name;
   // private @NonNull Class<? extends @NonNull Object> clazz;
   public Link(String n) {
      this(n, null);
   }
   public Link(String n, @Nullable T t) {
      name=n;
      set(t);
   }
   public @Nullable T get() {
      if (sr == null)
         return null;
      return sr.get();
   }
   public @Nullable T set(@Nullable T t) {
      sr=(t == null) ? null : new SoftReference<T>(t);
      return t;
   }
   public void clear() {
      sr=null;
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder(name).append("[");
      if (get() instanceof T g)
         sb.append(g.toString());
      else
         sb.append("null");
      sb.append("]");
      return sb.toString();
   }
}

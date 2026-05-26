/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.data;

/**
 * Enfaches Paar von 2 Objekten eines vorgegebenen Typs
 *
 * @author Andreas Kielkopf
 * @param <T1>
 *           a
 * @param <T2>
 *           b
 *
 */
public class SimplePair<T1, T2> {
   @SuppressWarnings("null") private T1 a=null;
   @SuppressWarnings("null") private T2 b=null;
   /**
    * @param a_
    *           T1
    * @param b_
    *           T2
    *
    */
   public SimplePair(T1 a_, T2 b_) {
      setA(a_);
      setB(b_);
   }
   /**
    * @return T1
    */
   public T1 getA() {
      return a;
   }
   /**
    * @param a_
    *           T1
    *
    */
   public void setA(T1 a_) {
      if (a_ instanceof final T1 t1)
         a=t1;
   }
   /**
    * @return T2
    */
   public T2 getB() {
      return b;
   }
   /**
    * @param b_
    *           T2
    *
    */
   public void setB(T2 b_) {
      if (b_ instanceof final T2 t2)
         b=t2;
   }
}

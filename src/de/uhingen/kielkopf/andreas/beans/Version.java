package de.uhingen.kielkopf.andreas.beans;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Version mit automatischer decodierung des Versionstextes
 * 
 * @author Andreas Kielkopf
 * 
 * @param name
 *           Name zur suche nach der Version
 * @param version
 *           kompletter Versionstext
 * @param major
 *           vor dem Punkt
 * @param minor
 *           nach dem Punkt
 * @param patch
 *           sonstiges
 */
public record Version(String name, String version, String major, String minor, String patch) {
   private static final Pattern VERSION=Pattern.compile("[^0-9]*([-0-9.]+)");
   private static final Pattern MAYOR=Pattern.compile("[^0-9]*([0-9]+)");
   private static final Pattern MINOR=Pattern.compile("[^0-9]*[0-9]+[.]([0-9]+)");
   private static final Pattern PATCH=Pattern.compile("[^0-9]*[0-9]+[.][0-9]+[.]([0-9]+)");
   static private Version java;
   static private ExecutorService vx;
   private static ExecutorService rx;
   /** @return version of java */
   static public final Version getJava() {
      synchronized (VERSION) {
         if (java == null)
            java=new Version("java", System.getProperty("java.version"));
      }
      return java;
   }
   /** @return virtual executor (wenn möglich) */
   static public final ExecutorService getVx() {
      if (vx == null)
         synchronized (VERSION) {
            if (getJava().getMayor() > 20)
               vx=Executors.newVirtualThreadPerTaskExecutor();
            else
               vx=getRx();
         }
      return vx;
   }
   /** @return real Executor (Work stealing) */
   static public final synchronized ExecutorService getRx() {
      if (rx == null)
         synchronized (VERSION) {
            rx=Executors.newWorkStealingPool();
         }
      return rx;
   }
   /** @return Name des Executors */
   @SuppressWarnings("resource")
   static public final String getVxText() {
      return "using " + getVx().getClass().getSimpleName();
   }
   /**
    * Erzeuge eine Version mit dem gegebenen Namen
    * 
    * @param name
    *           Name der Version
    * @param t
    *           text der Version
    */
   public Version(String name, String t) {
      this(name, RecordParser.getString(VERSION.matcher(t)), RecordParser.getString(MAYOR.matcher(t)),
               RecordParser.getString(MINOR.matcher(t)), RecordParser.getString(PATCH.matcher(t)));
   }
   /** @return mayor */
   public int getMayor() {
      return Integer.parseInt(major);
   }
   /** @return minor */
   public int getMinor() {
      return Integer.parseInt(minor);
   }
   /** @return patchlevel */
   public int getPatch() {
      return Integer.parseInt(patch);
   }
   /** @return major.minor */
   public float getVersion() {
      try {
         return Float.parseFloat(version);
      } catch (NumberFormatException e) {
         return Float.parseFloat(Integer.toString(getMayor()) + "." + Integer.toString(getMinor()));
      }
   }
   public String toShortString() {
      StringBuilder builder=new StringBuilder();
      if (name == null)
         builder.append("Version ");
      else
         builder.append(name).append(" ");
      builder.append(version);
      return builder.toString();
   }
   @Override
   public String toString() {
      StringBuilder builder=new StringBuilder();
      if (name == null)
         builder.append("Version [");
      else
         builder.append(name).append(" [");
      builder.append("version=").append(version);
      builder.append(", major=").append(major);
      builder.append(", minor=").append(minor);
      builder.append(", patch=").append(patch);
      builder.append("]");
      return builder.toString();
   }
}

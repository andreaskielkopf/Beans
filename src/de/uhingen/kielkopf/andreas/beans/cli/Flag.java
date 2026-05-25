package de.uhingen.kielkopf.andreas.beans.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Framework zur Handhabung von CLI-Flags unter linux (needs java 21)
 * 
 * <pre>
 *    Speichere Argumente die beim Programmstart übergeben werden und ermögliche die Analyse "on demand"
 * 
 *       * Flags beginnend mit - oder -! 
 *       * Gruppen von Flags beginnend mit - 
 *       * Flags beginnend mit -- (langform) 
 *       * Parameter für jedes Flag durch = vom Flag getrennt
 *       * Argumente durch Leerzeichen getrennt
 * </pre>
 * 
 * @author Andreas Kielkopf
 * @since 2025 06 01
 */
public class Flag {
   /** Liste aller definierten Flags */
   @NonNull static private List<Flag> flagList     =new CopyOnWriteArrayList<>();
   /** Die beim Programmstart übergebenen Flags, Parameter und Argumente */
   @NonNull static private String     args         =" ";
   /** Die beim Programmstart übergebenen Argumente */
   static private List<String>        argumentListe=null;
   /** Anzahl der Zeichen für den langen Namen der Flags reservieren */
   static private Integer             spalten      =null;
   /** Kurzname des Flag (Ein Buchstabe) -x oder -!x */
   @Nullable final private Character  kurz;
   /** Langname des Flag (Text ohne Leerzeichen oder Sonderzeichen) --extra oder --!extra */
   @Nullable final private String     lang;
   /** Parameter (sofern vorhanden) -x=hallo oder --extra=hallo --extra=200 */
   @Nullable private String           param;
   /** Usage-Text für dieses Flag */
   @Nullable final private String     usage;
   /** Ist dieses Flag vorhanden und gesetzt */
   private Boolean                    flag         =null;
   /** Defaultwert für dieses Flag */
   private boolean                    standard     =false;
   /** Flag nur mit langer Bezeichnung */
   // public Flag(String lang1, String usage1) {
   // this(null, lang1, null, usage1);
   // }
   /** Flag nur mit kurzer Bezeichnung */
   // public Flag(char kurz1, String usage1) {
   // this(Character.valueOf(kurz1), null, null, usage1);
   // }
   /**
    * Flag mit langer und kurzer Bezeichnung und Hilfe-text
    * 
    * @param kurz1
    *           Kurzname des Flag
    * @param lang1
    *           Langname des Flag
    * @param usage1
    *           Text der bei usage angezeigt werden soll
    */
   public Flag(char kurz1, String lang1, String usage1) {
      this(Character.valueOf(kurz1), lang1, null, usage1);
   }
   /**
    * Flag mit langer und kurzer Bezeichnung, Hilfe-Text und default Parameter
    * 
    * @param kurz1
    *           Kurzname des Flag
    * @param lang1
    *           Langname des Flag
    * @param parameter1
    *           default-parameter des Flag
    * @param usage1
    *           Text der bei usage angezeigt werden soll
    */
   public Flag(char kurz1, String lang1, String usage1, String parameter1) {
      this(Character.valueOf(kurz1), lang1, parameter1, usage1);
   }
   /**
    * Interner Konstruktor für ein Flag
    * 
    * @param kurz1
    *           Kurzname des Flag
    * @param lang1
    *           Langname des Flag
    * @param parameter1
    *           default-parameter des Flag
    * @param usage1
    *           Text der bei usage angezeigt werden soll
    */
   private Flag(@Nullable Character kurz1, @Nullable String lang1, @Nullable String parameter1,
            @Nullable String usage1) {
      kurz=(kurz1 instanceof Character k && Character.isLetter(k)) ? k : null;
      lang=(lang1 instanceof String l && l.matches("[a-zA-Z_]+")) ? l : null;
      if (kurz == null && lang == null)
         throw new IllegalArgumentException("Flag needs to be [a-zA-Z]*");
      usage=(usage1 instanceof String u && !u.isBlank()) ? u : "";
      param=parameter1;
      flagList.add(this);
   }
   /**
    * Übergabe der Argumente an alle Flags
    * 
    * @param args1
    *           vom System beim Programmstart übergeben
    * @param standard_args
    *           (diese gelten, wenn keine anderen übergeben wurden)
    */
   @SuppressWarnings("null")
   public static void setArgs(String[] args1, String standard_args) {
      synchronized (args) {
         if (standard_args instanceof String s && !s.isBlank())
            args=s;
         if (args1 instanceof String[] a && a.length > 0)
            args=String.join(" ", a);
         args=" " + args + " ";
         argumentListe=null;
         for (Flag flag:flagList) {
            flag.param=null;
            flag.flag=null;
            flag.standard=false;
         }
      }
      // hier erfolgt keine Auswertung (lazy)
   }
   /**
    * Gibt die Argumente zurück, die momentan gelten
    * 
    * @return args
    */
   public static String getArgs() {
      return args;
   }
   /**
    * Auswertung dieses Flags (lazy)
    *
    * @return boolean ist dieses Flag gesetzt
    */
   public boolean get() {
      if (flag == null) { // noch nicht ausgewertet
         flag=false;
         /// @todo !(not) unterstützen
         if (lang instanceof String l) { // suche langform --extra
            /// @todo relpaceAll unnötig ?
            flag|=args.matches(".* --" + l.toLowerCase().replaceAll("_", "-") + " .*");
            /// @todo langform mit parameter noch nicht unterstützt
         }
         if (kurz instanceof Character k) { // suche kurzform (einzeln oder gruppiert) -x
            flag|=args.matches(".* -[a-z]*" + k + "[a-z]* .*");
            // suche kurzform mit parameter (einzeln oder gruppiert) -x=hallo
            flag|=args.matches(".* -[a-z]" + k + "=.* ");
         }
         flag|=standard;
         if (kurz instanceof Character k) { // suche !kurzform (einzeln oder gruppiert) -!x
            flag&=!args.matches(".* -[a-z]*" + "!" + k + "[a-z]* .*");
         }
      }
      return flag;
   }
   /**
    * Auswertung des Parameters zu diesem Flag (lazy)
    *
    * @return parameter
    */
   public String getParameter() {
      if (param == null) {
         if (kurz instanceof Character k) {// suche kurzform mit parameter
            final Matcher ma=Pattern.compile(" -" + k + "=([^- =]+)").matcher(args);
            if (ma.find()) // ersetze den bisherigen parameter
               param=ma.group(1);
         }
         if (lang instanceof String l) { // suche langform mit parameter
            final Matcher ma2=Pattern.compile(" --" + l.toLowerCase().replaceAll("_", "-") + "=([^- =]+)")
                     .matcher(args);
            if (ma2.find())// ersetze den bisherigen parameter
               param=ma2.group(1);
         }
      }
      return param;
   }
   /**
    * Holt den Parameter oder einen vorgegebenen Default
    * 
    * Wenn der Ersatz ein Integer ist, wird der Parameter als Integer zurückgegeben
    * 
    * @param ersatz
    *           defaultwert (String oder Integer)
    * @return zum Flag gehörender Parameter (String oder Integer)
    */
   public Object getParameterOrDefault(Object ersatz) {
      if (getParameter() instanceof String par && !par.isBlank()) {
         /// @todo switch case über den Typ
         if (ersatz instanceof Integer i)
            try {
               return Integer.decode(par);
            } catch (Exception ignore) { /* Dann wars eben kein Integer */
               return i;
            }
         return par;
      }
      return ersatz;
   }
   /**
    * Holt sonstige Argumente aus der Commandline, die nicht zu den Flags gehören
    * 
    * @param index
    *           des gewünschten Arguments (0-based)
    * @return Argument auf der Kommandozeile (String)
    */
   public static String getArgument(int index) {
      return getArgumentOrDefault(index, "");
   }
   /**
    * Holt sonstige Argumente aus der Commandline (mit Default)
    * 
    * @param index
    *           des gewünschten Arguments (0-based)
    * @param standard
    *           Default Argument, wenn der Index leer ist
    * @return Argument auf der Kommandozeile oder Default (String)
    */
   public static String getArgumentOrDefault(int index, String standard) {
      if (argumentListe == null)
         getArgumentList();
      return (index > argumentListe.size()) ? standard : argumentListe.get(index);
   }
   /**
    * Bilde die {@link argumentListe} aus den {@link args}.
    * 
    * Dabei wird zwischen {@link Flag} und parametern unterschieden Flags beginnen mit ' -' oder ' !' und parameter mit was anderem
    * 
    * @return Liste mit zusätzlichen Argumenten (ohne Flags und Parameter)
    */
   public static List<String> getArgumentList() {
      synchronized (args) {
         if (argumentListe == null) {
            var al=new ArrayList<String>();
            /**
             * Argument getrennt durch Leerzeichen und startet nicht mit einem - oder ! oder -!
             */
            final Matcher ma3=Pattern.compile(" [^ -!][^ ]*").matcher(args);
            while (ma3.find())
               al.add(ma3.group().trim());
            argumentListe=al;
         }
      }
      return new ArrayList<>(argumentListe); // gib dein Original nicht aus der Hand
   }
   /**
    * Manuelles setzen oder löschen von Flags durch das Programm
    *
    * @param b1
    *           zustand für das Flag
    * 
    */
   public void set(boolean b1) {
      flag=b1;
   }
   /**
    * Manuelles setzen oder löschen des defaultwertes für ein Flag
    * 
    * Wenn das Flag nicht explizit in den [args] vorkommt wird dieser Status angenommen
    *
    * @param b1
    *           defaultwert für das Flag
    * @return Flag
    */
   synchronized public Flag setDefault(boolean b1) {
      synchronized (args) {
         standard=b1;
      }
      return this;
   }
   /**
    * Main um einige Tests zu fahren
    * 
    * @param argumente
    *           Argumente von der Commandline
    */
   public static void main(String[] argumente) {
      Flag.setArgs(argumente, "-a test test1 -c /home /usr/local/bin --zweihundert=200 -f=16");
      for (String p:getArgumentList())
         System.out.println(p);
   }
   /**
    * Manuelles setzen des Paramters zu diesem Flag
    * 
    * @param p
    *           Setzt den Parameter für dieses Flag
    */
   public void setParameter(String p) {
      param=p;
   }
   // static public final int parseIntOrDefault(String s, int def) {
   // if (s != null)
   // try {
   // return Integer.parseInt(s);
   // } catch (NumberFormatException ignore) {
   // System.err.println(ignore.getMessage() + ":" + s);
   // }
   // return def;
   // }
   /**
    * Gibt eine Liste der Hilfetexte für die übergebenen Flags zurück
    * 
    * @param filter
    *           Liste der Flags die übergeben werden sollen
    * @return Liste mit Hilfetexten zu diesen Flags
    */
   static public ArrayList<String> getUsage(Flag... filter) {
      var usag=new ArrayList<>(List.of(" ", "Usage:", "------"));
      for (Flag flag:(filter instanceof Flag[] f && f.length > 0) ? Arrays.asList(f) : flagList)
         usag.add(flag.getHilfe());
      return usag;
   }
   /**
    * Gibt eine Hilfe-Zeile für dieses Flag zurück
    * 
    * @return Hilfezeile für dieses Flag
    */
   public String getHilfe() {
      StringBuilder zeile=new StringBuilder(" * ");
      zeile.append((kurz == null) ? "  " : "-" + kurz).append(" ");
      if (breite() > 0) {
         if (lang instanceof String l && !l.isBlank())
            zeile.append("--").append(l).append(" ".repeat(breite() - l.length() + 1));
         else
            zeile.append(" ".repeat(breite() + 3));
      }
      if (usage instanceof String u && !u.isBlank())
         zeile.append(u).append(" ");
      if (param instanceof String p && !p.isBlank())
         zeile.append("[=").append(p).append("] ");
      return zeile.toString();// .stripTrailing();
   }
   /** ermittle wie breit die Spalte für lange flags in den Hilfezeilen sein muß */
   static private int breite() {
      if (spalten == null) {
         int s=0;
         for (Flag flag:flagList)
            if (flag.lang instanceof String l)
               s=Math.max(s, l.length());
         spalten=s;
      }
      return spalten;
   }
   @Override
   public String toString() {
      return new StringBuilder("Flag[")//
               .append(kurz).append(" ")//
               .append(lang).append(" ")//
               .append(param).append(" ")//
               .append("]").toString();
   }
}

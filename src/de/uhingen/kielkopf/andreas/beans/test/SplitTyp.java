/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public enum SplitTyp {
   INT_(Integer.class), LONG_(Long.class), OTIME_(Instant.class), UUID_(String.class), PATH_(String.class);
   private final Class<@NonNull ?> c;
   /**
    * @param class1
    */
   SplitTyp(Class<@NonNull ?> class1) {
      c=class1;
   }
   /**
    * @param zeile
    * @param pos
    * @return
    */
   public Object extract(List<String> list) {
      try {
         String s=(list.size() == 1) ? list.getFirst() : String.join(" ", list);
         return switch (this) {
            case INT_ -> Integer.parseInt(s);
            case LONG_ -> Long.parseLong(s);
            case OTIME_ -> {
               LocalDateTime ldt=LocalDateTime.parse(s, dtfmt);
               yield ldt.atZone(ZoneId.systemDefault()).toInstant();
            }
            case UUID_ -> UUID.fromString(s);
            case PATH_ -> s;
            default -> null;
         };
      } catch (NumberFormatException e) {
         e.printStackTrace();
         return null;
      }
   }
   String expand(Object o) {
      return switch (o) {
         case final String s -> s;
         case final Integer i when c == Integer.class -> Integer.toUnsignedString(i);
         case final Long l when c == Long.class -> Long.toUnsignedString(l);
         case final UUID u when c == UUID.class -> u.toString();
         case final Instant n when c == Instant.class -> {
            final var ldt=LocalDateTime.ofInstant(n, ZoneId.systemDefault());
            yield ldt.format(dtfmt);
         }
         default -> o.toString();
      };
   }
   public static final DateTimeFormatter dtfmt=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
   public static final Pattern           uuid0=Pattern.compile("[-0-9a-f]{36}", Pattern.CASE_INSENSITIVE);
   public static final Pattern           uuid1=Pattern.compile("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}",
            Pattern.CASE_INSENSITIVE);
}

package de.uhingen.kielkopf.andreas.beans;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;

/**
 * IntegerView ist ein JSpinner mit einem Text davor und einer Einheit dahinter.
 * 
 * @author andreas
 * @since 20250620 verbesserte Kapselung
 */
public class IntegerView extends JPanel {
   static private final long serialVersionUID=-3088026069803197676L;
   private JLabel            jlbEinheit;
   private JLabel            jlbText;
   private JSpinner          spinner;
   /**
    * Create the panel.
    */
   public IntegerView() {
      initGUI();
   }
   /**
    * @param text
    *           für den Label
    * @param einheit
    *           für das Value
    */
   public IntegerView(@NonNull String text, @NonNull String einheit) {
      getLabelText().setText(text);
      getLabelEinheit().setText(einheit);
      initGUI();
   }
   /**
    * Delegiert zu spinner
    * 
    * @param listener
    *           hinzufügen
    */
   public void addChangeListener(ChangeListener listener) {
      spinner.addChangeListener(listener);
   }
   /**
    * @return text der Einheit
    */
   @NonNull
   public final String getEinheit() {
      final String einheit=getLabelEinheit().getText();
      return (einheit == null) ? "" : einheit;
   }
   /**
    * Delegiert zu spinner
    * 
    * @return model
    */
   public SpinnerModel getModel() {
      return spinner.getModel();
   }
   /**
    * @return text des Labels
    */
   @NonNull
   public final String getText() {
      final String text=getLabelText().getText();
      return (text == null) ? "" : text;
   }
   /**
    * Value des Spinners
    * 
    * @return value
    */
   @NonNull
   public Object getValue() {
      final Object o=getSpinner().getValue();
      return (o == null) ? 0 : o;
   }
   /**
    * Delegiert zu spinner
    * 
    * @param listener
    *           entfernen
    */
   public void removeChangeListener(ChangeListener listener) {
      spinner.removeChangeListener(listener);
   }
   /**
    * @param text
    *           als Einheit für das Value
    */
   public final void setEinheit(@NonNull String text) {
      getLabelEinheit().setText(text);
      invalidate();
   }
   /**
    * Delegiert zu spinner
    * 
    * @param model
    *           setzen
    */
   public void setModel(SpinnerModel model) {
      spinner.setModel(model);
   }
   /**
    * @param text
    *           als Beschreibung des Spinners
    */
   public final void setText(@NonNull String text) {
      getLabelText().setText(text);
      invalidate();
   }
   /**
    * @param value
    *           einstellen
    */
   public void setValue(@NonNull Object value) {
      getSpinner().setValue(value);
   }
   @SuppressWarnings("null")
   @NonNull
   private JLabel getLabelEinheit() {
      if (jlbEinheit == null)
         jlbEinheit=new JLabel("Einheit");
      return jlbEinheit;
   }
   /**
    * @return jLabel lazy
    */
   @SuppressWarnings("null")
   @NonNull
   private JLabel getLabelText() {
      if (jlbText == null) {
         jlbText=new JLabel("MusterText");
         jlbText.setHorizontalAlignment(SwingConstants.TRAILING);
         jlbText.setLabelFor(getSpinner());
      }
      return jlbText;
   }
   @SuppressWarnings("null")
   @NonNull
   private JSpinner getSpinner() {
      if (spinner == null) {
         spinner=new JSpinner();
         spinner.setModel(new SpinnerNumberModel(0, null, null, 1));
      }
      return spinner;
   }
   private void initGUI() {
      setMaximumSize(new Dimension(200, 50));
      setLayout(new BorderLayout());
      add(getLabelText(), BorderLayout.WEST);
      add(getSpinner(), BorderLayout.CENTER);
      add(getLabelEinheit(), BorderLayout.EAST);
   }
}

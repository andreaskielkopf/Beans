/**
 *
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author andreas
 *
 */
public class Fontauswahl extends JPanel implements PropertyChangeListener {
   static private final long                             serialVersionUID=-1221877930226284285L;
   static private final String                           TEXT_ACTION     ="Action ";
   static private final String                           TAG_FONTFAMILY  ="FONTFAMILY";
   static private final String                           TAG_FONTSTRECH  ="FONTSTRECH";
   static private final String                           TAG_FONTSIZE    ="FONTSIZE";
   private JComboBox<String>                             comboBoxFamily;
   private JSpinner                                      spinnerSize;
   private JComboBox<STYLE>                              comboBoxStyle;
   private @NonNull String                               fontFamily      ="DejaVu Sans";
   private float                                         fontSize        =18;
   private STYLE                                         fontStyle       =STYLE.Normal;
   private Font                                          fontFont;
   private JLabel                                        Mustertext;
   private JLabel                                        lblFamily;
   private JLabel                                        lblSize;
   private JLabel                                        lblStyle;
   private JPanel                                        panel;
   private Preferences                                   preferences;
   // @Override
   /*
    * public void setFont(Font font) { if (font == null) return; fontFont = font; fontFamily = font.getFamily();
    * getComboBoxFamily().setSelectedItem(fontFamily); fontSize = font.getSize2D(); getSpinnerSize().setValue(fontSize); fontStyle = font.getStyle();
    * getComboBoxStyle().setSelectedItem(fontStyle); getMustertext().setFont(font); fireActionPerformed(new ActionEvent(this,
    * ActionEvent.ACTION_PERFORMED, "Font changed")); }
    */
   private transient CopyOnWriteArraySet<ActionListener> actionListeners =new CopyOnWriteArraySet<>();
   private IntegerView                                   integerView;
   private JLabel                                        lblNewLabel;
   /**
    * Create the panel.
    */
   public Fontauswahl() {
      initGUI();
   }
   public enum STYLE {
      Normal(Font.PLAIN), Fett(Font.BOLD), Kursiv(Font.ITALIC), FettKursiv(Font.BOLD + Font.ITALIC);
      /** Wert aus Font */
      public final int nr;
      STYLE(int i) {
         nr=i;
      }
   }
   /**
    * @param listener
    *           hinzufügen
    */
   public synchronized void addActionListener(ActionListener listener) {
      actionListeners.add(listener);
   }
   @Override
   public Font getFont() {
      if (fontFont == null) {
         fontFont=super.getFont();
      }
      return fontFont;
   }
   /**
    * @return Scalefaktor 0.01f bis 1.99f
    */
   public double getStretch() {
      if (getIntegerView().getValue() instanceof final Number n)
         return 1d + (n.doubleValue() / 100d);
      return 1d;
   }
   /*
    * @Override public void update(Observable o, Object arg) { if (arg instanceof Color c) { getMustertext().setForeground(c); observable.fire(arg); }
    * }
    */
   @Override
   public void propertyChange(PropertyChangeEvent evt) {
      if (evt.getNewValue() instanceof final Color c) {
         getMustertext().setForeground(c);
         firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
         // observable.fire(arg);
      }
   }
   /**
    * @param listener
    *           entfernen
    */
   public void removeActionListener(ActionListener listener) {
      actionListeners.remove(listener);
   }
   /**
    * @param familiy
    *           Name der Font Familie
    * @param size
    *           größe in Punkt
    * @param style
    *           Stil-Auswahl aus Font.PLAIN, BOLD, ITALIC, or BOLD+ITALIC.
    */
   public void setFont(@Nullable String familiy, float size, STYLE style) {
      if ((familiy == null) || familiy.isEmpty() || (style == null) || (size <= 0.01f))
         return;
      final var isSameSize=(Math.abs(size - fontSize) < 0.001f);
      if (familiy.equalsIgnoreCase(fontFamily) && style.equals(fontStyle) && isSameSize)
         return;
      synchronized (fontFamily) {
         fontFamily=familiy;
         fontStyle=style;
         fontSize=size;
      }
      final var diff=Math.abs(Math.round(fontSize) - fontSize);
      if (new Font(fontFamily, fontStyle.nr, Math.round(fontSize)) instanceof Font font)
         fontFont=(diff <= 0.02f) ? font : font.deriveFont(fontSize);
      getMustertext().setFont(fontFont);
      SwingUtilities.invokeLater(
               () -> fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Font changed")));
   }
   /**
    * @return fontFamily
    */
   @SuppressWarnings("null")
   @NonNull
   JComboBox<String> getComboBoxFamily() {
      if (comboBoxFamily == null) {
         @NonNull
         final String[] fontFamilies=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
         @NonNull
         final Vector<String> families=new Vector<>();
         Collections.addAll(families, fontFamilies);
         comboBoxFamily=new JComboBox<>(families);
         // if (fontFamily==null)fontFamily="Arial";
         fontFamily=getPreferences().get(TAG_FONTFAMILY, fontFamily);
         // System.out.println("Fam:get " + getPreferences().get("FONTFAMILY", fontFamily));
         comboBoxFamily.addActionListener(e -> {
            // System.out.println("Action " + e.getActionCommand() + getComboBoxFamily().getSelectedItem());
            setFont((String) getComboBoxFamily().getSelectedItem(), fontSize, fontStyle);
            getPreferences().put(TAG_FONTFAMILY, fontFamily);
            // System.out.println("Fam:put" + getPreferences().get("FONTFAMILY", "nix"));
            try {
               getPreferences().flush();
            } catch (final BackingStoreException e1) {/* */}
            // System.out.println("Fam:flush" + getPreferences().get("FONTFAMILY", "nix"));
         });
         comboBoxFamily.setSelectedItem(fontFamily);
         comboBoxFamily.setPreferredSize(new Dimension(200, 28));
      }
      return comboBoxFamily;
   }
   private void fireActionPerformed(ActionEvent e) {
      for (final ActionListener actionListener:actionListeners)
         actionListener.actionPerformed(e);
   }
   private JComboBox<STYLE> getComboBoxStyle() {
      if (comboBoxStyle == null) {
         final Vector<STYLE> v=new Vector<>();
         Collections.addAll(v, STYLE.values());
         comboBoxStyle=new JComboBox<>(v);
         comboBoxStyle.addActionListener(e -> {
            System.out.println(TEXT_ACTION + e.getActionCommand() + getComboBoxStyle().getSelectedItem());
            @Nullable
            final Object styleObj=getComboBoxStyle().getSelectedItem();
            if (styleObj instanceof final STYLE style)
               setFont(fontFamily, fontSize, style);
         });
         comboBoxStyle.setMinimumSize(new Dimension(200, 28));
         // comboBoxStyle.setFont(UIManager.getFont("Button.font"));
      }
      return comboBoxStyle;
   }
   private IntegerView getIntegerView() {
      if (integerView == null) {
         integerView=new IntegerView();
         integerView.setModel(new SpinnerNumberModel(0, -50, 100, 1));
         final Fontauswahl me=this;
         integerView.addChangeListener(e -> EventQueue.invokeLater(() -> {
            final String fontStretch=Double.toString(me.getStretch());
            System.out.println("stretch=" + fontStretch);
            getMustertext().repaint(100);
            getPreferences().put(TAG_FONTSTRECH, fontStretch);
            try {
               getPreferences().flush();
            } catch (final BackingStoreException e1) {/* */}
            fireActionPerformed(new ActionEvent(me, ActionEvent.ACTION_PERFORMED, "Font changed"));
         }));
         integerView.setEinheit("%");
         integerView.setText("");
      }
      return integerView;
   }
   @SuppressWarnings("null")
   @NonNull
   private JLabel getLblFamily() {
      if (lblFamily == null) {
         lblFamily=new JLabel("FontFamily:");
         lblFamily.setLabelFor(getComboBoxFamily());
      }
      return lblFamily;
   }
   @SuppressWarnings("null")
   @NonNull
   private JLabel getLblSize() {
      if (lblSize == null) {
         lblSize=new JLabel("Size:");
         lblSize.setLabelFor(getSpinnerSize());
      }
      return lblSize;
   }
   private JLabel getLblStretch() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("Stretch:");
      }
      return lblNewLabel;
   }
   @SuppressWarnings("null")
   @NonNull
   private JLabel getLblStyle() {
      if (lblStyle == null) {
         lblStyle=new JLabel("Style:");
         lblStyle.setLabelFor(getComboBoxStyle());
      }
      return lblStyle;
   }
   @SuppressWarnings("null")
   @NonNull
   private JLabel getMustertext() {
      if (Mustertext == null) {
         Mustertext=new JLabel("Mustertext") {
            static private final long serialVersionUID=3166086304017612018L;
            @Override
            protected void paintComponent(Graphics g) {
               final Graphics2D g2=(Graphics2D) g;
               g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
               final double f=getStretch();
               if (f != 1d) {
                  final double s=getWidth() / 2d;
                  g2.translate(s, 0);
                  g2.scale(f, 1);
                  g2.translate(-s, 0);
               }
               super.paintComponent(g);
            }
         };
         Mustertext.setBorder(new BevelBorder(BevelBorder.LOWERED));
         Mustertext.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return Mustertext;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         final GridBagLayout gbl_panel=new GridBagLayout();
         gbl_panel.rowWeights=new double[] {0.0, 0.0};
         gbl_panel.columnWeights=new double[] {1.0, 1.0, 1.0, 1.0};
         panel.setLayout(gbl_panel);
         final GridBagConstraints gbc_lblFontfamily=new GridBagConstraints();
         // gbc_lblFontfamily.weightx=0.2;
         gbc_lblFontfamily.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblFontfamily.anchor=GridBagConstraints.WEST;
         gbc_lblFontfamily.insets=new Insets(0, 0, 5, 5);
         gbc_lblFontfamily.gridx=0;
         gbc_lblFontfamily.gridy=0;
         panel.add(getLblFamily(), gbc_lblFontfamily);
         final GridBagConstraints gbc_lblSize=new GridBagConstraints();
         // gbc_lblSize.weightx=0.4;
         gbc_lblSize.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblSize.anchor=GridBagConstraints.WEST;
         gbc_lblSize.insets=new Insets(0, 0, 5, 5);
         gbc_lblSize.gridx=1;
         gbc_lblSize.gridy=0;
         panel.add(getLblSize(), gbc_lblSize);
         final GridBagConstraints gbc_lblStyle=new GridBagConstraints();
         // gbc_lblStyle.weightx=0.3;
         gbc_lblStyle.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblStyle.anchor=GridBagConstraints.WEST;
         gbc_lblStyle.insets=new Insets(0, 0, 5, 5);
         gbc_lblStyle.gridx=2;
         gbc_lblStyle.gridy=0;
         panel.add(getLblStyle(), gbc_lblStyle);
         final GridBagConstraints gbc_lblStretch=new GridBagConstraints();
         gbc_lblStretch.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblStretch.anchor=GridBagConstraints.WEST;
         gbc_lblStretch.insets=new Insets(0, 0, 5, 5);
         gbc_lblStretch.gridx=3;
         gbc_lblStretch.gridy=0;
         panel.add(getLblStretch(), gbc_lblStretch);
         final GridBagConstraints gbc_comboBoxFontFamily=new GridBagConstraints();
         gbc_comboBoxFontFamily.fill=GridBagConstraints.HORIZONTAL;
         gbc_comboBoxFontFamily.anchor=GridBagConstraints.EAST;
         gbc_comboBoxFontFamily.insets=new Insets(0, 5, 0, 5);
         gbc_comboBoxFontFamily.gridx=0;
         gbc_comboBoxFontFamily.gridy=1;
         panel.add(getComboBoxFamily(), gbc_comboBoxFontFamily);
         final GridBagConstraints gbc_spinnerSize=new GridBagConstraints();
         gbc_spinnerSize.fill=GridBagConstraints.HORIZONTAL;
         gbc_spinnerSize.anchor=GridBagConstraints.EAST;
         gbc_spinnerSize.insets=new Insets(0, 5, 0, 5);
         gbc_spinnerSize.gridx=1;
         gbc_spinnerSize.gridy=1;
         panel.add(getSpinnerSize(), gbc_spinnerSize);
         final GridBagConstraints gbc_comboBoxStyle=new GridBagConstraints();
         gbc_comboBoxStyle.fill=GridBagConstraints.HORIZONTAL;
         gbc_comboBoxStyle.insets=new Insets(0, 5, 0, 5);
         gbc_comboBoxStyle.gridx=2;
         gbc_comboBoxStyle.gridy=1;
         panel.add(getComboBoxStyle(), gbc_comboBoxStyle);
         final GridBagConstraints gbc_integerView=new GridBagConstraints();
         gbc_integerView.insets=new Insets(0, 5, 0, 0);
         gbc_integerView.fill=GridBagConstraints.HORIZONTAL;
         gbc_integerView.anchor=GridBagConstraints.EAST;
         gbc_integerView.gridx=3;
         gbc_integerView.gridy=1;
         panel.add(getIntegerView(), gbc_integerView);
         // setFont(fontFamily, fontSize, fontStyle);
      }
      return panel;
   }
   @SuppressWarnings("null")
   @NonNull
   private Preferences getPreferences() {
      if (preferences == null)
         preferences=getPreferences(this.getClass());
      return preferences;
   }
   /**
    * Ermittelt ein Preference-Object für die übergebene Class
    *
    * @return preferences-Object
    */
   @SuppressWarnings("null")
   @NonNull
   private Preferences getPreferences(Class<?> c) {
      if (preferences == null) {
         final Class<?> cl=(c != null) ? c : this.getClass();
         preferences=Preferences.userNodeForPackage(cl);
         // System.out.println("P:" + preferences);
      }
      return preferences;
   }
   @SuppressWarnings("null")
   @NonNull
   private JSpinner getSpinnerSize() {
      if (spinnerSize == null) {
         spinnerSize=new JSpinner();
         spinnerSize.setMinimumSize(new Dimension(400, 28));
         spinnerSize.setModel(new SpinnerNumberModel(1f, 1f, 1000f, 1f));
         synchronized (fontFamily) {
            fontSize=getPreferences().getFloat(TAG_FONTSIZE, fontSize);
         }
         spinnerSize.addChangeListener(changeEvent -> {
            final Object value=getSpinnerSize().getValue();
            System.out.println(TEXT_ACTION + value + value.getClass());
            if (value instanceof final Integer i)
               setFont(fontFamily, i, fontStyle);
            else
               if (value instanceof final Float f)
                  setFont(fontFamily, f, fontStyle);
               else
                  if (value instanceof final Number n)
                     setFont(fontFamily, n.floatValue(), fontStyle);
            getPreferences().putFloat(TAG_FONTSIZE, fontSize);
            try {
               getPreferences().flush();
            } catch (final BackingStoreException e1) { /* */}
            System.out.println("Size:flush " + getPreferences().getFloat(TAG_FONTSIZE, fontSize));
         });
         spinnerSize.setValue(getPreferences().getFloat(TAG_FONTSIZE, fontSize));
      }
      return spinnerSize;
   }
   final private void initGUI() {
      setBorder(new EmptyBorder(5, 5, 5, 5));
      setPreferredSize(new Dimension(500, 150));
      setLayout(new BorderLayout(5, 5));
      add(getMustertext(), BorderLayout.CENTER);
      add(getPanel(), BorderLayout.NORTH);
   }
}

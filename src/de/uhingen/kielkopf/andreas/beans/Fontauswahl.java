/**
 *
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author andreas
 *
 */
public class Fontauswahl extends JPanel implements PropertyChangeListener {
   static private final long                             serialVersionUID=-1221877930226284285L;
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
      if (fontFont == null)
         fontFont=super.getFont();
      return fontFont;
   }
   /**
    * @return Scalefaktor 0.01f bis 1.99f
    */
   public double getStretch() {
      return getIntegerView().getValue() instanceof final Number n ? 1d + n.doubleValue() / 100d : 1d;
   }
   /*
    *
    */
   @Override
   public void propertyChange(PropertyChangeEvent evt) {
      if (evt.getNewValue() instanceof final Color c) {
         getMustertext().setForeground(c);
         firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
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
   public void setFont(String familie_, float size, STYLE style_) {
      if (familie_ instanceof final String familie && !familie.isBlank() && style_ instanceof final STYLE style && size >= 0.01f) {
         final var isSameSize=Math.abs(size - fontSize) < 0.001f;
         if (familie.equalsIgnoreCase(fontFamily) && style.equals(fontStyle) && isSameSize)
            return;
         synchronized (fontFamily) {
            fontFamily=familie;
            fontStyle=style;
            fontSize=size;
         }
         final var diff=Math.abs(Math.round(fontSize) - fontSize);
         if (new Font(fontFamily, fontStyle.nr, Math.round(fontSize)) instanceof final Font font)
            fontFont=diff <= 0.02f ? font : font.deriveFont(fontSize);
         getMustertext().setFont(fontFont);
         SwingUtilities.invokeLater(
                  () -> fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Font changed")));
      }
   }
   /**
    * ComboBox mit allen Fonts auf dem System
    *
    * @return fontFamily
    */
   JComboBox<String> getComboBoxFamily() {
      if (comboBoxFamily == null) {
         comboBoxFamily=new JComboBox<>(/* new Vector<>( Arrays.asList( */ GraphicsEnvironment
                  .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()/* )) */);
         if (getPreferences().get(TAG_FONTFAMILY, fontFamily) instanceof final String s)
            setFont(s, fontSize, fontStyle);
         comboBoxFamily.addActionListener(_ -> {
            setFont((String) getComboBoxFamily().getSelectedItem(), fontSize, fontStyle);
            getPreferences().put(TAG_FONTFAMILY, fontFamily);
            try {
               getPreferences().flush();
            } catch (final BackingStoreException e1) {/* */}
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
         comboBoxStyle=new JComboBox<>(new Vector<>(Arrays.asList(STYLE.values())));
         comboBoxStyle.addActionListener(_ -> {
            if (getComboBoxStyle().getSelectedItem() instanceof final STYLE style)
               setFont(fontFamily, fontSize, style);
         });
         comboBoxStyle.setMinimumSize(new Dimension(200, 28));
      }
      return comboBoxStyle;
   }
   private IntegerView getIntegerView() {
      if (integerView == null) {
         integerView=new IntegerView();
         integerView.setModel(new SpinnerNumberModel(0, -50, 100, 1));
         final var me=this;
         integerView.addChangeListener(_ -> SwingUtilities.invokeLater(() -> {
            final var fontStretch=Double.toString(me.getStretch());
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
   private JLabel getLblFamily() {
      if (lblFamily == null) {
         lblFamily=new JLabel("FontFamily:");
         lblFamily.setLabelFor(getComboBoxFamily());
      }
      return lblFamily;
   }
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
   public JLabel getMustertext() {
      if (Mustertext == null) {
         Mustertext=new JLabel("Mustertext") {
            static private final long serialVersionUID=3166086304017612018L;
            @Override
            protected void paintComponent(Graphics g) {
               final var g2=(Graphics2D) g;
               g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
               final var dbl=getStretch();
               if (dbl != 1d) {
                  final var s=getWidth() / 2d;
                  g2.translate(s, 0);
                  g2.scale(dbl, 1);
                  g2.translate(-s, 0);
               }
               super.paintComponent(g);
            }
         };
         Mustertext.setBorder(new BevelBorder(BevelBorder.LOWERED));
         // Mustertext.setFont(getFont());
         Mustertext.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return Mustertext;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         final var gbl_panel=new GridBagLayout();
         gbl_panel.rowWeights=new double[] {0.0, 0.0};
         gbl_panel.columnWeights=new double[] {1.0, 1.0, 1.0, 1.0};
         panel.setLayout(gbl_panel);
         final var gbc_lblFontfamily=new GridBagConstraints();
         // gbc_lblFontfamily.weightx=0.2;
         gbc_lblFontfamily.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblFontfamily.anchor=GridBagConstraints.WEST;
         gbc_lblFontfamily.insets=new Insets(0, 0, 5, 5);
         gbc_lblFontfamily.gridx=0;
         gbc_lblFontfamily.gridy=0;
         panel.add(getLblFamily(), gbc_lblFontfamily);
         final var gbc_lblSize=new GridBagConstraints();
         // gbc_lblSize.weightx=0.4;
         gbc_lblSize.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblSize.anchor=GridBagConstraints.WEST;
         gbc_lblSize.insets=new Insets(0, 0, 5, 5);
         gbc_lblSize.gridx=1;
         gbc_lblSize.gridy=0;
         panel.add(getLblSize(), gbc_lblSize);
         final var gbc_lblStyle=new GridBagConstraints();
         // gbc_lblStyle.weightx=0.3;
         gbc_lblStyle.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblStyle.anchor=GridBagConstraints.WEST;
         gbc_lblStyle.insets=new Insets(0, 0, 5, 5);
         gbc_lblStyle.gridx=2;
         gbc_lblStyle.gridy=0;
         panel.add(getLblStyle(), gbc_lblStyle);
         final var gbc_lblStretch=new GridBagConstraints();
         gbc_lblStretch.fill=GridBagConstraints.HORIZONTAL;
         gbc_lblStretch.anchor=GridBagConstraints.WEST;
         gbc_lblStretch.insets=new Insets(0, 0, 5, 5);
         gbc_lblStretch.gridx=3;
         gbc_lblStretch.gridy=0;
         panel.add(getLblStretch(), gbc_lblStretch);
         final var gbc_comboBoxFontFamily=new GridBagConstraints();
         gbc_comboBoxFontFamily.fill=GridBagConstraints.HORIZONTAL;
         gbc_comboBoxFontFamily.anchor=GridBagConstraints.EAST;
         gbc_comboBoxFontFamily.insets=new Insets(0, 5, 0, 5);
         gbc_comboBoxFontFamily.gridx=0;
         gbc_comboBoxFontFamily.gridy=1;
         panel.add(getComboBoxFamily(), gbc_comboBoxFontFamily);
         final var gbc_spinnerSize=new GridBagConstraints();
         gbc_spinnerSize.fill=GridBagConstraints.HORIZONTAL;
         gbc_spinnerSize.anchor=GridBagConstraints.EAST;
         gbc_spinnerSize.insets=new Insets(0, 5, 0, 5);
         gbc_spinnerSize.gridx=1;
         gbc_spinnerSize.gridy=1;
         panel.add(getSpinnerSize(), gbc_spinnerSize);
         final var gbc_comboBoxStyle=new GridBagConstraints();
         gbc_comboBoxStyle.fill=GridBagConstraints.HORIZONTAL;
         gbc_comboBoxStyle.insets=new Insets(0, 5, 0, 5);
         gbc_comboBoxStyle.gridx=2;
         gbc_comboBoxStyle.gridy=1;
         panel.add(getComboBoxStyle(), gbc_comboBoxStyle);
         final var gbc_integerView=new GridBagConstraints();
         gbc_integerView.insets=new Insets(0, 5, 0, 0);
         gbc_integerView.fill=GridBagConstraints.HORIZONTAL;
         gbc_integerView.anchor=GridBagConstraints.EAST;
         gbc_integerView.gridx=3;
         gbc_integerView.gridy=1;
         panel.add(getIntegerView(), gbc_integerView);
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
         final var cl=c != null ? c : this.getClass();
         preferences=Preferences.userNodeForPackage(cl);
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
            setFont(fontFamily, getPreferences().getFloat(TAG_FONTSIZE, fontSize), fontStyle);
         }
         spinnerSize.addChangeListener(_ -> {
            final var value=getSpinnerSize().getValue();
            switch (value) {
               case final Integer i -> setFont(fontFamily, i, fontStyle);
               case final Float f -> setFont(fontFamily, f, fontStyle);
               case final Number n -> setFont(fontFamily, n.floatValue(), fontStyle);
               case null, default -> {
                  /* nix tun */ }
            }
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
      add(getPanel(), BorderLayout.NORTH);
      add(getMustertext(), BorderLayout.CENTER);
      // SwingUtilities.invokeLater(() -> getMustertext().setFont(getFont()));
   }
}

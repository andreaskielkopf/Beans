/**
 *
 */
package de.uhingen.kielkopf.andreas.beans;

import java.awt.BorderLayout;
import java.awt.print.*;
import java.util.Arrays;
import java.util.Vector;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.swing.*;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author andreas
 *
 */
public class Druckvorschau extends JPanel {
   static private final long       serialVersionUID=-5880307937617186718L;
   private JButton                 btnPrint;
   private Printable               printable;
   private JComboBox<PrintService> comboBoxPrinter;
   private JComboBox<String>       comboBoxPapier;
   private JComboBox<Horizontal>   comboBoxHorizontal;
   private JComboBox<Vertikal>     comboBoxVertikal;
   private JPanel                  panel;
   private JPanel                  panel_1;
   private JPanel                  panel_2;
   private PaperPanel              paperPanel;
   private Box                     horizontalBox;
   private JPanel                  panel_4;
   private JPanel                  panel_5;
   private IntegerView             skalierung;
   /**
    * Panel zur Druckvorschau und zum Druck eines beliebigen Java-Elements(Printable)
    */
   public Druckvorschau() {
      setLayout(new BorderLayout());
      add(getPanel(), BorderLayout.NORTH);
      add(getPanel_1(), BorderLayout.CENTER);
      add(getPanel_2(), BorderLayout.SOUTH);
   }
   private enum Horizontal {
      LINKS, MITTE, RECHTS
   }

   private enum Vertikal {
      OBEN, MITTE, UNTEN
   }
   /**
    * Übergibt das Element das gedruckt werden soll
    *
    * @param vorschau
    *           das Element das gedruckt werden soll (zur Vorschau)
    */
   public void setPrintable(Printable vorschau) {
      printable=vorschau;
      getBtnPrint().setEnabled(printable != null);
   }
   /**
    * Knopf um den Druck auszulösen
    *
    * @return button
    */
   JButton getBtnPrint() {
      if (btnPrint == null) {
         btnPrint=new JButton("Print");
         btnPrint.setEnabled(false);
         btnPrint.addActionListener(_ -> {
            try {
               print();
            } catch (final PrinterException ignore) {
               ignore.printStackTrace();
            }
         });
      }
      return btnPrint;
   }
   /**
    * Auswahl der Horizontalen Ausrichtung
    *
    * @return auswahlH
    */
   @SuppressWarnings({"unchecked", "rawtypes", "null"})
   @NonNull
   JComboBox<Horizontal> getComboBoxHorizontal() {
      if (comboBoxHorizontal == null) {
         comboBoxHorizontal=new JComboBox(Horizontal.values());
         comboBoxHorizontal.addActionListener(_ -> getPaperPanel().setPos(getComboBoxVertikal().getSelectedIndex() - 1,
                  getComboBoxHorizontal().getSelectedIndex() - 1));
         comboBoxHorizontal.setSelectedItem(Horizontal.MITTE);
      }
      return comboBoxHorizontal;
   }
   /**
    * Auswahl des Papierformats
    *
    * @return auswahlP
    */
   JComboBox<String> getComboBoxPapier() {
      if (comboBoxPapier == null) {
         comboBoxPapier=new JComboBox<>();
         comboBoxPapier.setEnabled(false);
         // comboBoxPapier.addActionListener(e -> {});
      }
      return comboBoxPapier;
   }
   /**
    * Auswahl des Druckers
    *
    * @return auswahlD
    */
   JComboBox<PrintService> getComboBoxPrinter() {
      if (comboBoxPrinter == null) {
         comboBoxPrinter=new JComboBox<>(new Vector<>(Arrays.asList(PrinterJob.lookupPrintServices())));
      }
      return comboBoxPrinter;
   }
   /**
    * Auswahl der vertikalen Ausrichtung
    *
    * @return auswahlV
    */
   @SuppressWarnings({"unchecked", "rawtypes", "null"})
   @NonNull
   JComboBox<Vertikal> getComboBoxVertikal() {
      if (comboBoxVertikal == null) {
         comboBoxVertikal=new JComboBox(Vertikal.values());
         comboBoxVertikal.addActionListener(_ -> getPaperPanel().setPos(getComboBoxVertikal().getSelectedIndex() - 1,
                  getComboBoxHorizontal().getSelectedIndex() - 1));
         comboBoxVertikal.setSelectedItem(Vertikal.OBEN);
      }
      return comboBoxVertikal;
   }
   /**
    * Auswahl der Skalierung des printable
    *
    * @return auswahlS
    */
   IntegerView getSkalierung() {
      if (skalierung == null) {
         skalierung=new IntegerView();
         skalierung.setModel(new SpinnerNumberModel(0, -50, 100, 1));
         skalierung.setEinheit("%");
         skalierung.setText("stretch");
      }
      return skalierung;
   }
   /**
    * Versucht den Ausdruck zu starten
    *
    * @throws PrinterException
    *            Abbruch des Drucks
    */
   protected void print() throws PrinterException {
      if (printable == null)
         return;
      final var printjob=PrinterJob.getPrinterJob(); // Der Printjob. der nacher die Arbeit macht
      if (getComboBoxPrinter().getSelectedItem() instanceof final PrintService ps)
         printjob.setPrintService(ps);
      final var paper=new Paper(); // Das im Drucker verwendete Papier
      final Double papierBreite=(210 * 72) / 25.4d, papierHoehe=(297 * 72) / 25.4d;
      final Double plakettenBreite=(52 * 72) / 25.4d, plakettenHoehe=(82 * 72) / 25.4d;
      paper.setSize(papierBreite, papierHoehe); // Das ist A4
      // Und jetzt welchen Bereich soll man denn bedrucken
      Double X=0d, Y=0d;
      if (getComboBoxHorizontal().getSelectedItem() instanceof final Horizontal h)
         X=switch (h) {
            case LINKS -> 0d;
            case MITTE -> (papierBreite - plakettenBreite) / 2;
            case RECHTS -> papierBreite - plakettenBreite;
         };
      if (getComboBoxVertikal().getSelectedItem() instanceof final Vertikal v)
         Y=switch (v) {
            case OBEN -> 0d;
            case MITTE -> (papierHoehe - plakettenHoehe) / 2;
            case UNTEN -> papierHoehe - plakettenHoehe;
         };
      paper.setImageableArea(X, Y, plakettenBreite, plakettenHoehe);
      final var pageformat=new PageFormat();
      pageformat.setPaper(paper);
      pageformat.setOrientation(PageFormat.LANDSCAPE);
      printjob.setPrintable(printable, pageformat);
      printjob.setCopies(1);
      printjob.setJobName("Kongressplakette");
      final var attr_set=new HashPrintRequestAttributeSet();
      // MediaSizeName msn = MediaSize.findMedia(82, 52, MediaSize.MM);
      // MediaSizeName msn = MediaSize.ISO.A5.getMediaSizeName();
      // attr_set.add(msn);
      attr_set.add(new Copies(1));
      printjob.print(attr_set);
   }
   @SuppressWarnings("null")
   @NonNull
   private Box getHorizontalBox() {
      if (horizontalBox == null) {
         horizontalBox=Box.createHorizontalBox();
         horizontalBox.add(getPanel_4());
         horizontalBox.add(getPaperPanel());
         horizontalBox.add(getPanel_5());
      }
      return horizontalBox;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.add(getComboBoxPrinter());
         panel.add(getComboBoxPapier());
         panel.add(getComboBoxHorizontal());
         panel.add(getComboBoxVertikal());
      }
      return panel;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getHorizontalBox());
      }
      return panel_1;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.add(getBtnPrint());
         panel_2.add(getSkalierung());
      }
      return panel_2;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel_4() {
      if (panel_4 == null)
         panel_4=new JPanel();
      return panel_4;
   }
   @SuppressWarnings("null")
   @NonNull
   private JPanel getPanel_5() {
      if (panel_5 == null)
         panel_5=new JPanel();
      return panel_5;
   }
   @SuppressWarnings("null")
   @NonNull
   private PaperPanel getPaperPanel() {
      if (paperPanel == null)
         paperPanel=new PaperPanel("Andreas Kielkopf", "Uhingen");
      return paperPanel;
   }
}

package de.uhingen.kielkopf.andreas.beans;

import java.awt.*;

import javax.swing.*;

/**
 * JPanel mit Elementen zur Auswahl einer Farbe
 * 
 * @author Andreas Kielkopf
 *
 */
public class ColorWahl extends JPanel {
   static private final long serialVersionUID=-2932667983835743210L;
   private JLabel            lblColor;
   private JPanel            panel;
   private JSlider           slider;
   private JSlider           slider_1;
   private JSlider           slider_2;
   private JSlider           slider_3;
   protected int             red             =10;
   protected int             green           =10;
   protected int             blue            =10;
   protected int             white           =250;
   private Color             color;
   /**
    * Create the panel.
    */
   public ColorWahl() {
      setLayout(new BorderLayout());
      add(getPanel(), BorderLayout.CENTER);
      setColor();
   }
   private JLabel getLblColor() {
      if (lblColor == null) {
         lblColor=new JLabel("Color");
         lblColor.setOpaque(true);
         lblColor.setBackground(Color.WHITE);
         lblColor.setMinimumSize(new Dimension(50, 50));
         lblColor.setHorizontalTextPosition(SwingConstants.CENTER);
         lblColor.setFont(new Font("Dialog", Font.BOLD, 30));
         lblColor.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblColor;
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(0, 1, 0, 0));
         panel.add(getSlider());
         panel.add(getSlider_1());
         panel.add(getSlider_2());
         panel.add(getSlider_3());
         panel.setMinimumSize(new Dimension(50, 50));
      }
      return panel;
   }
   private JSlider getSlider() {
      if (slider == null) {
         slider=new JSlider();
         slider.setForeground(Color.RED);
         slider.setPaintTicks(true);
         slider.setPaintLabels(true);
         slider.setMinorTickSpacing(2);
         slider.setMajorTickSpacing(20);
         slider.setMaximum(255);
         slider.setValue(red);
         slider.addChangeListener(e -> {
            red=slider.getValue();
            setColor();
         });
         slider.setBackground(Color.WHITE);
      }
      return slider;
   }
   private JSlider getSlider_1() {
      if (slider_1 == null) {
         slider_1=new JSlider();
         slider_1.setMajorTickSpacing(20);
         slider_1.setMinorTickSpacing(2);
         slider_1.setForeground(Color.GREEN);
         slider_1.setPaintTicks(true);
         slider_1.setPaintLabels(true);
         slider_1.setMaximum(255);
         slider_1.setValue(green);
         slider_1.addChangeListener(e -> {
            green=slider_1.getValue();
            setColor();
         });
         slider_1.setBackground(Color.WHITE);
      }
      return slider_1;
   }
   private JSlider getSlider_2() {
      if (slider_2 == null) {
         slider_2=new JSlider();
         slider_2.setPaintTicks(true);
         slider_2.setMinorTickSpacing(2);
         slider_2.setMajorTickSpacing(20);
         slider_2.setForeground(Color.BLUE);
         slider_2.setPaintLabels(true);
         slider_2.setMaximum(255);
         slider_2.setValue(blue);
         slider_2.addChangeListener(e -> {
            blue=slider_2.getValue();
            setColor();
         });
         slider_2.setBackground(Color.WHITE);
      }
      return slider_2;
   }
   private JSlider getSlider_3() {
      if (slider_3 == null) {
         slider_3=new JSlider();
         slider_3.setPaintTicks(true);
         slider_3.setPaintLabels(true);
         slider_3.setMinorTickSpacing(2);
         slider_3.setMajorTickSpacing(20);
         slider_3.setForeground(Color.BLACK);
         slider_3.setMaximum(255);
         slider_3.setValue(white);
         slider_3.addChangeListener(e -> {
            white=slider_3.getValue();
            setColor();
         });
         slider_3.setBackground(java.awt.Color.WHITE);
      }
      return slider_3;
   }
   private void setColor() {
      final Color old=color;
      color=new Color(red, green, blue, white);
      firePropertyChange("color", old, color);
      getLblColor().setForeground(color);
   }
}

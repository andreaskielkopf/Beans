/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;

/**
 * @author Andreas Kielkopf
 * @param <T>
 *
 */
public class JScrollList<T extends Comparable<T>> extends JPanel {
   private static final long   serialVersionUID=5740746925432033747L;
   private JPanel              panel;
   private JScrollPane         scrollPane;
   private JList<T>            list;
   private SortedListModel<T> listModel;
   private JPanel              panelNorth;
   private JPanel              panelSouth;
   private JPanel              panelWest;
   private JPanel              panelEast;
   /**
    * Create the panel.
    */
   public JScrollList() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getScrollPane());
         panel.add(getPanelWest(), BorderLayout.WEST);
         panel.add(getPanelNorth(), BorderLayout.NORTH);
         panel.add(getPanelSouth(), BorderLayout.SOUTH);
         panel.add(getPanelEast(), BorderLayout.EAST);
      }
      return panel;
   }
   private JScrollPane getScrollPane() {
      if (scrollPane == null) {
         scrollPane=new JScrollPane(getList());
      }
      return scrollPane;
   }
   private JList<T> getList() {
      if (list == null) {
         list=new JList<T>(getListModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
               final var idx=locationToIndex(e.getPoint());
               if (idx >= 0 && idx < getListModel().getSize() && getListModel().get(idx) instanceof final Object o)
                  return o instanceof final hasToolTip t ? t.getToolTip() : "Info: " + o.toString();
               return "nix";
            }
         };
         list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean isSelected,
                     boolean cellHasFocus) {
               Component q=super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
               if (q instanceof JLabel jl)
                  if (value instanceof hasBackground hb)
                     if (hb.getBackground(isSelected) instanceof Color c)
                        jl.setBackground(c);
               return q;
            }
         });
         list.setLayoutOrientation(JList.VERTICAL_WRAP);
         list.setVisibleRowCount(20);
      }
      return list;
   }
   public T getSelectedValue() {
      return list.getSelectedValue();
   }
   public void addListSelectionListener(ListSelectionListener listener) {
      list.addListSelectionListener(listener);
   }
 
   public SortedListModel<T> getListModel() {
      if (listModel == null)
         listModel=new SortedListModel<>();
      return listModel;
   }
   public JPanel getPanelNorth() {
      if (panelNorth == null) {
         panelNorth=new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
      }
      return panelNorth;
   }
   public JPanel getPanelSouth() {
      if (panelSouth == null) {
         panelSouth=new JPanel(new BorderLayout(0, 0));
      }
      return panelSouth;
   }
   public JPanel getPanelWest() {
      if (panelWest == null) {
         panelWest=new JPanel(new BorderLayout(0, 0));
      }
      return panelWest;
   }
   public JPanel getPanelEast() {
      if (panelEast == null) {
         panelEast=new JPanel(new BorderLayout(0, 0));
      }
      return panelEast;
   }
   /**
    * @param jf
    */
   public void add(T e) {
      if (!contains(e)) {
         getListModel().addElement(e);
         this.repaint(1000);
      }
   }
   public void clear() {
      getListModel().clear();
      this.repaint(1000);
   }
   public void select(String select) {
      if (getList().getSelectedValue() instanceof final T t && select.equals(t.toString()))
         return;
      final var e=getListModel().elements();
      while (e.hasMoreElements())
         if (e.nextElement() instanceof final T v)
            if (select.equals(v.toString()))
               SwingUtilities.invokeLater(() -> getList().setSelectedValue(v, true));
   }
   /**
    * @param jf
    * @return
    */
   public boolean contains(T t) {
      return getListModel().contains(t);
   }
}

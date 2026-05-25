/**
 *
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

/**
 * @author Andreas Kielkopf
 *
 */
public class AskPath extends JPanel {
   private static final long serialVersionUID=-9052284296573128242L;
   private TitledBorder      tb;
   private JPanel            panel;
   private JButton           btnSelect;
   private JTextField        pathField;
   private JLabel            lblInfo;
   private Path              path            =Path.of(".");
   private int               selectionMode   =JFileChooser.FILES_AND_DIRECTORIES;
   private FileFilter        filter;
   /**
    * Create the panel.
    */
   public AskPath() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      // setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getBtnSelect(), BorderLayout.WEST);
         panel.add(getPathField());
         panel.add(getLblInfo(), BorderLayout.NORTH);
         panel.setBorder(new TitledBorder(null, "e", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      }
      return panel;
   }
   private JButton getBtnSelect() {
      if (btnSelect == null) {
         btnSelect=new JButton("select");
         btnSelect.addActionListener(_ -> select());
      }
      return btnSelect;
   }
   private void select() {
      System.out.println("select");
      final var chooser=new JFileChooser();
      chooser.setFileSelectionMode(selectionMode); // oder DIRECTORIES_ONLY
      if (getTitel() instanceof final String t)
         chooser.setDialogTitle(t);// "Wähle das Projektverzeichnis (z. B. /home/user/git/projekt)");
      if (getPath() instanceof final Path p)
         chooser.setCurrentDirectory(p.toFile());
      if (getFilter() instanceof final FileFilter f)
         chooser.setFileFilter(f);
      chooser.setDialogType(JFileChooser.OPEN_DIALOG);
      chooser.setAccessory(new JLabel(getText()));
      if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
         if (chooser.getSelectedFile() instanceof final File selected)
            setPath(selected.toPath());
         System.out.println("Gewählter Pfad: " + getPath().toString());
      } else {
         System.out.println("Abgebrochen");
      }
   }
   public JTextField getPathField() {
      if (pathField == null) {
         pathField=new JTextField();
         pathField.setColumns(10);
      }
      return pathField;
   }
   private JLabel getLblInfo() {
      if (lblInfo == null) {
         lblInfo=new JLabel("Wähle den Pfad oder die Datei wo die main()-Methode zu suchen ist");
         lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblInfo;
   }
   public void setSelectionMode(int selectionMode_) {
      switch (selectionMode_) {
         case JFileChooser.DIRECTORIES_ONLY, JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.FILES_ONLY:
            selectionMode=selectionMode_;
      }
   }
   public String getTitel() {
      return getTb().getTitle();
   }
   public void setTitel(String titel_) {
      getTb().setTitle(titel_);
   }
   public TitledBorder getTb() {
      if (tb == null) {
         tb=getPanel().getBorder() instanceof final TitledBorder t ? t
                  : new TitledBorder(null, "e", TitledBorder.LEADING, TitledBorder.TOP, null, null);
      }
      return tb;
   }
   public String getText() {
      return lblInfo.getText();
   }
   public void setText(String text) {
      lblInfo.setText(text);
   }
   public FileFilter getFilter() {
      return filter;
   }
   public void setFilter(FileFilter filter_) {
      filter=filter_;
   }
   public Path getPath() {
      return path;
   }
   public void setPath(Path path_) {
      path=path_;
      getPathField().setText(path.toString());
   }
}

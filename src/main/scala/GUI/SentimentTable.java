package GUI;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;

public class SentimentTable extends JFrame {

    public SentimentTable(Object[][] values, String id) {
        super(id);

        DefaultTableModel dtm = new DefaultTableModel() {
            // make first cell uneditable
            public boolean isCellEditable(int row, int column)
            {
                return !(column == 0);
            }
        };

        dtm.setDataVector(values,
                new Object[]{"SentimentPackage","Formal citations", "Text"});

        JTable table = new JTable(dtm) {
            public Class getColumnClass(int column)
            {
                return getValueAt(0, column).getClass();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getColumn("SentimentPackage").setCellRenderer(new SentimentRenderer());
        table.getColumn("Formal citations").setCellRenderer(new TextAreaRenderer());
        table.getColumn("Formal citations").setCellEditor(new TextAreaEditor());
        table.getColumn("Text").setCellRenderer(new TextAreaRenderer());
        table.getColumn("Text").setCellEditor(new TextAreaEditor());
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(600);


        table.setRowHeight(80);
        JScrollPane scroll = new JScrollPane(table);
        getContentPane().add(scroll);

        setSize( 900, 500 );
        setVisible(true);
    }
}

class SentimentRenderer extends JLabel implements TableCellRenderer {
    public SentimentRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setForeground(Color.BLACK);
        switch (value.toString()) {
            case "negative":
                setBackground(Color.red);
                break;
            case "objective":
                setBackground(Color.yellow);
                break;
            case "positive":
                setBackground(Color.green);
                break;
            default:
                break;
        }
        return this;
    }
}

class TextAreaRenderer extends JScrollPane implements TableCellRenderer
{
    JTextArea textarea;

    public TextAreaRenderer() {
        textarea = new JTextArea();
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        getViewport().add(textarea);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column)
    {
        if (isSelected) {
            //setForeground(table.getSelectionForeground());
            //setBackground(table.getSelectionBackground());
            //textarea.setForeground(table.getSelectionForeground());
            //textarea.setBackground(table.getSelectionBackground());
        } else {
            //setForeground(table.getForeground());
            //setBackground(table.getBackground());
           // textarea.setForeground(table.getForeground());
            //textarea.setBackground(table.getBackground());
        }
        textarea.setText((String) value);
        textarea.setCaretPosition(0);
        return this;
    }
}

class TextAreaEditor extends DefaultCellEditor {
    protected JScrollPane scrollpane;
    protected JTextArea textarea;

    public TextAreaEditor() {
        super(new JCheckBox());
        scrollpane = new JScrollPane();
        textarea = new JTextArea();
        textarea.setEditable(false);
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        scrollpane.getViewport().add(textarea);
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textarea.setText((String) value);

        return scrollpane;
    }

    public Object getCellEditorValue() {
        return textarea.getText();
    }
}
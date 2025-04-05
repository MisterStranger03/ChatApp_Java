package chatting;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class UIStyles {

    // ------------- Color Constants -------------
    public static final Color DARK_BG = new Color(30, 30, 30);
    public static final Color DARKER_BG = new Color(20, 20, 20);
    public static final Color LIGHT_TEXT = new Color(220, 220, 220);
    public static final Color ACCENT_COLOR = new Color(100, 255, 218);
    public static final Color BUTTON_BG = new Color(80, 180, 255);
    public static final Color BUTTON_BG_HOVER = new Color(255, 255, 255);
    public static final Color BUTTON_TEXT = Color.WHITE;
    public static final Color BUTTON_TEXT_HOVER = Color.BLACK;
    public static final Color FIELD_BG = new Color(60, 60, 60);
    public static final Color DATE_BG = new Color(80, 80, 80);

    // ------------- Font Constants -------------
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 36);
    public static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    public static void styleTextField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    public static void styleTextField(JPasswordField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(FIELD_BG);
        field.setForeground(LIGHT_TEXT);
        field.setCaretColor(LIGHT_TEXT);
        field.setBorder(new LineBorder(ACCENT_COLOR, 1));
    }

    public static void styleButton(JButton button) {
        button.setFont(BUTTON_FONT);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new RoundedBorder(10));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = b.getModel();
                int arc = 20;
                Color bg = BUTTON_BG;
                Color fg = BUTTON_TEXT;
                if (model.isRollover()) {
                    bg = BUTTON_BG_HOVER;
                    fg = BUTTON_TEXT_HOVER;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), arc, arc);
                g2.setColor(ACCENT_COLOR);
                g2.drawRoundRect(0, 0, b.getWidth() - 1, b.getHeight() - 1, arc, arc);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = new Rectangle(0, 0, b.getWidth(), b.getHeight());
                String text = b.getText();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (r.width - textWidth) / 2;
                int y = (r.height + textHeight) / 2 - 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
    }

    public static void styleInputArea(JTextArea area) {
        area.setFont(FIELD_FONT);
        area.setBackground(FIELD_BG);
        area.setForeground(LIGHT_TEXT);
        area.setCaretColor(LIGHT_TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new RoundedBorder(10));
        area.setRows(1);
    }

    public static void scrollToBottom(JScrollPane scrollPane) {
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    private static class RoundedBorder extends AbstractBorder {
        private int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getBackground());
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius + 1;
            return insets;
        }
    }
}

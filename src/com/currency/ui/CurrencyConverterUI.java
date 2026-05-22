package com.currency.ui;

import com.currency.model.Currency;
import com.currency.model.ConversionHistory;
import com.currency.service.CurrencyService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

public class CurrencyConverterUI extends JFrame {

    // ── Colour palette ──────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(15,  17,  26);
    private static final Color BG_CARD      = new Color(24,  27,  42);
    private static final Color BG_INPUT     = new Color(32,  36,  56);
    private static final Color ACCENT       = new Color(99, 102, 241);   // indigo
    private static final Color ACCENT_LIGHT = new Color(129, 140, 248);
    private static final Color SUCCESS      = new Color(34,  197,  94);
    private static final Color DANGER       = new Color(239,  68,  68);
    private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(55,  65,  81);

    // ── Fonts ────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_RESULT = new Font("Segoe UI", Font.BOLD,  26);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);

    private final CurrencyService service = new CurrencyService();

    // ── Shared controls ──────────────────────────────────────────────────
    private JTabbedPane tabs;

    // Convert tab
    private JComboBox<String> cbFrom, cbTo;
    private JTextField tfAmount;
    private JLabel lblResult, lblRate;

    // Currencies tab
    private JTable tblCurrencies;
    private DefaultTableModel tmCurrencies;

    // Add currency tab
    private JTextField tfCode, tfName, tfRate, tfSymbol;

    // Update rate tab
    private JTextField tfUpdateCode, tfUpdateRate;

    // Delete tab
    private JTextField tfDeleteCode;

    // History tab
    private JTable tblHistory;
    private DefaultTableModel tmHistory;
    private JTextField tfHistoryLimit;

    // Search history tab
    private JTextField tfSearchFrom, tfSearchTo;
    private JTable tblSearchResult;
    private DefaultTableModel tmSearch;

    // ── Constructor ──────────────────────────────────────────────────────
    public CurrencyConverterUI() {
        setTitle("Currency Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 620);
        setMinimumSize(new Dimension(760, 540));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);

        setVisible(true);
        refreshCurrencyComboBoxes();
        refreshCurrenciesTable();
    }

    // ── Header ───────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        p.setPreferredSize(new Dimension(0, 64));

        JLabel title = new JLabel("  💱  Currency Converter");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel("Real-time exchange  |  JDBC powered   ");
        sub.setFont(FONT_SMALL);
        sub.setForeground(TEXT_MUTED);

        p.add(title, BorderLayout.WEST);
        p.add(sub,   BorderLayout.EAST);
        return p;
    }

    // ── Tabbed pane ──────────────────────────────────────────────────────
    private JTabbedPane buildTabs() {
        tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_MUTED);
        tabs.setFont(FONT_LABEL);

        UIManager.put("TabbedPane.selected",              BG_CARD);
        UIManager.put("TabbedPane.contentAreaColor",      BG_DARK);
        UIManager.put("TabbedPane.tabAreaBackground",     BG_DARK);
        UIManager.put("TabbedPane.selectedForeground",    ACCENT_LIGHT);

        tabs.addTab("⇄  Convert",        buildConvertPanel());
        tabs.addTab("☰  Currencies",      buildCurrenciesPanel());
        tabs.addTab("＋  Add Currency",   buildAddPanel());
        tabs.addTab("✎  Update Rate",     buildUpdatePanel());
        tabs.addTab("✕  Delete",          buildDeletePanel());
        tabs.addTab("⟳  History",         buildHistoryPanel());
        tabs.addTab("⌕  Search History",  buildSearchPanel());

        return tabs;
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 1 – CONVERT
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildConvertPanel() {
        JPanel outer = darkPanel(new GridBagLayout());
        outer.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(8, 8, 8, 8);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        // Amount
        g.gridx = 0; g.gridy = 0; g.gridwidth = 1;
        outer.add(label("Amount"), g);
        g.gridx = 1;
        tfAmount = styledField("100");
        outer.add(tfAmount, g);

        // From
        g.gridx = 0; g.gridy = 1;
        outer.add(label("From"), g);
        g.gridx = 1;
        cbFrom = styledCombo();
        outer.add(cbFrom, g);

        // To
        g.gridx = 0; g.gridy = 2;
        outer.add(label("To"), g);
        g.gridx = 1;
        cbTo = styledCombo();
        outer.add(cbTo, g);

        // Convert button
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        JButton btn = accentButton("Convert");
        btn.addActionListener(e -> doConvert());
        outer.add(btn, g);

        // Result panel
        g.gridy = 4;
        JPanel resultCard = darkPanel(new GridLayout(2, 1, 4, 4));
        resultCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(16, 20, 16, 20)));

        lblResult = new JLabel("—", SwingConstants.CENTER);
        lblResult.setFont(FONT_RESULT);
        lblResult.setForeground(ACCENT_LIGHT);

        lblRate = new JLabel("Enter an amount and click Convert", SwingConstants.CENTER);
        lblRate.setFont(FONT_SMALL);
        lblRate.setForeground(TEXT_MUTED);

        resultCard.add(lblResult);
        resultCard.add(lblRate);
        outer.add(resultCard, g);

        return outer;
    }

    private void doConvert() {
        try {
            String from   = ((String) cbFrom.getSelectedItem());
            String to     = ((String) cbTo.getSelectedItem());
            double amount = Double.parseDouble(tfAmount.getText().trim());
            if (from == null || to == null) { showError("Select currencies."); return; }
            double result = service.convert(from, to, amount);
            if (result < 0) {
                lblResult.setForeground(DANGER);
                lblResult.setText("Not found");
                lblRate.setText("One or both currency codes are invalid.");
            } else {
                lblResult.setForeground(SUCCESS);
                lblResult.setText(String.format("%s %.4f  =  %s %.4f", from, amount, to, result));
                double rate = result / amount;
                lblRate.setText(String.format("1 %s  =  %.6f %s", from, rate, to));
            }
        } catch (NumberFormatException ex) {
            showError("Please enter a valid numeric amount.");
        } catch (SQLException ex) {
            showError("DB Error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 2 – VIEW CURRENCIES
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildCurrenciesPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 12));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"Code", "Name", "Rate to USD", "Symbol"};
        tmCurrencies = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCurrencies = styledTable(tmCurrencies);

        JScrollPane sp = styledScroll(tblCurrencies);

        JButton refresh = accentButton("⟳  Refresh");
        refresh.addActionListener(e -> refreshCurrenciesTable());

        JPanel top = darkPanel(new BorderLayout());
        top.add(sectionTitle("Supported Currencies"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);

        p.add(top, BorderLayout.NORTH);
        p.add(sp,  BorderLayout.CENTER);
        return p;
    }

    private void refreshCurrenciesTable() {
        try {
            tmCurrencies.setRowCount(0);
            for (Currency c : service.getAllCurrencies())
                tmCurrencies.addRow(new Object[]{c.getCode(), c.getName(),
                    String.format("%.4f", c.getRateToUSD()), c.getSymbol()});
            refreshCurrencyComboBoxes();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 3 – ADD CURRENCY
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildAddPanel() {
        JPanel p = darkPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(30, 50, 30, 50));
        GridBagConstraints g = gbc();

        g.gridy = 0; g.gridwidth = 2;
        p.add(sectionTitle("Add New Currency"), g);

        g.gridwidth = 1;
        g.gridy = 1; g.gridx = 0; p.add(label("Code (e.g. EUR)"), g);
        g.gridx = 1; tfCode = styledField(""); p.add(tfCode, g);

        g.gridy = 2; g.gridx = 0; p.add(label("Name"), g);
        g.gridx = 1; tfName = styledField(""); p.add(tfName, g);

        g.gridy = 3; g.gridx = 0; p.add(label("Rate to USD"), g);
        g.gridx = 1; tfRate = styledField(""); p.add(tfRate, g);

        g.gridy = 4; g.gridx = 0; p.add(label("Symbol"), g);
        g.gridx = 1; tfSymbol = styledField(""); p.add(tfSymbol, g);

        g.gridy = 5; g.gridx = 0; g.gridwidth = 2;
        JButton btn = accentButton("Add Currency");
        btn.addActionListener(e -> doAddCurrency());
        p.add(btn, g);
        return p;
    }

    private void doAddCurrency() {
        try {
            String code   = tfCode.getText().trim().toUpperCase();
            String name   = tfName.getText().trim();
            double rate   = Double.parseDouble(tfRate.getText().trim());
            String symbol = tfSymbol.getText().trim();
            if (code.isEmpty() || name.isEmpty() || symbol.isEmpty()) {
                showError("All fields are required."); return;
            }
            Currency c = new Currency(0, code, name, rate, symbol);
            if (service.addCurrency(c)) {
                showSuccess("Currency " + code + " added successfully.");
                tfCode.setText(""); tfName.setText(""); tfRate.setText(""); tfSymbol.setText("");
                refreshCurrenciesTable();
            } else {
                showError("Failed to add currency.");
            }
        } catch (NumberFormatException ex) {
            showError("Invalid rate. Enter a numeric value.");
        } catch (SQLException ex) {
            showError("DB Error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 4 – UPDATE RATE
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildUpdatePanel() {
        JPanel p = darkPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(30, 50, 30, 50));
        GridBagConstraints g = gbc();

        g.gridy = 0; g.gridwidth = 2;
        p.add(sectionTitle("Update Exchange Rate"), g);

        g.gridwidth = 1;
        g.gridy = 1; g.gridx = 0; p.add(label("Currency Code"), g);
        g.gridx = 1; tfUpdateCode = styledField(""); p.add(tfUpdateCode, g);

        g.gridy = 2; g.gridx = 0; p.add(label("New Rate to USD"), g);
        g.gridx = 1; tfUpdateRate = styledField(""); p.add(tfUpdateRate, g);

        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        JButton btn = accentButton("Update Rate");
        btn.addActionListener(e -> doUpdateRate());
        p.add(btn, g);
        return p;
    }

    private void doUpdateRate() {
        try {
            String code = tfUpdateCode.getText().trim().toUpperCase();
            double rate = Double.parseDouble(tfUpdateRate.getText().trim());
            if (code.isEmpty()) { showError("Enter a currency code."); return; }
            if (service.updateRate(code, rate)) {
                showSuccess("Rate for " + code + " updated.");
                tfUpdateCode.setText(""); tfUpdateRate.setText("");
                refreshCurrenciesTable();
            } else {
                showError("Currency not found or update failed.");
            }
        } catch (NumberFormatException ex) {
            showError("Invalid rate.");
        } catch (SQLException ex) {
            showError("DB Error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 5 – DELETE CURRENCY
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildDeletePanel() {
        JPanel p = darkPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(30, 50, 30, 50));
        GridBagConstraints g = gbc();

        g.gridy = 0; g.gridwidth = 2;
        p.add(sectionTitle("Delete Currency"), g);

        g.gridwidth = 1;
        g.gridy = 1; g.gridx = 0; p.add(label("Currency Code"), g);
        g.gridx = 1; tfDeleteCode = styledField(""); p.add(tfDeleteCode, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 2;
        JButton btn = dangerButton("Delete Currency");
        btn.addActionListener(e -> doDelete());
        p.add(btn, g);
        return p;
    }

    private void doDelete() {
        String code = tfDeleteCode.getText().trim().toUpperCase();
        if (code.isEmpty()) { showError("Enter a currency code."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete currency \"" + code + "\"? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            if (service.deleteCurrency(code)) {
                showSuccess("Currency " + code + " deleted.");
                tfDeleteCode.setText("");
                refreshCurrenciesTable();
            } else {
                showError("Currency not found.");
            }
        } catch (SQLException ex) { showError("DB Error: " + ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 6 – HISTORY
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildHistoryPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 12));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"From", "Amount", "To", "Converted", "Rate", "Time"};
        tmHistory = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblHistory = styledTable(tmHistory);
        JScrollPane sp = styledScroll(tblHistory);

        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(sectionTitle("Conversion History"));
        top.add(label("Last"));
        tfHistoryLimit = styledField("20");
        tfHistoryLimit.setPreferredSize(new Dimension(60, 32));
        top.add(tfHistoryLimit);
        top.add(label("records"));

        JButton load  = accentButton("Load");
        JButton clear = dangerButton("Clear All");
        load.addActionListener(e  -> loadHistory());
        clear.addActionListener(e -> doClearHistory());

        JPanel btns = darkPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(load);
        btns.add(clear);

        JPanel header = darkPanel(new BorderLayout());
        header.add(top,  BorderLayout.WEST);
        header.add(btns, BorderLayout.EAST);

        p.add(header, BorderLayout.NORTH);
        p.add(sp,     BorderLayout.CENTER);
        return p;
    }

    private void loadHistory() {
        try {
            int limit = Integer.parseInt(tfHistoryLimit.getText().trim());
            List<ConversionHistory> list = service.getRecentHistory(limit);
            tmHistory.setRowCount(0);
            for (ConversionHistory h : list)
                tmHistory.addRow(new Object[]{
                    h.getFromCurrency(), String.format("%.4f", h.getAmount()),
                    h.getToCurrency(),   String.format("%.4f", h.getConvertedAmount()),
                    String.format("%.6f", h.getExchangeRate()), h.getConversionTime()});
        } catch (NumberFormatException ex) {
            showError("Enter a valid integer for the limit.");
        } catch (SQLException ex) { showError("DB Error: " + ex.getMessage()); }
    }

    private void doClearHistory() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear ALL conversion history?",
            "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            service.clearHistory();
            tmHistory.setRowCount(0);
            showSuccess("History cleared.");
        } catch (SQLException ex) { showError("DB Error: " + ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TAB 7 – SEARCH HISTORY
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildSearchPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 12));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] cols = {"From", "Amount", "To", "Converted", "Rate", "Time"};
        tmSearch = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSearchResult = styledTable(tmSearch);
        JScrollPane sp = styledScroll(tblSearchResult);

        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(sectionTitle("Search History"));
        top.add(label("From"));
        tfSearchFrom = styledField("");
        tfSearchFrom.setPreferredSize(new Dimension(70, 32));
        top.add(tfSearchFrom);
        top.add(label("To"));
        tfSearchTo = styledField("");
        tfSearchTo.setPreferredSize(new Dimension(70, 32));
        top.add(tfSearchTo);
        JButton btn = accentButton("Search");
        btn.addActionListener(e -> doSearchHistory());
        top.add(btn);

        p.add(top, BorderLayout.NORTH);
        p.add(sp,  BorderLayout.CENTER);
        return p;
    }

    private void doSearchHistory() {
        try {
            String from = tfSearchFrom.getText().trim().toUpperCase();
            String to   = tfSearchTo.getText().trim().toUpperCase();
            if (from.isEmpty() || to.isEmpty()) { showError("Enter both currency codes."); return; }
            List<ConversionHistory> list = service.getHistoryByPair(from, to);
            tmSearch.setRowCount(0);
            for (ConversionHistory h : list)
                tmSearch.addRow(new Object[]{
                    h.getFromCurrency(), String.format("%.4f", h.getAmount()),
                    h.getToCurrency(),   String.format("%.4f", h.getConvertedAmount()),
                    String.format("%.6f", h.getExchangeRate()), h.getConversionTime()});
            if (list.isEmpty())
                showSuccess("No history found for " + from + " → " + to + ".");
        } catch (SQLException ex) { showError("DB Error: " + ex.getMessage()); }
    }

    // ── Utility: refresh combos ──────────────────────────────────────────
    private void refreshCurrencyComboBoxes() {
        try {
            List<Currency> list = service.getAllCurrencies();
            String selFrom = (String) cbFrom.getSelectedItem();
            String selTo   = (String) cbTo.getSelectedItem();
            cbFrom.removeAllItems();
            cbTo.removeAllItems();
            for (Currency c : list) {
                cbFrom.addItem(c.getCode());
                cbTo.addItem(c.getCode());
            }
            if (selFrom != null) cbFrom.setSelectedItem(selFrom);
            if (selTo   != null) cbTo.setSelectedItem(selTo);
        } catch (SQLException ignored) {}
    }

    // ── UI factory helpers ───────────────────────────────────────────────
    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG_DARK);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField(placeholder, 16);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_LIGHT);
        tf.setFont(FONT_LABEL);
        tf.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    private JComboBox<String> styledCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_LABEL);
        cb.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        return cb;
    }

    private JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_LIGHT); }
            public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }
        });
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(DANGER);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(220, 38, 38)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(DANGER); }
        });
        return b;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_PRIMARY);
        t.setFont(FONT_LABEL);
        t.setGridColor(BORDER_COLOR);
        t.setSelectionBackground(ACCENT);
        t.setSelectionForeground(Color.WHITE);
        t.setRowHeight(30);
        t.setShowGrid(true);
        t.setFillsViewportHeight(true);

        JTableHeader h = t.getTableHeader();
        h.setBackground(BG_INPUT);
        h.setForeground(TEXT_MUTED);
        h.setFont(FONT_BOLD);
        h.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < model.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(center);

        return t;
    }

    private JScrollPane styledScroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_DARK);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(new LineBorder(BORDER_COLOR, 1));
        return sp;
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(8, 8, 8, 8);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.gridx   = 0;
        return g;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Entry point ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(CurrencyConverterUI::new);
    }
}

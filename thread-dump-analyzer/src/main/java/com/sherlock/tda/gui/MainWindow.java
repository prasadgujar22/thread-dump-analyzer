package com.sherlock.tda.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.sherlock.tda.analysis.DumpAnalyzer;
import com.sherlock.tda.analysis.DumpAnalyzer.ThreadStateTransition;
import com.sherlock.tda.model.*;
import com.sherlock.tda.parser.ThreadDumpParser;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import org.jfree.chart.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;

public class MainWindow extends JFrame {
    
    private final DefaultListModel<ThreadDump> dumpListModel = new DefaultListModel<>();
    private final JList<ThreadDump> dumpList = new JList<>(dumpListModel);
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JLabel statusLabel = new JLabel("Ready");
    private boolean darkMode = true;
    
    private ThreadDump currentDump = null;

    public MainWindow() {
        setTitle("Sherlock Thread Dump Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 950);
        setLocationRelativeTo(null);
        
        initComponents();
        initMenuBar();
        initToolbar();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        
        // Left panel - dump list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(new TitledBorder("Thread Dumps"));
        
        dumpList.setCellRenderer(new DumpListRenderer());
        dumpList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDump(dumpList.getSelectedValue());
            }
        });
        
        JScrollPane listScroll = new JScrollPane(dumpList);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton addBtn = new JButton("Add Dump");
        JButton compareBtn = new JButton("Compare (2)");
        JButton multiCompareBtn = new JButton("Compare (3+)");
        addBtn.addActionListener(e -> loadDumpFile());
        compareBtn.addActionListener(e -> showCompareDialog());
        multiCompareBtn.addActionListener(e -> showMultiCompareDialog());
        buttonPanel.add(addBtn);
        buttonPanel.add(compareBtn);
        buttonPanel.add(multiCompareBtn);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(leftPanel, BorderLayout.WEST);
        
        // Center - tabbed pane
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Dump...");
        JMenuItem exitItem = new JMenuItem("Exit");
        openItem.addActionListener(e -> loadDumpFile());
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        JMenu viewMenu = new JMenu("View");
        JMenuItem themeItem = new JMenuItem("Toggle Dark/Light");
        themeItem.addActionListener(e -> toggleTheme());
        viewMenu.add(themeItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton openBtn = new JButton("Open");
        JButton analyzeBtn = new JButton("Analyze");
        JButton exportBtn = new JButton("Export");
        
        openBtn.addActionListener(e -> loadDumpFile());
        analyzeBtn.addActionListener(e -> analyzeCurrentDump());
        exportBtn.addActionListener(e -> exportReport());
        
        toolBar.add(openBtn);
        toolBar.add(analyzeBtn);
        toolBar.addSeparator();
        toolBar.add(exportBtn);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void loadDumpFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Thread Dumps", "txt", "log", "tdump"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ThreadDump dump = ThreadDumpParser.parseFile(chooser.getSelectedFile().toPath());
                dumpListModel.addElement(dump);
                statusLabel.setText("Loaded: " + dump.getFileName() + " (" + dump.getThreads().size() + " threads)");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error parsing dump: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadDump(ThreadDump dump) {
        if (dump == null) return;
        currentDump = dump;
        
        tabbedPane.removeAll();
        
        // Overview tab with charts
        tabbedPane.addTab("Overview", createOverviewPanel(dump));
        
        // Charts tab
        tabbedPane.addTab("Charts", createChartsPanel(dump));
        
        // Threads tab
        tabbedPane.addTab("Threads", createThreadsPanel(dump));
        
        // States tab
        tabbedPane.addTab("States", createStatesPanel(dump));
        
        // Long Running tab
        tabbedPane.addTab("Long Running", createLongRunningPanel(dump));
        
        // Deadlocks tab
        if (!dump.getDeadlocks().isEmpty()) {
            tabbedPane.addTab("Deadlocks (" + dump.getDeadlocks().size() + ")", 
                createDeadlocksPanel(dump));
        }
        
        // Hot Methods tab
        tabbedPane.addTab("Hot Methods", createHotMethodsPanel(dump));
        
        // Identical Stacks tab
        tabbedPane.addTab("Identical Stacks", createIdenticalStacksPanel(dump));
        
        revalidate();
        repaint();
    }
    
    // ===== CHARTS PANEL =====
    private JPanel createChartsPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 1. Thread States Pie Chart
        panel.add(createThreadStatePieChart(dump));
        
        // 2. Stack Depth Distribution Pie Chart
        panel.add(createStackDepthPieChart(dump));
        
        // 3. Thread Name Groups Bar Chart
        panel.add(createThreadNameGroupsChart(dump));
        
        // 4. Pool Distribution Bar Chart
        panel.add(createPoolDistributionChart(dump));
        
        return panel;
    }
    
    private JPanel createThreadStatePieChart(ThreadDump dump) {
        Map<ThreadState, Long> states = DumpAnalyzer.analyzeStates(dump);
        DefaultPieDataset dataset = new DefaultPieDataset();
        states.forEach((state, count) -> dataset.setValue(state.getLabel(), count));
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Thread States", dataset, true, true, false);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }
    
    private JPanel createStackDepthPieChart(ThreadDump dump) {
        Map<String, Long> distribution = DumpAnalyzer.analyzeStackDepthDistribution(dump);
        DefaultPieDataset dataset = new DefaultPieDataset();
        distribution.forEach((range, count) -> {
            if (count > 0) dataset.setValue(range, count);
        });
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Stack Depth Distribution", dataset, true, true, false);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }
    
    private JPanel createThreadNameGroupsChart(ThreadDump dump) {
        Map<String, Long> groups = DumpAnalyzer.analyzeThreadNameGroups(dump);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        groups.forEach((group, count) -> dataset.addValue(count, "Threads", group));
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Thread Name Groups", "Group", "Count", dataset,
            PlotOrientation.HORIZONTAL, false, true, false);
        
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(70, 130, 180));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }
    
    private JPanel createPoolDistributionChart(ThreadDump dump) {
        Map<String, Long> pools = dump.getThreadsByPool();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        pools.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> dataset.addValue(e.getValue(), "Threads", e.getKey()));
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Top 10 Thread Pools", "Pool", "Count", dataset,
            PlotOrientation.HORIZONTAL, false, true, false);
        
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(60, 179, 113));
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }
    
    // ===== OVERVIEW PANEL =====
    private JPanel createOverviewPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Info cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        cardsPanel.add(createCard("File", dump.getFileName()));
        cardsPanel.add(createCard("Server", dump.getServerType()));
        cardsPanel.add(createCard("JVM", dump.getJvmVersion()));
        cardsPanel.add(createCard("Total Threads", String.valueOf(dump.getThreads().size())));
        cardsPanel.add(createCard("Deadlocks", String.valueOf(dump.getDeadlocks().size())));
        cardsPanel.add(createCard("Daemon Threads", 
            String.valueOf(dump.getThreads().stream().filter(ThreadInfo::isDaemon).count())));
        
        panel.add(cardsPanel, BorderLayout.NORTH);
        
        // Summary text
        JTextArea summaryArea = new JTextArea(DumpAnalyzer.generateSummary(dump));
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summaryArea.setEditable(false);
        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    // ===== THREADS PANEL =====
    private JPanel createThreadsPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"TID", "Name", "State", "Pool", "Stack Depth", "Daemon"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        
        for (ThreadInfo thread : dump.getThreads()) {
            model.addRow(new Object[]{
                thread.getId(),
                thread.getName(),
                thread.getState().getIcon() + " " + thread.getState().getLabel(),
                thread.getPoolName(),
                thread.getStackTrace().size(),
                thread.isDaemon() ? "Yes" : "No"
            });
        }
        
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        
        // Color code states
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String stateStr = (String) table.getValueAt(row, 3);
                if (!isSelected) {
                    if (stateStr.contains("BLOCKED")) c.setBackground(new Color(255, 200, 200));
                    else if (stateStr.contains("WAITING")) c.setBackground(new Color(255, 255, 200));
                    else if (stateStr.contains("RUNNABLE")) c.setBackground(new Color(200, 255, 200));
                    else c.setBackground(table.getBackground());
                }
                return c;
            }
        });
        
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        // Detail panel for selected thread
        JTextArea detailArea = new JTextArea(10, 50);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        detailArea.setEditable(false);
        
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                ThreadInfo thread = dump.getThreads().get(modelRow);
                detailArea.setText(formatThreadDetail(thread));
            }
        });
        
        panel.add(new JScrollPane(detailArea), BorderLayout.SOUTH);
        return panel;
    }
    
    // ===== LONG RUNNING PANEL =====
    private JPanel createLongRunningPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Show info about multi-dump requirement
        JLabel infoLabel = new JLabel(
            "Proper long-running detection requires 2+ dumps (5-10s apart). " +
            "TID is used for identification. Threads must be RUNNABLE with same top frame in ALL dumps.");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.NORTH);
        
        // Results table
        String[] columns = {"TID", "Name", "State", "Pool", "Stack Depth", "Top Method"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        
        // If we have multiple dumps loaded, run proper multi-dump detection
        if (dumpListModel.size() >= 2) {
            List<ThreadDump> allDumps = new ArrayList<>();
            for (int i = 0; i < dumpListModel.size(); i++) {
                allDumps.add(dumpListModel.getElementAt(i));
            }
            List<ThreadInfo> longRunning = DumpAnalyzer.findLongRunningThreads(allDumps);
            for (ThreadInfo thread : longRunning) {
                String topMethod = thread.getStackTrace().isEmpty() ? "N/A" :
                    thread.getStackTrace().get(0).toString();
                model.addRow(new Object[]{
                    thread.getId(),
                    thread.getName(),
                    thread.getState().getLabel() + " (all dumps)",
                    thread.getPoolName(),
                    thread.getStackTrace().size(),
                    topMethod
                });
            }
            infoLabel.setText("Detected " + longRunning.size() + 
                " long-running threads across " + allDumps.size() + " dumps (RUNNABLE + same top frame, TID-based).");
        } else {
            // Fallback: show deep stack threads for single dump
            infoLabel.setText("Only 1 dump loaded. Showing deep stack threads (heuristic). Load 2+ dumps for proper detection.");
            List<ThreadInfo> deepStacks = DumpAnalyzer.findLongRunningThreads(dump, 10);
            for (ThreadInfo thread : deepStacks) {
                String topMethod = thread.getStackTrace().isEmpty() ? "N/A" :
                    thread.getStackTrace().get(0).toString();
                model.addRow(new Object[]{
                    thread.getId(),
                    thread.getName(),
                    thread.getState().getLabel(),
                    thread.getPoolName(),
                    thread.getStackTrace().size(),
                    topMethod
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        return panel;
    }
    
    // ===== STATES PANEL =====
    private JPanel createStatesPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // State distribution table
        Map<ThreadState, Long> states = DumpAnalyzer.analyzeStates(dump);
        String[] cols = {"State", "Count", "Percentage"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        long total = dump.getThreads().size();
        
        for (ThreadState state : ThreadState.values()) {
            long count = states.getOrDefault(state, 0L);
            if (count > 0) {
                model.addRow(new Object[]{
                    state.getIcon() + " " + state.getLabel(),
                    count,
                    String.format("%.1f%%", (count * 100.0) / total)
                });
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(28);
        panel.add(new JScrollPane(table));
        
        // Pool distribution
        Map<String, Long> pools = dump.getThreadsByPool();
        String[] poolCols = {"Pool", "Thread Count"};
        DefaultTableModel poolModel = new DefaultTableModel(poolCols, 0);
        pools.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> poolModel.addRow(new Object[]{e.getKey(), e.getValue()}));
        
        JTable poolTable = new JTable(poolModel);
        poolTable.setRowHeight(25);
        panel.add(new JScrollPane(poolTable));
        
        return panel;
    }
    
    // ===== DEADLOCKS PANEL =====
    private JPanel createDeadlocksPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setForeground(Color.RED);
        
        StringBuilder sb = new StringBuilder();
        for (DeadlockInfo dl : dump.getDeadlocks()) {
            sb.append("DEADLOCK DETECTED\n");
            sb.append("=================\n");
            sb.append("Involved Threads:\n");
            for (String t : dl.getInvolvedThreads()) {
                sb.append("  - ").append(t).append("\n");
            }
            sb.append("\n");
        }
        area.setText(sb.toString());
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }
    
    // ===== HOT METHODS PANEL =====
    private JPanel createHotMethodsPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout());
        List<String> hotMethods = DumpAnalyzer.detectHotMethods(dump, 20);
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        hotMethods.forEach(listModel::addElement);
        
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }
    
    // ===== IDENTICAL STACKS PANEL =====
    private JPanel createIdenticalStacksPanel(ThreadDump dump) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        Map<String, List<ThreadInfo>> identical = DumpAnalyzer.findIdenticalStacks(dump);
        
        String[] columns = {"Stack Signature", "Thread Count", "TIDs"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        
        for (Map.Entry<String, List<ThreadInfo>> entry : identical.entrySet()) {
            List<ThreadInfo> threads = entry.getValue();
            StringBuilder tids = new StringBuilder();
            for (int i = 0; i < Math.min(threads.size(), 5); i++) {
                if (i > 0) tids.append(", ");
                tids.append(threads.get(i).getId());
            }
            if (threads.size() > 5) tids.append("...");
            
            // Shorten stack signature for display
            String sig = entry.getKey().split("\n")[0];
            if (sig.length() > 80) sig = sig.substring(0, 80) + "...";
            
            model.addRow(new Object[]{sig, threads.size(), tids.toString()});
        }
        
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JLabel infoLabel = new JLabel("Threads sharing identical call stacks indicate contention or thread pool saturation.");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.NORTH);
        
        return panel;
    }
    
    // ===== COMPARISON DIALOGS =====
    private void showCompareDialog() {
        if (dumpListModel.size() < 2) {
            JOptionPane.showMessageDialog(this, 
                "Need at least 2 dumps for comparison", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JComboBox<ThreadDump> baseCombo = new JComboBox<>();
        JComboBox<ThreadDump> compareCombo = new JComboBox<>();
        for (int i = 0; i < dumpListModel.size(); i++) {
            baseCombo.addItem(dumpListModel.getElementAt(i));
            compareCombo.addItem(dumpListModel.getElementAt(i));
        }
        
        JPanel dialogPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        dialogPanel.add(new JLabel("Baseline:"));
        dialogPanel.add(baseCombo);
        dialogPanel.add(new JLabel("Compare To:"));
        dialogPanel.add(compareCombo);
        
        if (JOptionPane.showConfirmDialog(this, dialogPanel, "Compare Dumps", 
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            ThreadDump base = (ThreadDump) baseCombo.getSelectedItem();
            ThreadDump compare = (ThreadDump) compareCombo.getSelectedItem();
            
            if (base != null && compare != null && base != compare) {
                ComparisonResult result = DumpAnalyzer.compare(base, compare);
                showComparisonResult(result);
            }
        }
    }
    
    private void showMultiCompareDialog() {
        if (dumpListModel.size() < 3) {
            JOptionPane.showMessageDialog(this, 
                "Need at least 3 dumps for multi-comparison", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JPanel selectionPanel = new JPanel(new GridLayout(0, 1));
        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (int i = 0; i < dumpListModel.size(); i++) {
            JCheckBox cb = new JCheckBox(dumpListModel.getElementAt(i).getFileName());
            cb.putClientProperty("dump", dumpListModel.getElementAt(i));
            checkBoxes.add(cb);
            selectionPanel.add(cb);
        }
        
        JScrollPane scrollPane = new JScrollPane(selectionPanel);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        if (JOptionPane.showConfirmDialog(this, scrollPane, "Select 3+ Dumps to Compare", 
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            
            List<ThreadDump> selected = new ArrayList<>();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    selected.add((ThreadDump) cb.getClientProperty("dump"));
                }
            }
            
            if (selected.size() >= 3) {
                MultiComparisonResult result = DumpAnalyzer.compareMultiple(selected);
                showMultiComparisonResult(result);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please select at least 3 dumps", "Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    private void showComparisonResult(ComparisonResult result) {
        JDialog dialog = new JDialog(this, "Comparison Results", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextArea summary = new JTextArea();
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summary.setEditable(false);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Comparison: ").append(result.getBaseline().getFileName())
          .append(" vs ").append(result.getCompareTo().getFileName()).append("\n\n");
        
        sb.append("New Threads: ").append(result.getNewThreads().size()).append("\n");
        sb.append("Removed Threads: ").append(result.getRemovedThreads().size()).append("\n");
        sb.append("State Changes: ").append(
            result.getChanges().stream().filter(c -> c.getType() == ComparisonResult.ChangeType.STATE_CHANGED).count()
        ).append("\n\n");
        
        sb.append("State Delta:\n");
        result.getStateDelta().forEach((state, delta) -> {
            if (delta != 0) {
                sb.append("  ").append(state.getLabel()).append(": ")
                  .append(delta > 0 ? "+" : "").append(delta).append("\n");
            }
        });
        
        if (!result.getNewThreads().isEmpty()) {
            sb.append("\nNew Threads:\n");
            result.getNewThreads().forEach(t -> sb.append("  + ").append(t).append("\n"));
        }
        
        if (!result.getRemovedThreads().isEmpty()) {
            sb.append("\nRemoved Threads:\n");
            result.getRemovedThreads().forEach(t -> sb.append("  - ").append(t).append("\n"));
        }
        
        summary.setText(sb.toString());
        panel.add(new JScrollPane(summary), BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showMultiComparisonResult(MultiComparisonResult result) {
        JDialog dialog = new JDialog(this, "Multi-Dump Comparison Results", true);
        dialog.setSize(1200, 800);
        dialog.setLocationRelativeTo(this);
        
        JTabbedPane tabs = new JTabbedPane();
        
        // Summary tab
        JTextArea summary = new JTextArea();
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summary.setEditable(false);
        summary.setText(DumpAnalyzer.generateMultiSummary(result));
        tabs.addTab("Summary", new JScrollPane(summary));
        
        // Persistent threads tab
        String[] cols = {"TID", "Name", "State", "Pool"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (ThreadInfo t : result.getPersistentThreads()) {
            model.addRow(new Object[]{t.getId(), t.getName(), t.getState().getLabel(), 
                t.getPoolName()});
        }
        tabs.addTab("Persistent Threads (" + result.getPersistentThreads().size() + ")", 
            new JScrollPane(new JTable(model)));
        
        // Long-running threads tab
        String[] lrCols = {"TID", "Name", "Pool", "Top Method", "Stack Depth"};
        DefaultTableModel lrModel = new DefaultTableModel(lrCols, 0);
        for (ThreadInfo t : result.getLongRunningThreads()) {
            String topMethod = t.getStackTrace().isEmpty() ? "N/A" :
                t.getStackTrace().get(0).toString();
            lrModel.addRow(new Object[]{t.getId(), t.getName(), t.getPoolName(), 
                topMethod, t.getStackTrace().size()});
        }
        tabs.addTab("Long Running (" + result.getLongRunningThreads().size() + ")", 
            new JScrollPane(new JTable(lrModel)));
        
        // State transitions tab
        String[] transCols = {"TID", "Thread Name", "From State", "To State", "Dump Index", "Pool"};
        DefaultTableModel transModel = new DefaultTableModel(transCols, 0);
        for (ThreadStateTransition trans : result.getStateTransitions()) {
            transModel.addRow(new Object[]{trans.getTid(), trans.getThreadName(),
                trans.getFromState().getLabel(), trans.getToState().getLabel(),
                trans.getDumpIndex(), trans.getPoolName()});
        }
        tabs.addTab("State Transitions (" + result.getStateTransitions().size() + ")", 
            new JScrollPane(new JTable(transModel)));
        
        // State history chart
        tabs.addTab("State History Chart", createStateHistoryChart(result));
        
        dialog.add(tabs);
        dialog.setVisible(true);
    }
    
    private JPanel createStateHistoryChart(MultiComparisonResult result) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        List<ThreadDump> dumps = result.getDumps();
        for (Map.Entry<ThreadState, List<Long>> entry : result.getStateHistory().entrySet()) {
            ThreadState state = entry.getKey();
            List<Long> counts = entry.getValue();
            for (int i = 0; i < counts.size(); i++) {
                dataset.addValue(counts.get(i), state.getLabel(), 
                    "Dump " + (i + 1) + "\n" + dumps.get(i).getFileName());
            }
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
            "Thread State History Across Dumps", "Dump", "Thread Count", dataset,
            PlotOrientation.VERTICAL, true, true, false);
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 500));
        return chartPanel;
    }
    
    // ===== ACTIONS =====
    private void analyzeCurrentDump() {
        if (currentDump == null) return;
        JOptionPane.showMessageDialog(this, 
            DumpAnalyzer.generateSummary(currentDump),
            "Analysis Summary", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportReport() {
        if (currentDump == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("report.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(chooser.getSelectedFile().toPath(), 
                    DumpAnalyzer.generateSummary(currentDump).getBytes());
                statusLabel.setText("Report exported to " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }
    
    private void toggleTheme() {
        darkMode = !darkMode;
        try {
            if (darkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "Sherlock Thread Dump Analyzer v2.0\n" +
            "TID-based cross-dump comparison\n" +
            "Charts: Thread States, Stack Depth, Name Groups, Pool Distribution\n" +
            "Supports: HotSpot JDK 8/11/21, WebLogic, WebSphere, Tomcat\n" +
            "Built with FlatLaf + JFreeChart + Swing",
            "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private JPanel createCard(String title, String value) {
        JPanel card = new JPanel();
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        card.add(titleLabel, BorderLayout.NORTH);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private String formatThreadDetail(ThreadInfo thread) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(thread.getName()).append("\n");
        sb.append("TID: ").append(thread.getId()).append("\n");
        sb.append("State: ").append(thread.getState().getLabel()).append("\n");
        sb.append("Pool: ").append(thread.getPoolName()).append("\n");
        sb.append("Daemon: ").append(thread.isDaemon()).append("\n");
        sb.append("Priority: ").append(thread.getPriority()).append("\n\n");
        sb.append("Stack Trace (").append(thread.getStackTrace().size()).append(" frames):\n");
        for (ThreadInfo.StackFrame frame : thread.getStackTrace()) {
            sb.append("  at ").append(frame.toString()).append("\n");
        }
        return sb.toString();
    }
    
    private static class DumpListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ThreadDump) {
                ThreadDump dump = (ThreadDump) value;
                setText(dump.getFileName() + " (" + dump.getThreads().size() + " threads)");
                setIcon(dump.getDeadlocks().isEmpty() ? null : UIManager.getIcon("OptionPane.warningIcon"));
            }
            return this;
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}

package cn.ltssil.linux.ui;

import cn.ltssil.linux.model.ProcessInfo;
import cn.ltssil.linux.service.ProcessService;
import cn.ltssil.linux.util.LogUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainFrame extends JFrame {

    private final ProcessService processService = new ProcessService();

    private JLabel cpuLabel;
    private JLabel memoryLabel;
    private JLabel timeLabel;
    private JLabel processCountLabel;
    private JLabel statusLabel;

    private JTable processTable;
    private DefaultTableModel tableModel;
    private JTextField pidField;
    private JTextField nameField;
    private JComboBox<String> sortBox;

    private JButton searchButton;
    private JButton refreshButton;
    private JButton killButton;
    private JButton exportButton;
    private JButton sortButton;

    private SwingWorker<List<ProcessInfo>, Void> loadingWorker;

    public MainFrame() {
        initFrame();
        initComponents();
        loadProcessDataAsync();
        setVisible(true);
        startAutoRefresh();
    }

    private void initFrame() {
        setTitle("Linux Task Manager");
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));
    }

    private void initComponents() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        rootPanel.setOpaque(false);
        add(rootPanel, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 10));
        infoPanel.setBackground(new Color(232, 242, 255));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(196, 214, 235)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        cpuLabel = createInfoLabel();
        memoryLabel = createInfoLabel();
        processCountLabel = createInfoLabel();
        timeLabel = createInfoLabel();

        infoPanel.add(cpuLabel);
        infoPanel.add(memoryLabel);
        infoPanel.add(processCountLabel);
        infoPanel.add(timeLabel);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        controlPanel.setOpaque(false);

        JLabel pidTextLabel = new JLabel("PID:");
        pidField = new JTextField(10);
        pidField.putClientProperty("JTextField.placeholderText", "输入 PID");

        JLabel nameTextLabel = new JLabel("进程名:");
        nameField = new JTextField(14);
        nameField.putClientProperty("JTextField.placeholderText", "输入进程名");

        searchButton = new JButton("搜索");

        sortBox = new JComboBox<>(new String[]{
                "PID升序",
                "PID降序",
                "CPU降序",
                "内存降序",
                "名称升序"
        });
        sortButton = new JButton("排序");

        controlPanel.add(pidTextLabel);
        controlPanel.add(pidField);
        controlPanel.add(nameTextLabel);
        controlPanel.add(nameField);
        controlPanel.add(searchButton);
        controlPanel.add(Box.createHorizontalStrut(16));
        controlPanel.add(new JLabel("排序方式:"));
        controlPanel.add(sortBox);
        controlPanel.add(sortButton);

        northPanel.add(infoPanel);
        northPanel.add(Box.createVerticalStrut(8));
        northPanel.add(controlPanel);

        rootPanel.add(northPanel, BorderLayout.NORTH);

        String[] columns = {"PID", "进程名称", "命令路径", "CPU(%)", "内存(MB)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        processTable = new JTable(tableModel);
        processTable.setRowHeight(30);
        processTable.setAutoCreateRowSorter(true);
        processTable.setFillsViewportHeight(true);
        processTable.setGridColor(new Color(225, 228, 232));
        processTable.setShowGrid(true);
        processTable.setSelectionBackground(new Color(0, 120, 215));
        processTable.setSelectionForeground(Color.WHITE);
        processTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        processTable.getTableHeader().setReorderingAllowed(false);

        processTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? Color.WHITE : new Color(248, 250, 252));
                }

                if (column == 0 || column == 3 || column == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        setColumnWidth(processTable, 0, 90);
        setColumnWidth(processTable, 1, 180);
        setColumnWidth(processTable, 2, 560);
        setColumnWidth(processTable, 3, 90);
        setColumnWidth(processTable, 4, 100);

        processTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = processTable.rowAtPoint(e.getPoint());
                    if (viewRow >= 0) {
                        showProcessDetail(viewRow);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(processTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        buttonPanel.setOpaque(false);

        refreshButton = new JButton("刷新");
        killButton = new JButton("结束进程");
        exportButton = new JButton("导出日志");

        buttonPanel.add(refreshButton);
        buttonPanel.add(killButton);
        buttonPanel.add(exportButton);

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        rootPanel.add(bottomPanel, BorderLayout.SOUTH);

        styleButton(searchButton, new Color(70, 130, 180));
        styleButton(refreshButton, new Color(70, 130, 180));
        styleButton(exportButton, new Color(70, 130, 180));
        styleButton(sortButton, new Color(70, 130, 180));
        styleButton(killButton, new Color(220, 53, 69));

        searchButton.addActionListener(e -> searchProcessAsync());
        refreshButton.addActionListener(e -> loadProcessDataAsync());
        killButton.addActionListener(e -> killSelectedProcess());
        exportButton.addActionListener(e -> exportLog());
        sortButton.addActionListener(e -> loadProcessDataAsync());
    }

    private JLabel createInfoLabel() {
        JLabel label = new JLabel("...");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        return label;
    }

    private void setColumnWidth(JTable table, int columnIndex, int width) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(width);
        column.setMinWidth(Math.min(width, 60));
    }

    private void updateSystemInfo(long processCount) {
        Runtime runtime = Runtime.getRuntime();

        int cpuCount = runtime.availableProcessors();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long totalMemoryMb = runtime.totalMemory() / 1024 / 1024;

        cpuLabel.setText("CPU核心数: " + cpuCount);
        memoryLabel.setText("JVM内存: " + usedMemoryMb + "MB / " + totalMemoryMb + "MB");
        processCountLabel.setText("进程数: " + processCount);
        timeLabel.setText("时间: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ));
    }

    private void loadProcessDataAsync() {
        searchProcessAsync();
    }

    private void searchProcessAsync() {
        if (loadingWorker != null && !loadingWorker.isDone()) {
            loadingWorker.cancel(true);
        }

        final String pidText = pidField.getText().trim();
        final String nameText = nameField.getText().trim().toLowerCase();

        if (pidText.isEmpty() && nameText.isEmpty()) {
            setStatus("正在加载进程数据...");
        } else {
            setStatus("正在搜索进程数据...");
        }

        final Long pidFilter;
        if (!pidText.isEmpty()) {
            try {
                pidFilter = Long.parseLong(pidText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "PID必须是数字");
                return;
            }
        } else {
            pidFilter = null;
        }

        loadingWorker = new SwingWorker<>() {
            @Override
            protected List<ProcessInfo> doInBackground() {
                List<ProcessInfo> processes = processService.getAllProcesses();
                List<ProcessInfo> matched = new ArrayList<>();

                for (ProcessInfo process : processes) {
                    boolean pidMatch = pidFilter == null || process.getPid() == pidFilter;
                    boolean nameMatch = nameText.isEmpty()
                            || process.getName().toLowerCase().contains(nameText)
                            || process.getCommand().toLowerCase().contains(nameText);

                    if (pidMatch && nameMatch) {
                        matched.add(process);
                    }
                }

                return sortProcesses(matched);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    List<ProcessInfo> processes = get();
                    fillTable(processes);
                    updateSystemInfo(processes.size());

                    if (pidText.isEmpty() && nameText.isEmpty()) {
                        setStatus("已加载 " + processes.size() + " 个进程");
                    } else {
                        setStatus("已找到 " + processes.size() + " 个匹配进程");
                    }

                    if (!pidText.isEmpty() || !nameText.isEmpty()) {
                        if (processes.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                    MainFrame.this,
                                    "没有找到匹配的进程"
                            );
                        }
                    }
                } catch (Exception ex) {
                    setStatus("加载失败");
                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "刷新失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        loadingWorker.execute();
    }

    private void fillTable(List<ProcessInfo> processes) {
        tableModel.setRowCount(0);

        for (ProcessInfo process : processes) {
            addProcessToTable(process);
        }
    }

    private void addProcessToTable(ProcessInfo process) {
        tableModel.addRow(new Object[]{
                process.getPid(),
                process.getName(),
                process.getCommand(),
                formatCpu(process.getCpuUsage()),
                process.getMemoryMb()
        });
    }

    private List<ProcessInfo> sortProcesses(List<ProcessInfo> processes) {
        String mode = (String) sortBox.getSelectedItem();

        if (mode == null) {
            return processes;
        }

        return switch (mode) {
            case "PID降序" -> processService.sortByPidDesc(processes);
            case "CPU降序" -> processService.sortByCpuDesc(processes);
            case "内存降序" -> processService.sortByMemoryDesc(processes);
            case "名称升序" -> processService.sortByNameAsc(processes);
            default -> processService.sortByPidAsc(processes);
        };
    }

    private void showProcessDetail(int viewRow) {
        int modelRow = processTable.convertRowIndexToModel(viewRow);

        Object pidObj = tableModel.getValueAt(modelRow, 0);
        Object nameObj = tableModel.getValueAt(modelRow, 1);
        Object commandObj = tableModel.getValueAt(modelRow, 2);
        Object cpuObj = tableModel.getValueAt(modelRow, 3);
        Object memoryObj = tableModel.getValueAt(modelRow, 4);

        StringBuilder sb = new StringBuilder();
        sb.append("PID：").append(pidObj).append("\n\n");
        sb.append("进程名称：").append(nameObj).append("\n\n");
        sb.append("命令路径：").append(commandObj).append("\n\n");
        sb.append("CPU占用：").append(cpuObj).append("%\n\n");
        sb.append("内存占用：").append(memoryObj).append(" MB");

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        area.setBackground(new Color(250, 250, 250));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(620, 320));

        JOptionPane.showMessageDialog(
                this,
                pane,
                "进程详情",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void killSelectedProcess() {
        int row = processTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选择一个进程");
            return;
        }

        int modelRow = processTable.convertRowIndexToModel(row);
        long pid = Long.parseLong(tableModel.getValueAt(modelRow, 0).toString());

        int result = JOptionPane.showConfirmDialog(
                this,
                "确定结束 PID = " + pid + " ?",
                "确认",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success = processService.killProcess(pid);

        if (success) {
            JOptionPane.showMessageDialog(this, "结束成功");
            loadProcessDataAsync();
        } else {
            JOptionPane.showMessageDialog(this, "结束失败");
        }
    }

    private void exportLog() {
        try {
            LogUtil.export(processService.getAllProcesses());
            JOptionPane.showMessageDialog(this, "导出成功\nprocess_log.txt");
            setStatus("已导出日志");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "导出失败\n" + e.getMessage());
            setStatus("导出失败");
        }
    }

    private void startAutoRefresh() {
        Timer timer = new Timer(1000, e -> loadProcessDataAsync());
        timer.start();
    }

    private void styleButton(JButton button, Color background) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(112, 36));
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private String formatCpu(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
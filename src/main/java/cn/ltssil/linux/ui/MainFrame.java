package cn.ltssil.linux.ui;

import cn.ltssil.linux.model.ProcessInfo;
import cn.ltssil.linux.service.ProcessService;
import cn.ltssil.linux.util.LogUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {

    private final ProcessService processService = new ProcessService();

    private JLabel cpuLabel;
    private JLabel memoryLabel;
    private JLabel timeLabel;
    private JLabel processCountLabel;

    private JTable processTable;
    private DefaultTableModel tableModel;
    private JTextField pidField;
    private JComboBox<String> sortBox;

    private JButton searchButton;
    private JButton refreshButton;
    private JButton killButton;
    private JButton exportButton;
    private JButton sortButton;

    public MainFrame() {
        initFrame();
        initComponents();
        loadProcessData();
        setVisible(true);
        startAutoRefresh();
    }

    private void initFrame() {
        setTitle("Linux Task Manager");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void initComponents() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        cpuLabel = new JLabel();
        memoryLabel = new JLabel();
        processCountLabel = new JLabel();
        timeLabel = new JLabel();

        infoPanel.add(cpuLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(memoryLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(processCountLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(timeLabel);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        pidField = new JTextField(12);
        searchButton = new JButton("查询PID");

        sortBox = new JComboBox<>(new String[]{
                "PID升序",
                "PID降序",
                "CPU降序",
                "内存降序",
                "名称升序"
        });
        sortButton = new JButton("排序");

        controlPanel.add(new JLabel("PID:"));
        controlPanel.add(pidField);
        controlPanel.add(searchButton);
        controlPanel.add(Box.createHorizontalStrut(25));
        controlPanel.add(new JLabel("排序方式:"));
        controlPanel.add(sortBox);
        controlPanel.add(sortButton);

        northPanel.add(infoPanel);
        northPanel.add(controlPanel);
        add(northPanel, BorderLayout.NORTH);

        String[] columns = {"PID", "进程名称", "命令路径", "CPU(%)", "内存(MB)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        processTable = new JTable(tableModel);
        processTable.setRowHeight(24);
        processTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(processTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        refreshButton = new JButton("刷新");
        killButton = new JButton("结束进程");
        exportButton = new JButton("导出日志");

        bottomPanel.add(refreshButton);
        bottomPanel.add(killButton);
        bottomPanel.add(exportButton);

        add(bottomPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> searchByPid());
        refreshButton.addActionListener(e -> loadProcessData());
        killButton.addActionListener(e -> killSelectedProcess());
        exportButton.addActionListener(e -> exportLog());
        sortButton.addActionListener(e -> loadProcessData());
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

    private void loadProcessData() {
        List<ProcessInfo> processes = processService.getAllProcesses();
        processes = sortProcesses(processes);

        tableModel.setRowCount(0);

        for (ProcessInfo process : processes) {
            tableModel.addRow(new Object[]{
                    process.getPid(),
                    process.getName(),
                    process.getCommand(),
                    String.format("%.2f", process.getCpuUsage()),
                    process.getMemoryMb()
            });
        }

        updateSystemInfo(processes.size());
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

    private void searchByPid() {
        String text = pidField.getText().trim();

        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入PID");
            return;
        }

        try {
            long pid = Long.parseLong(text);

            processService.getProcessByPid(pid).ifPresentOrElse(process -> {
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{
                        process.getPid(),
                        process.getName(),
                        process.getCommand(),
                        String.format("%.2f", process.getCpuUsage()),
                        process.getMemoryMb()
                });
                updateSystemInfo(1);
            }, () -> JOptionPane.showMessageDialog(this, "未找到该PID对应的进程"));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "PID必须是数字");
        }
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
            loadProcessData();
        } else {
            JOptionPane.showMessageDialog(this, "结束失败");
        }
    }

    private void exportLog() {
        try {
            LogUtil.export(processService.getAllProcesses());
            JOptionPane.showMessageDialog(this, "导出成功\nprocess_log.txt");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "导出失败\n" + e.getMessage());
        }
    }

    private void startAutoRefresh() {
        Timer timer = new Timer(5000, e -> loadProcessData());
        timer.start();
    }
}
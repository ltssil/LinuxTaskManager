package cn.ltssil.linux.ui;

import cn.ltssil.linux.model.ProcessInfo;
import cn.ltssil.linux.service.ProcessService;
import cn.ltssil.linux.util.LogUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final ProcessService processService = new ProcessService();

    private JTable processTable;
    private DefaultTableModel tableModel;
    private JTextField pidField;

    private JButton searchButton;
    private JButton refreshButton;
    private JButton killButton;
    private JButton exportButton;

    public MainFrame() {
        initFrame();
        initComponents();
        loadProcessData();
        setVisible(true);
    }

    private void initFrame() {
        setTitle("Linux Task Manager");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        pidField = new JTextField(15);
        searchButton = new JButton("查询PID");

        topPanel.add(new JLabel("PID:"));
        topPanel.add(pidField);
        topPanel.add(searchButton);

        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"PID", "进程名称", "命令路径"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        processTable = new JTable(tableModel);
        processTable.setRowHeight(24);

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
        exportButton.addActionListener(
                e -> exportLog()
        );
    }

    private void loadProcessData() {
        tableModel.setRowCount(0);

        List<ProcessInfo> processes = processService.getAllProcesses();
        for (ProcessInfo process : processes) {
            tableModel.addRow(new Object[]{
                    process.getPid(),
                    process.getName(),
                    process.getCommand()
            });
        }
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
                        process.getCommand()
                });
            }, () -> JOptionPane.showMessageDialog(this, "未找到该PID对应的进程"));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "PID必须是数字");
        }
    }

    private void killSelectedProcess() {

        int row = processTable.getSelectedRow();

        if (row == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先选择一个进程"
            );

            return;
        }

        long pid = Long.parseLong(
                tableModel.getValueAt(row, 0).toString()
        );

        int result = JOptionPane.showConfirmDialog(
                this,
                "确定结束 PID = " + pid + " ?",
                "确认",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success =
                processService.killProcess(pid);

        if (success) {

            JOptionPane.showMessageDialog(
                    this,
                    "结束成功"
            );

            loadProcessData();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "结束失败"
            );
        }
    }

    private void exportLog() {

        try {

            LogUtil.export(
                    processService.getAllProcesses()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "导出成功\nprocess_log.txt"
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "导出失败\n" + e.getMessage()
            );
        }
    }
}
package cn.ltssil.linux.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MainFrame extends JFrame {

    private JTable processTable;

    private JTextField pidField;

    private JButton searchButton;
    private JButton refreshButton;
    private JButton killButton;
    private JButton exportButton;

    public MainFrame() {

        initFrame();

        initComponents();

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

        JPanel topPanel = new JPanel();

        pidField = new JTextField(15);

        searchButton = new JButton("查询PID");

        topPanel.add(new JLabel("PID:"));

        topPanel.add(pidField);

        topPanel.add(searchButton);

        add(topPanel, BorderLayout.NORTH);

        String[] columns = {
                "PID",
                "进程名称",
                "命令路径"
        };

        DefaultTableModel model =
                new DefaultTableModel(columns, 0);

        processTable = new JTable(model);

        JScrollPane scrollPane =
                new JScrollPane(processTable);

        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();

        refreshButton = new JButton("刷新");

        killButton = new JButton("结束进程");

        exportButton = new JButton("导出日志");

        bottomPanel.add(refreshButton);

        bottomPanel.add(killButton);

        bottomPanel.add(exportButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }
}
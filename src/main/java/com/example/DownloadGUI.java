package com.example;

import com.example.client.ConcurrentDownloadClient;
import com.example.utils.ConfigLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadGUI extends JFrame {
    private JTextField urlField;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JTable stepTable;
    private DefaultTableModel tableModel;

    public DownloadGUI() {
        setTitle("Distributed File Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("URL (ignore): "));
        urlField = new JTextField(20);
        inputPanel.add(urlField);
        downloadButton = new JButton("Download");
        inputPanel.add(downloadButton);

        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        tableModel = new DefaultTableModel(
                new Object[]{"Bước", "Hoạt động", "Giao thức", "Mô hình", "Vị trí"}, 0
        );
        stepTable = new JTable(tableModel);
        stepTable.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(stepTable);

        add(inputPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        downloadButton.addActionListener(e -> startDownload());

        pack();
        setLocationRelativeTo(null);
    }

    private void startDownload() {
        statusLabel.setText("Downloading...");
        new Thread(() -> {
            try {
                updateStepTable();
                ConcurrentDownloadClient.downloadFile("");
                statusLabel.setText("Download completed");
                JOptionPane.showMessageDialog(this, "Download completed\nHash verified successfully!");
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void updateStepTable() {
        tableModel.setRowCount(0);
        String[] servers = ConfigLoader.getServers();
        addStep(1, "Khởi tạo kết nối đến các server", "TCP", "Client-Server", "ConcurrentDownloadClient.downloadFile()");
        for (int i = 0; i < servers.length; i++) {
            String[] parts = servers[i].split(":");
            addStep(2 + i, "Gửi yêu cầu tải chunk " + i + " đến " + servers[i], "TCP", "Client-Server", "DownloadTask.call()");
        }
        addStep(2 + servers.length, "Server xử lý và gửi dữ liệu chunk", "TCP", "Client-Server", "ClientHandler.run()");
        addStep(3 + servers.length, "Client nhận và lưu các chunk", "TCP", "Client-Server", "DownloadTask.call()");
        addStep(4 + servers.length, "Ghép các chunk thành file hoàn chỉnh", "N/A", "Client-Server", "FileUtils.saveChunk()");
        addStep(5 + servers.length, "Xác thực hash của file tải về", "N/A", "Client-Server", "FileUtils.calculateHash()");
    }

    private void addStep(int step, String activity, String protocol, String model, String location) {
        tableModel.addRow(new Object[]{step, activity, protocol, model, location});
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DownloadGUI gui = new DownloadGUI();
            gui.setVisible(true);
        });
    }
}
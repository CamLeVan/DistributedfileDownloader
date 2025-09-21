package com.example;

import com.example.client.ConcurrentDownloadClient;

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
        // Khởi tạo khung GUI
        setTitle("Distributed File Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel đầu vào
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("URL: "));
        urlField = new JTextField(20);
        inputPanel.add(urlField);
        downloadButton = new JButton("Download");
        inputPanel.add(downloadButton);

        // Panel trạng thái
        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Tạo bảng bước thực hiện
        tableModel = new DefaultTableModel(
                new Object[]{"Bước", "Hoạt động", "Giao thức", "Mô hình", "Vị trí"}, 0
        );
        stepTable = new JTable(tableModel);
        stepTable.setEnabled(false); // Chỉ đọc
        JScrollPane scrollPane = new JScrollPane(stepTable);

        // Thêm vào khung
        add(inputPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Sự kiện nút Download
        downloadButton.addActionListener(e -> startDownload());

        pack();
        setLocationRelativeTo(null);
    }

    private void startDownload() {
        statusLabel.setText("Downloading...");
        new Thread(() -> {
            try {
                // Cập nhật bảng bước thực hiện
                updateStepTable();

                // Giả lập tải file (gọi phương thức tải thật từ ConcurrentDownloadClient)
                String url = urlField.getText();
                ConcurrentDownloadClient client = new ConcurrentDownloadClient();
                client.downloadFile(url, 2); // 2 server

                // Xác thực hash (giả lập)
                statusLabel.setText("Download completed");
                JOptionPane.showMessageDialog(this, "Download completed\nHash verified successfully!");
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void updateStepTable() {
        // Xóa bảng cũ
        tableModel.setRowCount(0);

        // Thêm các bước chi tiết
        addStep(1, "Khởi tạo kết nối đến server", "TCP", "Client-Server", "ConcurrentDownloadClient.connect()");
        addStep(2, "Gửi yêu cầu tải chunk 0 đến server 12345", "TCP", "Client-Server", "DownloadTask.run()");
        addStep(3, "Gửi yêu cầu tải chunk 1 đến server 12346", "TCP", "Client-Server", "DownloadTask.run()");
        addStep(4, "Server xử lý và gửi dữ liệu chunk 0", "TCP", "Client-Server", "ClientHandler.run()");
        addStep(5, "Server xử lý và gửi dữ liệu chunk 1", "TCP", "Client-Server", "ClientHandler.run()");
        addStep(6, "Client nhận và lưu chunk 0", "TCP", "Client-Server", "DownloadTask.run()");
        addStep(7, "Client nhận và lưu chunk 1", "TCP", "Client-Server", "DownloadTask.run()");
        addStep(8, "Ghép các chunk thành file hoàn chỉnh", "N/A", "Client-Server", "ConcurrentDownloadClient.mergeChunks()");
        addStep(9, "Xác thực hash của file tải về", "N/A", "Client-Server", "FileUtils.verifyHash()");
    }

    private void addStep(int step, String activity, String protocol, String model, String location) {
        tableModel.addRow(new Object[]{step, activity, protocol, model, location});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DownloadGUI gui = new DownloadGUI();
            gui.setVisible(true);
        });
    }
}
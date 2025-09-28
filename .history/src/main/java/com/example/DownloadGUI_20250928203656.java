package com.example;

import com.example.client.ConcurrentDownloadClient;
import com.example.utils.ConfigLoader;
import com.example.utils.DBUtils;
import com.example.utils.FileUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class DownloadGUI extends JFrame {
    private JTextField urlField;
    private JButton downloadButton;
    private JButton resumeButton;
    private JLabel statusLabel;
    private JTable stepTable;
    public static DefaultTableModel tableModel;
    private JTable fileTable;
    private DefaultTableModel fileTableModel;
    private JButton listButton;
    private JButton downloadSelectedButton;

    public DownloadGUI() {
        setTitle("Distributed File Downloader - Real App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("URL (ignore): "));
        urlField = new JTextField(20);
        inputPanel.add(urlField);
        downloadButton = new JButton("Download Default");
        inputPanel.add(downloadButton);
        resumeButton = new JButton("Resume");
        resumeButton.setEnabled(false);
        inputPanel.add(resumeButton);

        JButton uploadButton = new JButton("Upload File Mới");
        inputPanel.add(uploadButton);
        uploadButton.addActionListener(e -> uploadNewFile());

        JPanel listPanel = new JPanel(new BorderLayout());
        listButton = new JButton("Lấy Danh sách Files");
        listPanel.add(listButton, BorderLayout.NORTH);
        fileTableModel = new DefaultTableModel(new Object[]{"Name", "Size (bytes)", "Hash", "Action"}, 0);
        fileTable = new JTable(fileTableModel);
        fileTable.setEnabled(true);
        // FIXED: Custom renderer cho Action column thành button
        fileTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        JScrollPane fileScroll = new JScrollPane(fileTable);
        listPanel.add(fileScroll, BorderLayout.CENTER);
        downloadSelectedButton = new JButton("Tải File Được Chọn");
        downloadSelectedButton.setEnabled(false);
        listPanel.add(downloadSelectedButton, BorderLayout.SOUTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        tableModel = new DefaultTableModel(
                new Object[]{"Bước", "Hoạt động", "Giao thức", "Mô hình", "Vị trí", "Trạng thái"}, 0
        );
        stepTable = new JTable(tableModel);
        stepTable.setEnabled(false);
        JScrollPane stepScroll = new JScrollPane(stepTable);

        add(inputPanel, BorderLayout.NORTH);
        add(listPanel, BorderLayout.WEST);
        add(statusLabel, BorderLayout.CENTER);
        add(stepScroll, BorderLayout.SOUTH);

        downloadButton.addActionListener(e -> startDownload("test.txt"));
        resumeButton.addActionListener(e -> resumeDownload("test.txt"));

        listButton.addActionListener(e -> fetchFileList());
        downloadSelectedButton.addActionListener(e -> downloadSelectedFile());

        pack();
        setLocationRelativeTo(null);
    }

    // FIXED: Custom ButtonRenderer & Editor (simple implementation)
    static class ButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if ("Tải".equals(value)) {
                JButton button = new JButton("Tải");
                button.addActionListener(e -> {
                    String fileName = (String) table.getValueAt(row, 0);
                    new DownloadGUI().startDownload(fileName);  // Trigger download
                });
                return button;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return new ButtonRenderer().getTableCellRendererComponent(table, value, isSelected, true, row, column);
        }
    }

    private void fetchFileList() {
        statusLabel.setText("Fetching file list...");
        new Thread(() -> {
            List<String[]> files = ConcurrentDownloadClient.getFileListFromMaster();
            SwingUtilities.invokeLater(() -> {
                fileTableModel.setRowCount(0);
                for (String[] row : files) {
                    if (row.length >= 3) {
                        String[] newRow = new String[4];
                        newRow[0] = row[0]; // name
                        newRow[1] = row[1]; // size
                        newRow[2] = row[2]; // hash
                        newRow[3] = "Tải";  // action
                        fileTableModel.addRow(newRow);
                    }
                }
                downloadSelectedButton.setEnabled(!files.isEmpty());
                statusLabel.setText("List fetched: " + files.size() + " files");
            });
        }).start();
    }

    // FIXED: Check selectedRow > -1
    private void downloadSelectedFile() {
        int row = fileTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn file để tải!");
            return;
        }
        String fileName = (String) fileTableModel.getValueAt(row, 0);
        startDownload(fileName);
    }

    private void startDownload(String fileName) {
        statusLabel.setText("Downloading " + fileName + "...");
        tableModel.setRowCount(0);
        resumeButton.setEnabled(false);
        new Thread(() -> {
            try {
                updateStepTableStepByStep(fileName);
                ConcurrentDownloadClient.downloadFile(fileName);
                statusLabel.setText("Download " + fileName + " completed");
                tableModel.setValueAt("Hoàn tất", tableModel.getRowCount() - 1, 5);
                JOptionPane.showMessageDialog(this, "Download " + fileName + " completed! Hash verified.");
            } catch (IOException | NoSuchAlgorithmException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                tableModel.setValueAt("Lỗi: " + ex.getMessage(), tableModel.getRowCount() - 1, 5);
                resumeButton.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void resumeDownload(String fileName) {
        startDownload(fileName);
    }

    private void updateStepTableStepByStep(String fileName) {
        String[] servers = ConfigLoader.getServers();
        addStep(1, "Khởi tạo kết nối đến các server cho " + fileName, "TCP", "Client-Server", "ConcurrentDownloadClient.downloadFile()", "Chờ");
        for (int i = 0; i < servers.length; i++) {
            String[] parts = servers[i].split(":");
            addStep(2 + i, "Gửi yêu cầu tải chunk " + i + " đến " + servers[i] + " cho " + fileName, "TCP", "Client-Server", "DownloadTask.call()", "Chờ");
        }
        addStep(2 + servers.length, "Server xử lý và gửi dữ liệu chunk cho " + fileName, "TCP", "Client-Server", "ClientHandler.run()", "Chờ");
        addStep(3 + servers.length, "Client nhận và lưu các chunk của " + fileName, "TCP", "Client-Server", "DownloadTask.call()", "Chờ");
        addStep(4 + servers.length, "Ghép các chunk thành file " + fileName, "N/A", "Client-Server", "FileUtils.saveChunk()", "Chờ");
        addStep(5 + servers.length, "Xác thực hash của " + fileName, "N/A", "Client-Server", "FileUtils.calculateHash()", "Chờ");
    }

    private void addStep(int step, String activity, String protocol, String model, String location, String status) {
        tableModel.addRow(new Object[]{step, activity, protocol, model, location, status});
    }

    public static void log(String message) {
        System.out.println(message);
    }
    private void uploadNewFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                String hash = FileUtils.calculateHash(selectedFile.getAbsolutePath(), "SHA-256");
                long size = selectedFile.length();
                String fileName = selectedFile.getName();
                // Copy to /files/
                String basePath = System.getProperty("user.dir") + "/files/";
                new File(basePath).mkdirs();
                Files.copy(selectedFile.toPath(), new File(basePath + fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                // Insert DB
                DBUtils.insertFile(fileName, size, hash);
                statusLabel.setText("Uploaded " + fileName + " successfully!");
                JOptionPane.showMessageDialog(this, "Upload " + fileName + " OK. Hash: " + hash);
                // Refresh list
                fetchFileList();
            } catch (IOException | NoSuchAlgorithmException ex) {
                statusLabel.setText("Upload error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Upload Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DownloadGUI gui = new DownloadGUI();
            gui.setVisible(true);
        });
    }
}
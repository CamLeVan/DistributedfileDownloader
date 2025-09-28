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
        setTitle("Hệ Thống Tải File Phân Tán - Ứng Dụng Thực Tế");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1000, 700);

        // Panel điều khiển chính
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Điều Khiển Tải File"));
        
        downloadButton = new JButton("Tải File Mặc Định (test.txt)");
        downloadButton.setToolTipText("Tải file test.txt từ server");
        controlPanel.add(downloadButton);
        
        resumeButton = new JButton("Tiếp Tục Tải");
        resumeButton.setToolTipText("Tiếp tục tải file bị gián đoạn");
        resumeButton.setEnabled(false);
        controlPanel.add(resumeButton);

        JButton uploadButton = new JButton("Tải Lên File Mới");
        uploadButton.setToolTipText("Tải file mới lên server");
        controlPanel.add(uploadButton);
        uploadButton.addActionListener(e -> uploadNewFile());

        // Panel danh sách file
        JPanel fileListPanel = new JPanel(new BorderLayout());
        fileListPanel.setBorder(BorderFactory.createTitledBorder("Danh Sách File Có Sẵn"));
        fileListPanel.setPreferredSize(new Dimension(400, 300));
        
        listButton = new JButton("Lấy Danh Sách File");
        listButton.setToolTipText("Tải danh sách file từ server");
        fileListPanel.add(listButton, BorderLayout.NORTH);
        
        fileTableModel = new DefaultTableModel(new Object[]{"Tên File", "Kích Thước (bytes)", "Hash SHA-256", "Hành Động"}, 0);
        fileTable = new JTable(fileTableModel);
        fileTable.setEnabled(true);
        fileTable.setRowHeight(30);
        
        // Custom renderer cho Action column thành button
        fileTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane fileScroll = new JScrollPane(fileTable);
        fileListPanel.add(fileScroll, BorderLayout.CENTER);
        
        downloadSelectedButton = new JButton("Tải File Được Chọn");
        downloadSelectedButton.setToolTipText("Tải file được chọn trong bảng");
        downloadSelectedButton.setEnabled(false);
        fileListPanel.add(downloadSelectedButton, BorderLayout.SOUTH);

        // Panel trạng thái
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Trạng Thái Hệ Thống"));
        
        statusLabel = new JLabel("Sẵn sàng - Chọn file để tải");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.BLUE);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // Panel tiến trình chi tiết
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createTitledBorder("Tiến Trình Tải File Chi Tiết"));
        
        tableModel = new DefaultTableModel(
                new Object[]{"Bước", "Hoạt Động", "Giao Thức", "Mô Hình", "Vị Trí Code", "Trạng Thái"}, 0
        );
        stepTable = new JTable(tableModel);
        stepTable.setEnabled(false);
        stepTable.setRowHeight(25);
        stepTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stepTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        stepTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        stepTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        stepTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        stepTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        JScrollPane stepScroll = new JScrollPane(stepTable);
        progressPanel.add(stepScroll, BorderLayout.CENTER);

        // Layout chính
        add(controlPanel, BorderLayout.NORTH);
        add(fileListPanel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.SOUTH);

        // Event handlers
        downloadButton.addActionListener(e -> startDownload("test.txt"));
        resumeButton.addActionListener(e -> resumeDownload("test.txt"));
        listButton.addActionListener(e -> fetchFileList());
        downloadSelectedButton.addActionListener(e -> downloadSelectedFile());

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
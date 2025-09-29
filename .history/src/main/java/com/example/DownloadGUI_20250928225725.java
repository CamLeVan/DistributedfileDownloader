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
        
        fileTableModel = new DefaultTableModel(new Object[]{"Tên File", "Kích Thước (bytes)", "Hash SHA-256", "Hành Động", "Xem Nội Dung"}, 0);
        fileTable = new JTable(fileTableModel);
        fileTable.setEnabled(true);
        fileTable.setRowHeight(30);
        
        // Custom renderer cho Action columns thành button
        fileTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        fileTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        fileTable.getColumnModel().getColumn(4).setCellRenderer(new ViewContentRenderer());
        fileTable.getColumnModel().getColumn(4).setCellEditor(new ViewContentEditor(new JCheckBox()));
        
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

    // FIXED: Custom ButtonRenderer & Editor với reference đúng
    static class ButtonRenderer extends DefaultTableCellRenderer {
        private DownloadGUI parentGUI;
        
        public ButtonRenderer(DownloadGUI parent) {
            this.parentGUI = parent;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if ("Tải Ngay".equals(value)) {
                JButton button = new JButton("Tải Ngay");
                button.setBackground(Color.GREEN);
                button.setForeground(Color.WHITE);
                button.addActionListener(e -> {
                    String fileName = (String) table.getValueAt(row, 0);
                    if (parentGUI != null) {
                        parentGUI.startDownload(fileName);
                    }
                });
                return button;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private DownloadGUI parentGUI;
        
        public ButtonEditor(JCheckBox checkBox, DownloadGUI parent) {
            super(checkBox);
            this.parentGUI = parent;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return new ButtonRenderer(parentGUI).getTableCellRendererComponent(table, value, isSelected, true, row, column);
        }
    }

    // Renderer cho nút "Xem File" - FIXED với reference đúng
    static class ViewContentRenderer extends DefaultTableCellRenderer {
        private DownloadGUI parentGUI;
        
        public ViewContentRenderer(DownloadGUI parent) {
            this.parentGUI = parent;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if ("Xem File".equals(value)) {
                JButton button = new JButton("Xem File");
                button.setBackground(Color.CYAN);
                button.setForeground(Color.BLACK);
                button.addActionListener(e -> {
                    String fileName = (String) table.getValueAt(row, 0);
                    if (parentGUI != null) {
                        parentGUI.viewFileContent(fileName);
                    }
                });
                return button;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    // Editor cho nút "Xem File"
    static class ViewContentEditor extends DefaultCellEditor {
        public ViewContentEditor(JCheckBox checkBox) {
            super(checkBox);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return new ViewContentRenderer().getTableCellRendererComponent(table, value, isSelected, true, row, column);
        }
    }

    private void fetchFileList() {
        statusLabel.setText("Đang lấy danh sách file từ server...");
        statusLabel.setForeground(Color.ORANGE);
        new Thread(() -> {
            List<String[]> files = ConcurrentDownloadClient.getFileListFromMaster();
            SwingUtilities.invokeLater(() -> {
                fileTableModel.setRowCount(0);
                for (String[] row : files) {
                    if (row.length >= 3) {
                        String[] newRow = new String[5];
                        newRow[0] = row[0]; // tên file
                        newRow[1] = row[1]; // kích thước
                        newRow[2] = row[2].substring(0, Math.min(16, row[2].length())) + "..."; // hash rút gọn
                        newRow[3] = "Tải Ngay";  // hành động
                        newRow[4] = "Xem File";  // xem nội dung
                        fileTableModel.addRow(newRow);
                    }
                }
                downloadSelectedButton.setEnabled(!files.isEmpty());
                statusLabel.setText("Đã lấy danh sách: " + files.size() + " file có sẵn");
                statusLabel.setForeground(files.isEmpty() ? Color.RED : Color.GREEN);
            });
        }).start();
    }

    // Kiểm tra file được chọn
    private void downloadSelectedFile() {
        int row = fileTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file để tải!", "Chưa Chọn File", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String fileName = (String) fileTableModel.getValueAt(row, 0);
        startDownload(fileName);
    }

    private void startDownload(String fileName) {
        statusLabel.setText("Đang tải file: " + fileName + "...");
        statusLabel.setForeground(Color.ORANGE);
        tableModel.setRowCount(0);
        resumeButton.setEnabled(false);
        downloadButton.setEnabled(false);
        
        new Thread(() -> {
            try {
                updateStepTableStepByStep(fileName);
                ConcurrentDownloadClient.downloadFile(fileName);
                statusLabel.setText("Tải file " + fileName + " thành công!");
                statusLabel.setForeground(Color.GREEN);
                tableModel.setValueAt("Hoàn thành", tableModel.getRowCount() - 1, 5);
                JOptionPane.showMessageDialog(this, 
                    "Tải file " + fileName + " thành công!\n" +
                    "Hash đã được xác thực.", 
                    "Tải Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | NoSuchAlgorithmException ex) {
                statusLabel.setText("Lỗi tải file: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                tableModel.setValueAt("Lỗi: " + ex.getMessage(), tableModel.getRowCount() - 1, 5);
                resumeButton.setEnabled(true);
                JOptionPane.showMessageDialog(this, 
                    "Lỗi tải file: " + ex.getMessage(), 
                    "Lỗi Tải File", 
                    JOptionPane.ERROR_MESSAGE);
            } finally {
                downloadButton.setEnabled(true);
            }
        }).start();
    }

    private void resumeDownload(String fileName) {
        startDownload(fileName);
    }

    private void updateStepTableStepByStep(String fileName) {
        String[] servers = ConfigLoader.getServers();
        addStep(1, "Khởi tạo kết nối đến " + servers.length + " server cho " + fileName, "TCP", "Client-Server", "ConcurrentDownloadClient.downloadFile()", "Đang thực hiện");
        for (int i = 0; i < servers.length; i++) {
            String[] parts = servers[i].split(":");
            addStep(2 + i, "Gửi yêu cầu tải phần " + (i+1) + " đến " + servers[i], "TCP", "Client-Server", "DownloadTask.call()", "Đang thực hiện");
        }
        addStep(2 + servers.length, "Server xử lý và gửi dữ liệu phần file", "TCP", "Client-Server", "ClientHandler.run()", "Đang thực hiện");
        addStep(3 + servers.length, "Client nhận và lưu các phần của file", "TCP", "Client-Server", "DownloadTask.call()", "Đang thực hiện");
        addStep(4 + servers.length, "Ghép các phần thành file hoàn chỉnh", "N/A", "Client-Server", "FileUtils.saveChunk()", "Đang thực hiện");
        addStep(5 + servers.length, "Xác thực tính toàn vẹn file bằng SHA-256", "N/A", "Client-Server", "FileUtils.calculateHash()", "Đang thực hiện");
    }

    private void addStep(int step, String activity, String protocol, String model, String location, String status) {
        tableModel.addRow(new Object[]{step, activity, protocol, model, location, status});
    }

    public static void log(String message) {
        System.out.println(message);
    }

    // Method để xem nội dung file
    private void viewFileContent(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, 
                "File " + fileName + " chưa được tải về!\nVui lòng tải file trước khi xem nội dung.", 
                "File Chưa Tồn Tại", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
            
            // Tạo cửa sổ xem nội dung
            JFrame contentFrame = new JFrame("Nội Dung File: " + fileName);
            contentFrame.setSize(600, 400);
            contentFrame.setLocationRelativeTo(this);
            
            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            contentFrame.add(scrollPane);
            
            // Thêm nút xuất file
            JPanel buttonPanel = new JPanel();
            JButton exportButton = new JButton("Xuất File");
            exportButton.addActionListener(e -> exportFile(fileName));
            buttonPanel.add(exportButton);
            
            contentFrame.add(buttonPanel, BorderLayout.SOUTH);
            contentFrame.setVisible(true);
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Lỗi đọc file: " + ex.getMessage(), 
                "Lỗi Đọc File", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method để xuất file
    private void exportFile(String fileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn vị trí lưu file");
        chooser.setSelectedFile(new File(fileName));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File sourceFile = new File(fileName);
                File destFile = chooser.getSelectedFile();
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                JOptionPane.showMessageDialog(this, 
                    "Xuất file thành công!\nVị trí: " + destFile.getAbsolutePath(), 
                    "Xuất Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi xuất file: " + ex.getMessage(), 
                    "Lỗi Xuất File", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void uploadNewFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn File Để Tải Lên Server");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            statusLabel.setText("Đang tải lên file: " + selectedFile.getName() + "...");
            statusLabel.setForeground(Color.ORANGE);
            
            try {
                // Tính hash SHA-256
                String hash = FileUtils.calculateHash(selectedFile.getAbsolutePath(), "SHA-256");
                long size = selectedFile.length();
                String fileName = selectedFile.getName();
                
                // Copy file vào thư mục resources
                String basePath = System.getProperty("user.dir") + "/src/main/resources/";
                new File(basePath).mkdirs();
                Files.copy(selectedFile.toPath(), new File(basePath + fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Thêm vào database
                DBUtils.insertFile(fileName, size, hash);
                
                statusLabel.setText("Tải lên file " + fileName + " thành công!");
                statusLabel.setForeground(Color.GREEN);
                
                JOptionPane.showMessageDialog(this, 
                    "Tải lên file thành công!\n" +
                    "Tên file: " + fileName + "\n" +
                    "Kích thước: " + size + " bytes\n" +
                    "Hash SHA-256: " + hash.substring(0, 16) + "...", 
                    "Tải Lên Thành Công", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Làm mới danh sách file
                fetchFileList();
                
            } catch (IOException | NoSuchAlgorithmException ex) {
                statusLabel.setText("Lỗi tải lên: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, 
                    "Lỗi tải lên file: " + ex.getMessage(), 
                    "Lỗi Tải Lên", 
                    JOptionPane.ERROR_MESSAGE);
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
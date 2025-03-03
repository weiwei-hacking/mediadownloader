import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.awt.Desktop;
import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

public class MediaDownloader extends JFrame {
    private JTextField urlField;
    private JTextField pathField;
    private JProgressBar progressBar;
    private JButton videoButton;
    private JButton audioButton;
    private Timer reverseTimer;
    
    public MediaDownloader() {
        // 設置視窗
        setTitle("Media Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 220);
        setLocationRelativeTo(null);
        
        // 創建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // URL輸入區域
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        JLabel urlLabel = new JLabel("貼上連結:");
        urlField = new JTextField();
        // 減少輸入框高度
        urlField.setPreferredSize(new Dimension(urlField.getPreferredSize().width, 25));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);
        
        // 路徑選擇區域
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        JLabel pathLabel = new JLabel("下載路徑:");
        pathField = new JTextField(System.getProperty("user.home") + File.separator + "Downloads");
        pathField.setEditable(false);
        pathField.setPreferredSize(new Dimension(pathField.getPreferredSize().width, 25));
        
        // 添加瀏覽按鈕
        JButton browseButton = new JButton("...");
        browseButton.setPreferredSize(new Dimension(30, 25));
        
        pathPanel.add(pathLabel, BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        
        // 按鈕區域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        videoButton = new JButton("視訊格式 (MP4)");
        audioButton = new JButton("音訊格式 (MP3)");
        buttonPanel.add(videoButton);
        buttonPanel.add(audioButton);
        
        // 進度條
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("準備就緒");
        progressBar.setForeground(new Color(0, 0, 0, 0)); // 初始為透明
        progressBar.setBorderPainted(true);
        
        // 設置進度條文字顏色為黑色
        UIDefaults defaults = UIManager.getDefaults();
        defaults.put("ProgressBar.selectionForeground", Color.BLACK);
        defaults.put("ProgressBar.selectionBackground", Color.BLACK);
        SwingUtilities.updateComponentTreeUI(progressBar);
        
        // 添加組件到主面板
        mainPanel.add(urlPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(pathPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(progressBar);
        
        // 添加到框架
        add(mainPanel);
        
        // 添加事件監聽器
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDownloadPath();
            }
        });
        
        videoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadMedia("mp4");
            }
        });
        
        audioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadMedia("mp3");
            }
        });
    }
    
    private void selectDownloadPath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("選擇下載路徑");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void downloadMedia(final String format) {
        final String url = urlField.getText().trim();
        final String path = pathField.getText().trim();
        
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入有效的URL", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        videoButton.setEnabled(false);
        audioButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("準備下載...");
        progressBar.setForeground(Color.GREEN);  // 設置進度條為綠色
        
        new SwingWorker<File, Integer>() {
            @Override
            protected File doInBackground() throws Exception {
                try {
                    // 使用yt-dlp的命名模板來處理檔案名稱
                    String outputTemplate = path + File.separator + "%(title)s.%(ext)s";
                    
                    // 準備yt-dlp命令
                    ProcessBuilder processBuilder;
                    if (format.equals("mp4")) {
                        processBuilder = new ProcessBuilder(
                            "yt-dlp",
                            "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                            "-o", outputTemplate,
                            url
                        );
                    } else { // mp3
                        processBuilder = new ProcessBuilder(
                            "yt-dlp",
                            "-x", "--audio-format", "mp3",
                            "-o", outputTemplate,
                            url
                        );
                    }
                    
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    
                    // 用於存儲輸出檔案路徑
                    final StringBuilder outputFilePath = new StringBuilder();
                    
                    // 讀取yt-dlp輸出並更新進度
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 檢查輸出行是否包含完成下載的文件信息
                            if (line.contains("[download] Destination:")) {
                                String filePath = line.substring(line.indexOf(":") + 1).trim();
                                outputFilePath.setLength(0);
                                outputFilePath.append(filePath);
                            } 
                            // 如果找到合併信息，可能文件已重命名
                            else if (line.contains("[Merger] Merging formats into")) {
                                String filePath = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                                outputFilePath.setLength(0);
                                outputFilePath.append(filePath);
                            }
                            // 檢查最終文件路徑信息
                            else if (line.contains("[ExtractAudio] Destination:")) {
                                String filePath = line.substring(line.indexOf(":") + 1).trim();
                                outputFilePath.setLength(0);
                                outputFilePath.append(filePath);
                            }
                            
                            // 嘗試從輸出中獲取下載進度
                            if (line.contains("%")) {
                                try {
                                    // 解析進度百分比
                                    int percentIndex = line.indexOf("%");
                                    if (percentIndex > 0) {
                                        String percentStr = line.substring(percentIndex - 4, percentIndex).trim();
                                        int progress = (int) Float.parseFloat(percentStr);
                                        publish(progress);
                                    }
                                } catch (Exception e) {
                                    // 如果無法解析進度，忽略錯誤
                                }
                            }
                        }
                    }
                    
                    // 等待下載完成
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new Exception("yt-dlp下載失敗，退出代碼：" + exitCode);
                    }
                    
                    // 如果有得到輸出文件路徑，返回該文件
                    if (outputFilePath.length() > 0) {
                        return new File(outputFilePath.toString());
                    }
                    
                    // 嘗試查找可能的輸出文件
                    File directory = new File(path);
                    File[] files = directory.listFiles((dir, name) -> name.endsWith("." + format));
                    if (files != null && files.length > 0) {
                        // 按照修改時間排序，獲取最新的文件
                        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                        return files[0];
                    }
                    
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MediaDownloader.this, 
                            "下載失敗：" + e.getMessage(), 
                            "錯誤", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                }
            }
            
            @Override
            protected void process(java.util.List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                progressBar.setValue(latest);
                
                if (latest < 100) {
                    progressBar.setString("下載中: " + latest + "%");
                } else {
                    progressBar.setString("轉換格式中...");
                }
            }
            
            @Override
            protected void done() {
                try {
                    File downloadedFile = get();
                    
                    if (downloadedFile != null && downloadedFile.exists()) {
                        progressBar.setValue(100);
                        progressBar.setString("下載完成!");
                        progressBar.setForeground(Color.BLUE);  // 下載完成後變為藍色
                        
                        // 顯示通知
                        showNotification("下載完成", "媒體已下載為 " + format + " 格式: " + downloadedFile.getName());
                        
                        // 打開文件夾
                        openFolder(downloadedFile.getParent());
                    } else {
                        progressBar.setValue(0);
                        progressBar.setString("下載失敗");
                        progressBar.setForeground(Color.RED);
                    }
                } catch (Exception e) {
                    progressBar.setValue(0);
                    progressBar.setString("下載失敗");
                    progressBar.setForeground(Color.RED);
                    e.printStackTrace();
                }
                
                // 啟動倒計時器，使進度條慢慢返回0
                final int[] currentValue = {progressBar.getValue()};
                reverseTimer = new Timer(50, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentValue[0] -= 2;
                        progressBar.setValue(currentValue[0]);
                        
                        if (currentValue[0] <= 0) {
                            reverseTimer.stop();
                            progressBar.setString("準備就緒");
                            progressBar.setForeground(new Color(0, 0, 0, 0)); // 返回透明
                            
                            // 重新啟用按鈕
                            videoButton.setEnabled(true);
                            audioButton.setEnabled(true);
                        }
                    }
                });
                reverseTimer.start();
            }
        }.execute();
    }
    
    private void showNotification(String title, String message) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                
                // 創建圖標
                Image image = createDefaultIcon();
                
                TrayIcon trayIcon = new TrayIcon(image, "Media Downloader");
                trayIcon.setImageAutoSize(true);
                
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                
                // 延遲後移除圖標
                new Timer(5000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tray.remove(trayIcon);
                    }
                }).start();
                
            } catch (Exception e) {
                e.printStackTrace();
                // 如果系統托盤不可用，則使用對話框
                JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // 如果系統不支持系統托盤，則使用對話框
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void openFolder(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Image createDefaultIcon() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, size, size);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(2, 2, size-4, size-4);
        g2d.dispose();
        return image;
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MediaDownloader().setVisible(true);
            }
        });
    }
}
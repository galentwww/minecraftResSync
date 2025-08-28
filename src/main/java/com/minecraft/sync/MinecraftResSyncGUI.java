package com.minecraft.sync;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinecraftResSyncGUI extends JFrame {
    
    // UI Components
    private JTextField apiUrlField;
    private JButton fetchButton;
    private JButton startWorkflowButton;
    private JProgressBar progressBar;
    private JProgressBar stageProgressBar;
    private JLabel stageLabel;
    private JTable modTable;
    private DefaultTableModel tableModel;
    private JPanel statsPanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JCheckBox autoDownloadCheckbox;
    
    // Stats display components
    private JLabel totalItemsLabel;
    private JLabel modCountLabel;
    private JLabel resourcePackCountLabel;
    private JLabel shaderCountLabel;
    private JLabel configCountLabel;
    private JLabel requiredCountLabel;
    private JLabel optionalCountLabel;
    
    // Data
    private ModListResponse currentResponse;
    private String initialApiUrl;
    
    // Workflow stages
    private enum WorkflowStage {
        FETCH_DATA("获取数据", 0),
        DOWNLOAD_PREREQUISITE_MODS("下载必备前置mod", 1),
        DOWNLOAD_CONFIGS("下载自定义配置", 2), 
        DOWNLOAD_REQUIRED_MODS("下载必备mod", 3),
        SELECT_OPTIONAL_MODS("选择并下载可选mod", 4),
        DOWNLOAD_RESOURCE_PACKS("下载资源包", 5),
        DOWNLOAD_SHADERS("下载光影", 6),
        COMPLETED("完成", 7);
        
        private final String description;
        private final int stepIndex;
        
        WorkflowStage(String description, int stepIndex) {
            this.description = description;
            this.stepIndex = stepIndex;
        }
        
        public String getDescription() { return description; }
        public int getStepIndex() { return stepIndex; }
        public static int getTotalSteps() { return values().length; }
    }
    
    private WorkflowStage currentStage = WorkflowStage.FETCH_DATA;
    
    // Platform-dependent fallback path for modlist.json
    private static String getLocalModlistPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows fallback - current directory or user home
            String currentDir = System.getProperty("user.dir");
            return currentDir + "\\modlist.json";
        } else {
            // macOS/Linux fallback
            return "/Users/galentwww/IdeaProjects/minecraftResSync/modlist.json";
        }
    }
    
    /**
     * Get UI font with Chinese character support for cross-platform compatibility
     */
    private Font getUIFont(int style, int size) {
        String os = System.getProperty("os.name").toLowerCase();
        String fontName;
        
        if (os.contains("win")) {
            // Windows: Use Microsoft YaHei for Chinese support
            fontName = "Microsoft YaHei";
        } else if (os.contains("mac")) {
            // macOS: Use PingFang SC for Chinese support
            fontName = "PingFang SC";
        } else {
            // Linux: Use commonly available fonts
            fontName = "DejaVu Sans";
        }
        
        // Try the preferred font first, fallback to SANS_SERIF if not available
        Font font = new Font(fontName, style, size);
        if (font.getFamily().equals(fontName)) {
            return font;
        } else {
            // Fallback to system default
            return new Font(Font.SANS_SERIF, style, size);
        }
    }
    
    public MinecraftResSyncGUI() {
        this(null);
    }
    
    public MinecraftResSyncGUI(String apiUrl) {
        this.initialApiUrl = apiUrl;
        initializeTheme();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultValues();
        
        // Auto-fetch if API URL provided
        if (apiUrl != null && !apiUrl.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                apiUrlField.setText(apiUrl);
                // 禁用URL编辑框，因为是通过命令行参数指定的
                apiUrlField.setEditable(false);
                apiUrlField.setToolTipText("通过命令行参数指定的API地址，不可编辑");
                fetchModData();
            });
        }
    }
    
    private void initializeTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            // Fallback to default look and feel if FlatLaf fails
            e.printStackTrace();
        }
    }
    
    private void initializeComponents() {
        setTitle("Minecraft Resource Sync Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // API URL input
        apiUrlField = new JTextField("https://api.galentwww.cn/items/modlist");
        apiUrlField.setFont(getUIFont(Font.PLAIN, 12));
        
        // Buttons
        fetchButton = new JButton("仅获取数据");
        fetchButton.setFont(getUIFont(Font.PLAIN, 11));
        fetchButton.setPreferredSize(new Dimension(120, 30));
        
        startWorkflowButton = new JButton("开始同步");
        startWorkflowButton.setFont(getUIFont(Font.BOLD, 12));
        startWorkflowButton.setPreferredSize(new Dimension(120, 30));
        
        
        autoDownloadCheckbox = new JCheckBox("自动下载", true);
        autoDownloadCheckbox.setFont(getUIFont(Font.PLAIN, 11));
        
        // Progress bars
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        
        // Stage progress bar
        stageProgressBar = new JProgressBar(0, WorkflowStage.getTotalSteps() - 1);
        stageProgressBar.setStringPainted(true);
        stageProgressBar.setValue(0);
        stageProgressBar.setString("准备就绪");
        
        // Stage label
        stageLabel = new JLabel("当前阶段: 准备就绪");
        stageLabel.setFont(getUIFont(Font.BOLD, 12));
        
        // Table for mods
        String[] columnNames = {"名称", "类型", "类别", "是否必需", "状态"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return Boolean.class;  // Required column
                return String.class;
            }
        };
        
        modTable = new JTable(tableModel);
        modTable.setRowHeight(25);
        modTable.setFont(getUIFont(Font.PLAIN, 11));
        modTable.getTableHeader().setFont(getUIFont(Font.BOLD, 12));
        modTable.setRowSorter(new TableRowSorter<>(tableModel));
        
        // Set column widths
        modTable.getColumnModel().getColumn(0).setPreferredWidth(200); // 名称
        modTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 类型
        modTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 类别
        modTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // 是否必需
        modTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 状态
        
        // Stats panel (will be created as a custom component)
        statsPanel = createStatsPanel();
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        logArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Status label
        statusLabel = new JLabel("Ready to fetch mod data");
        statusLabel.setFont(getUIFont(Font.PLAIN, 11));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel - URL input and controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        
        // URL and controls panel
        JPanel urlPanel = new JPanel(new BorderLayout(10, 0));
        urlPanel.add(new JLabel("API Endpoint:"), BorderLayout.WEST);
        urlPanel.add(apiUrlField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(fetchButton);
        buttonPanel.add(startWorkflowButton);
        urlPanel.add(buttonPanel, BorderLayout.EAST);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(autoDownloadCheckbox);
        
        topPanel.add(urlPanel, BorderLayout.CENTER);
        topPanel.add(controlPanel, BorderLayout.EAST);
        
        // Stage progress panel
        JPanel stagePanel = new JPanel(new BorderLayout(10, 5));
        stagePanel.add(stageLabel, BorderLayout.WEST);
        stagePanel.add(stageProgressBar, BorderLayout.CENTER);
        
        JPanel progressPanel = new JPanel(new BorderLayout(0, 5));
        progressPanel.add(stagePanel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.SOUTH);
        
        topPanel.add(progressPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center panel - Split pane with table and right panel
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6);
        
        // Left: Table
        JScrollPane tableScrollPane = new JScrollPane(modTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("资源列表"));
        mainSplitPane.setLeftComponent(tableScrollPane);
        
        // Right: Split pane with stats and log (vertical split)
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setResizeWeight(0.6); // Give more space to stats
        
        // Top: Stats panel with border
        JPanel statsContainer = new JPanel(new BorderLayout());
        statsContainer.setBorder(BorderFactory.createTitledBorder("统计信息"));
        statsContainer.add(statsPanel, BorderLayout.CENTER);
        rightSplitPane.setTopComponent(statsContainer);
        
        // Bottom: Log area with border
        JPanel logContainer = new JPanel(new BorderLayout());
        logContainer.setBorder(BorderFactory.createTitledBorder("操作日志"));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logContainer.add(logScrollPane, BorderLayout.CENTER);
        rightSplitPane.setBottomComponent(logContainer);
        
        rightSplitPane.setPreferredSize(new Dimension(320, 0));
        mainSplitPane.setRightComponent(rightSplitPane);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Bottom panel - Status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(5, 15, 15, 15));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Initialize stat labels
        totalItemsLabel = new JLabel("总数量: 0");
        totalItemsLabel.setFont(getUIFont(Font.BOLD, 14));
        
        modCountLabel = new JLabel("Mods: 0");
        modCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        
        resourcePackCountLabel = new JLabel("资源包: 0");
        resourcePackCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        
        shaderCountLabel = new JLabel("光影包: 0");
        shaderCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        
        configCountLabel = new JLabel("配置文件: 0");
        configCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        
        requiredCountLabel = new JLabel("必需项: 0");
        requiredCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        requiredCountLabel.setForeground(new Color(220, 53, 69)); // Bootstrap danger color
        
        optionalCountLabel = new JLabel("可选项: 0");
        optionalCountLabel.setFont(getUIFont(Font.PLAIN, 12));
        optionalCountLabel.setForeground(new Color(40, 167, 69)); // Bootstrap success color
        
        // Add components with spacing
        panel.add(totalItemsLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Catalog section
        JLabel catalogTitle = new JLabel("按类别分组:");
        catalogTitle.setFont(getUIFont(Font.BOLD, 11));
        panel.add(catalogTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(modCountLabel);
        panel.add(resourcePackCountLabel);
        panel.add(shaderCountLabel);
        panel.add(configCountLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Required/Optional section
        JLabel reqTitle = new JLabel("按需求分组:");
        reqTitle.setFont(getUIFont(Font.BOLD, 11));
        panel.add(reqTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(requiredCountLabel);
        panel.add(optionalCountLabel);
        
        // Add flexible space at bottom
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private void setupEventHandlers() {
        fetchButton.addActionListener(e -> fetchModData());
        startWorkflowButton.addActionListener(e -> startWorkflow());
        
        apiUrlField.addActionListener(e -> fetchModData());
        
    }
    
    private void setDefaultValues() {
        updateStatus("就绪，可以获取数据或开始完整流程");
        updateStage(WorkflowStage.FETCH_DATA);
        appendLog("应用程序已启动，准备就绪\n");
    }
    
    private void fetchModData() {
        String apiUrl = apiUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入API端点URL", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Just fetch data without workflow
        appendLog("仅获取数据...\n");
        executeFetchDataStage(apiUrl);
    }
    
    private void startWorkflow() {
        String apiUrl = apiUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入API端点URL", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Disable controls during workflow
        startWorkflowButton.setEnabled(false);
        fetchButton.setEnabled(false);
        
        appendLog("开始完整工作流程...\n");
        
        // Start with fetch data stage
        currentStage = WorkflowStage.FETCH_DATA;
        executeCurrentStage(apiUrl);
    }
    
    private void executeCurrentStage(String apiUrl) {
        updateStage(currentStage);
        appendLog(String.format("正在执行: %s\n", currentStage.getDescription()));
        
        switch (currentStage) {
            case FETCH_DATA:
                executeFetchDataStage(apiUrl);
                break;
            case DOWNLOAD_PREREQUISITE_MODS:
                executeDownloadPrerequisiteModsStage();
                break;
            case DOWNLOAD_CONFIGS:
                executeDownloadConfigsStage();
                break;
            case DOWNLOAD_REQUIRED_MODS:
                executeDownloadRequiredModsStage();
                break;
            case SELECT_OPTIONAL_MODS:
                executeSelectOptionalModsStage();
                break;
            case DOWNLOAD_RESOURCE_PACKS:
                executeDownloadResourcePacksStage();
                break;
            case DOWNLOAD_SHADERS:
                executeDownloadShadersStage();
                break;
            case COMPLETED:
                completeWorkflow();
                break;
        }
    }
    
    private void nextStage(String apiUrl) {
        WorkflowStage[] stages = WorkflowStage.values();
        if (currentStage.getStepIndex() < stages.length - 1) {
            currentStage = stages[currentStage.getStepIndex() + 1];
            executeCurrentStage(apiUrl);
        } else {
            completeWorkflow();
        }
    }
    
    private void executeFetchDataStage(String apiUrl) {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("正在获取数据...");
        
        SwingWorker<ModListResponse, Void> worker = new SwingWorker<ModListResponse, Void>() {
            @Override
            protected ModListResponse doInBackground() throws Exception {
                try {
                    String jsonResponse = HttpClient.get(apiUrl);
                    return JsonParser.parseModListFromString(jsonResponse);
                } catch (Exception e) {
                    appendLog("远程获取失败，尝试本地备份...\n");
                    return JsonParser.parseModListFromFile(getLocalModlistPath());
                }
            }
            
            @Override
            protected void done() {
                try {
                    ModListResponse response = get();
                    if (response != null && response.getData() != null) {
                        currentResponse = response;
                        updateTable(response);
                        updateStats(response);
                        appendLog(String.format("成功加载 %d 个资源\n", response.getData().size()));
                        
                        // Continue to next stage if in workflow
                        if (startWorkflowButton.isEnabled() == false) {
                            nextStage(apiUrl);
                        } else {
                            updateStatus("数据获取完成");
                            fetchButton.setEnabled(true);
                        }
                    } else {
                        appendLog("数据获取失败\n");
                        resetWorkflow();
                    }
                } catch (Exception e) {
                    appendLog("错误: " + e.getMessage() + "\n");
                    resetWorkflow();
                } finally {
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                }
            }
        };
        
        worker.execute();
    }
    
    private void executeDownloadPrerequisiteModsStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> prerequisiteMods = currentResponse.getData().stream()
            .filter(mod -> "mods".equals(mod.getCatelog()) 
                && mod.isRequired() 
                && "libs".equals(mod.getSubject()))
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个必备前置mod:\n", prerequisiteMods.size()));
        for (ModInfo mod : prerequisiteMods) {
            appendLog(String.format("  - %s\n", mod.getFriendlyName()));
        }
        
        if (autoDownloadCheckbox.isSelected() && !prerequisiteMods.isEmpty()) {
            realDownload(prerequisiteMods, "必备前置mod", () -> nextStage(apiUrlField.getText().trim()));
        } else {
            appendLog("跳过下载必备前置mod\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void executeDownloadConfigsStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> configs = currentResponse.getData().stream()
            .filter(mod -> "config".equals(mod.getCatelog()))
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个配置文件:\n", configs.size()));
        for (ModInfo mod : configs) {
            appendLog(String.format("  - %s\n", mod.getFriendlyName()));
        }
        
        if (autoDownloadCheckbox.isSelected() && !configs.isEmpty()) {
            realDownload(configs, "配置文件", () -> nextStage(apiUrlField.getText().trim()));
        } else {
            appendLog("跳过下载配置文件\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void executeDownloadRequiredModsStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> requiredMods = currentResponse.getData().stream()
            .filter(mod -> "mods".equals(mod.getCatelog()) 
                && mod.isRequired() 
                && !"libs".equals(mod.getSubject()))
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个必备mod:\n", requiredMods.size()));
        for (ModInfo mod : requiredMods) {
            appendLog(String.format("  - %s (%s)\n", mod.getFriendlyName(), mod.getSubject()));
        }
        
        if (autoDownloadCheckbox.isSelected() && !requiredMods.isEmpty()) {
            realDownload(requiredMods, "必备mod", () -> nextStage(apiUrlField.getText().trim()));
        } else {
            appendLog("跳过下载必备mod\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void executeSelectOptionalModsStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> optionalMods = currentResponse.getData().stream()
            .filter(mod -> "mods".equals(mod.getCatelog()) && !mod.isRequired())
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个可选mod:\n", optionalMods.size()));
        
        if (!optionalMods.isEmpty()) {
            // Filter out mods that already exist with correct hash
            List<ModInfo> availableMods = optionalMods.stream()
                .filter(mod -> !FileDownloader.fileExists(mod))
                .collect(Collectors.toList());
            
            appendLog(String.format("其中 %d 个需要下载，%d 个已存在\n", 
                availableMods.size(), optionalMods.size() - availableMods.size()));
            
            if (availableMods.isEmpty()) {
                appendLog("所有可选mod都已存在，跳过选择\n");
                nextStage(apiUrlField.getText().trim());
            } else {
                // Show selection dialog for available mods only
                SwingUtilities.invokeLater(() -> {
                    showOptionalModSelectionDialog(availableMods, optionalMods, () -> nextStage(apiUrlField.getText().trim()));
                });
            }
        } else {
            appendLog("没有可选mod\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void executeDownloadResourcePacksStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> resourcePacks = currentResponse.getData().stream()
            .filter(mod -> "resourcepacks".equals(mod.getCatelog()) && mod.isRequired())
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个必备资源包:\n", resourcePacks.size()));
        for (ModInfo mod : resourcePacks) {
            appendLog(String.format("  - %s\n", mod.getFriendlyName()));
        }
        
        if (autoDownloadCheckbox.isSelected() && !resourcePacks.isEmpty()) {
            realDownload(resourcePacks, "资源包", () -> nextStage(apiUrlField.getText().trim()));
        } else {
            appendLog("跳过下载资源包\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void executeDownloadShadersStage() {
        if (currentResponse == null) {
            nextStage(apiUrlField.getText().trim());
            return;
        }
        
        List<ModInfo> shaders = currentResponse.getData().stream()
            .filter(mod -> "shaderpacks".equals(mod.getCatelog()))
            .collect(Collectors.toList());
        
        appendLog(String.format("发现 %d 个光影包:\n", shaders.size()));
        for (ModInfo mod : shaders) {
            appendLog(String.format("  - %s\n", mod.getFriendlyName()));
        }
        
        if (autoDownloadCheckbox.isSelected() && !shaders.isEmpty()) {
            realDownload(shaders, "光影包", () -> nextStage(apiUrlField.getText().trim()));
        } else {
            appendLog("跳过下载光影包\n");
            nextStage(apiUrlField.getText().trim());
        }
    }
    
    private void updateTable(ModListResponse response) {
        tableModel.setRowCount(0);
        
        for (ModInfo mod : response.getData()) {
            // Check file status
            String statusText = getFileStatusText(mod);
            
            Object[] row = {
                mod.getFriendlyName(),           // 名称
                getCatalogDisplayName(mod.getCatelog()), // 类型
                getSubjectDisplayName(mod.getSubject()), // 类别
                mod.isRequired(),                 // 是否必需
                statusText                        // 状态
            };
            tableModel.addRow(row);
        }
    }
    
    /**
     * 将catalog映射为中文显示名称
     */
    private String getCatalogDisplayName(String catalog) {
        if (catalog == null) return "未知";
        
        switch (catalog.toLowerCase()) {
            case "mods":
                return "模组";
            case "resourcepacks":
                return "资源";
            case "shaderpacks":
                return "光影";
            case "config":
                return "配置";
            default:
                return catalog;
        }
    }
    
    /**
     * 将subject映射为中文显示名称
     */
    private String getSubjectDisplayName(String subject) {
        if (subject == null) return "未知";
        
        switch (subject.toLowerCase()) {
            case "beautify":
                return "美化";
            case "enhance":
                return "增强";
            case "gamemode":
                return "玩法";
            case "libs":
                return "前置";
            case "others":
                return "其他";
            default:
                return subject;
        }
    }
    
    private void updateStats(ModListResponse response) {
        SwingUtilities.invokeLater(() -> {
            // Total count
            int totalItems = response.getData().size();
            totalItemsLabel.setText("总数量: " + totalItems);
            
            // Catalog statistics (main classification)
            Map<String, Long> catalogCount = response.getData().stream()
                .filter(mod -> mod.getCatelog() != null)
                .collect(Collectors.groupingBy(
                    ModInfo::getCatelog, 
                    Collectors.counting()
                ));
            
            // Update catalog counts
            modCountLabel.setText("Mods: " + catalogCount.getOrDefault("mods", 0L));
            resourcePackCountLabel.setText("资源包: " + catalogCount.getOrDefault("resourcepacks", 0L));
            shaderCountLabel.setText("光影包: " + catalogCount.getOrDefault("shaderpacks", 0L));
            configCountLabel.setText("配置文件: " + catalogCount.getOrDefault("config", 0L));
            
            // Required vs Optional
            long requiredCount = response.getData().stream()
                .filter(ModInfo::isRequired)
                .count();
            long optionalCount = response.getData().size() - requiredCount;
            
            requiredCountLabel.setText("必需项: " + requiredCount);
            optionalCountLabel.setText("可选项: " + optionalCount);
        });
    }
    
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
    
    
    // Helper methods for workflow
    private void updateStage(WorkflowStage stage) {
        SwingUtilities.invokeLater(() -> {
            stageProgressBar.setValue(stage.getStepIndex());
            stageProgressBar.setString(String.format("%d/%d - %s", 
                stage.getStepIndex() + 1, WorkflowStage.getTotalSteps(), stage.getDescription()));
            stageLabel.setText("当前阶段: " + stage.getDescription());
        });
    }
    
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("[%tT] %s", System.currentTimeMillis(), message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void realDownload(List<ModInfo> items, String itemType, Runnable onComplete) {
        if (items.isEmpty()) {
            onComplete.run();
            return;
        }
        
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(items.size());
        progressBar.setValue(0);
        progressBar.setString(String.format("正在下载%s (0/%d)", itemType, items.size()));
        
        SwingWorker<Integer, String> worker = new SwingWorker<Integer, String>() {
            private int successCount = 0;
            private int failCount = 0;
            
            @Override
            protected Integer doInBackground() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    ModInfo item = items.get(i);
                    final int currentIndex = i;
                    
                    // Update progress for current item
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(currentIndex);
                        progressBar.setString(String.format("正在下载%s (%d/%d) - %s", 
                            itemType, currentIndex + 1, items.size(), item.getFriendlyName()));
                    });
                    
                    // Check if file is already ready (correct name and hash)
                    if (FileDownloader.isFileReady(item)) {
                        publish(String.format("跳过已就绪文件: %s", item.getFriendlyName()));
                        successCount++;
                        continue;
                    }
                    
                    // Download the file
                    boolean downloadSuccess = FileDownloader.downloadFile(item, 
                        (bytesRead, totalBytes) -> {
                            // Individual file progress (optional - could be shown in detailed view)
                        });
                    
                    if (downloadSuccess) {
                        successCount++;
                        publish(String.format("✓ 下载成功: %s", item.getFriendlyName()));
                    } else {
                        failCount++;
                        publish(String.format("✗ 下载失败: %s - %s", item.getFriendlyName(), 
                            (item.getRes() != null ? item.getRes() : "无下载链接")));
                    }
                    
                    // Update main progress
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(currentIndex + 1);
                        progressBar.setString(String.format("正在下载%s (%d/%d)", 
                            itemType, currentIndex + 1, items.size()));
                    });
                }
                
                return successCount;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                // Add log messages to the log area
                for (String message : chunks) {
                    appendLog(message + "\n");
                }
            }
            
            @Override
            protected void done() {
                try {
                    int successful = get();
                    progressBar.setVisible(false);
                    
                    final String summary = String.format("%s下载完成: 成功 %d/%d", 
                        itemType, successful, items.size());
                    
                    if (failCount > 0) {
                        final String finalSummary = summary + String.format("，失败 %d 个", failCount);
                        appendLog("⚠️ " + finalSummary + "\n");
                        
                        // Show warning dialog for failures
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MinecraftResSyncGUI.this,
                                finalSummary + "\n请检查网络连接或下载链接。",
                                "下载完成（有失败项）", JOptionPane.WARNING_MESSAGE);
                        });
                    } else {
                        appendLog("✓ " + summary + "\n");
                    }
                    
                    // Update statistics with directory sizes
                    updateDownloadStats();
                    
                } catch (Exception e) {
                    appendLog("下载过程中发生错误: " + e.getMessage() + "\n");
                    e.printStackTrace();
                } finally {
                    onComplete.run();
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Get file status text for display in table
     */
    private String getFileStatusText(ModInfo modInfo) {
        try {
            Path targetDir = FileDownloader.createDirectoryStructure(modInfo.getCatelog());
            if (targetDir == null) return "未知";
            
            String fileName = getTargetFileName(modInfo);
            String expectedHash = modInfo.getHash();
            
            // Check if target file exists and has correct hash
            Path targetFile = targetDir.resolve(fileName);
            if (Files.exists(targetFile)) {
                if (expectedHash != null && !expectedHash.trim().isEmpty()) {
                    String actualHash = calculateFileHash(targetFile);
                    if (expectedHash.equalsIgnoreCase(actualHash)) {
                        return "✓ 就绪";
                    } else {
                        return "⚠ 需更新";
                    }
                } else {
                    return "✓ 就绪";
                }
            }
            
            // Check if any other file in the directory has the matching hash
            if (expectedHash != null && !expectedHash.trim().isEmpty() && 
                Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                try {
                    Path matchingFile = Files.list(targetDir)
                        .filter(Files::isRegularFile)
                        .filter(file -> {
                            try {
                                String fileHash = calculateFileHash(file);
                                return expectedHash.equalsIgnoreCase(fileHash);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
                        
                    if (matchingFile != null) {
                        return "🔄 需重命名";
                    }
                } catch (Exception e) {
                    // Ignore directory scanning errors
                }
            }
            
            return "📥 待下载";
            
        } catch (Exception e) {
            return "未知";
        }
    }
    
    /**
     * Generate target file name using friendly_name with appropriate extension (UI helper)
     */
    private String getTargetFileName(ModInfo modInfo) {
        String friendlyName = modInfo.getFriendlyName();
        if (friendlyName == null || friendlyName.trim().isEmpty()) {
            // Fallback to raw name if friendly name is not available
            String rawName = modInfo.getRawName();
            if (rawName != null && !rawName.trim().isEmpty()) {
                return rawName;
            }
            // Last resort: extract from URL
            return extractFileNameFromUrl(modInfo.getRes());
        }
        
        // Determine extension from raw_name or URL
        String extension = getFileExtension(modInfo);
        
        // Combine friendly name with extension
        return friendlyName + extension;
    }
    
    /**
     * Get file extension from raw_name or URL (UI helper)
     */
    private String getFileExtension(ModInfo modInfo) {
        // First try to get extension from raw_name
        String rawName = modInfo.getRawName();
        if (rawName != null && !rawName.trim().isEmpty()) {
            int dotIndex = rawName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < rawName.length() - 1) {
                return rawName.substring(dotIndex);
            }
        }
        
        // Fallback to URL
        String url = modInfo.getRes();
        if (url != null) {
            try {
                String path = new URL(url).getPath();
                int dotIndex = path.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < path.length() - 1) {
                    return path.substring(dotIndex);
                }
            } catch (Exception e) {
                // Ignore URL parsing errors
            }
        }
        
        // Default extension based on catalog
        String catalog = modInfo.getCatelog();
        if (catalog != null) {
            switch (catalog.toLowerCase()) {
                case "mods":
                    return ".jar";
                case "resourcepacks":
                case "shaderpacks":
                    return ".zip";
                case "config":
                    return ".json";
            }
        }
        
        // Final fallback
        return ".jar";
    }
    
    /**
     * Extract filename from URL (helper method for UI)
     */
    private String extractFileNameFromUrl(String urlStr) {
        try {
            String path = new URL(urlStr).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.isEmpty() ? "downloaded_file" : fileName;
        } catch (Exception e) {
            return "downloaded_file_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Calculate MD5 hash of a file (helper method for UI)
     */
    private String calculateFileHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            
            try (FileInputStream fis = new FileInputStream(file.toFile());
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private void updateDownloadStats() {
        SwingUtilities.invokeLater(() -> {
            String[] catalogs = {"mods", "resourcepacks", "shaderpacks", "config"};
            long totalSize = 0;
            StringBuilder downloadStatsLog = new StringBuilder();
            downloadStatsLog.append("下载统计:\n");
            
            for (String catalog : catalogs) {
                long size = FileDownloader.getDirectorySize(catalog);
                totalSize += size;
                if (size > 0) {
                    downloadStatsLog.append(String.format("  %s: %s\n", 
                        catalog.toUpperCase(), formatBytes(size)));
                }
            }
            
            if (totalSize > 0) {
                downloadStatsLog.append(String.format("  总计: %s\n", formatBytes(totalSize)));
                
                // Log download statistics instead of showing in stats panel
                appendLog(downloadStatsLog.toString());
            }
        });
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void showOptionalModSelectionDialog(List<ModInfo> availableMods, List<ModInfo> allOptionalMods, Runnable onComplete) {
        JDialog dialog = new JDialog(this, "选择可选MOD", true);
        dialog.setSize(900, 500); // 增大宽度以适应分屏布局
        dialog.setLocationRelativeTo(this);
        
        // 使用左右分屏布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6); // 左侧60%，右侧40%
        
        // === 左侧面板：选择表格 ===
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("可选MOD列表"));
        
        DefaultTableModel selectionModel = new DefaultTableModel(
            new String[]{"选择", "名称", "类别", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only allow editing checkbox for available mods
                if (column == 0) {
                    String status = (String) getValueAt(row, 3);
                    return !"✓ 已存在".equals(status);
                }
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return String.class;
            }
        };
        
        // Add all optional mods to the table (both available and existing)
        for (ModInfo mod : allOptionalMods) {
            boolean isAvailable = availableMods.contains(mod);
            String status = isAvailable ? "📥 待下载" : "✓ 已存在";
            
            selectionModel.addRow(new Object[]{
                false, // Default unselected
                mod.getFriendlyName(), 
                getSubjectDisplayName(mod.getSubject()), 
                status
            });
        }
        
        JTable selectionTable = new JTable(selectionModel);
        selectionTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // 选择
        selectionTable.getColumnModel().getColumn(1).setPreferredWidth(200); // 名称
        selectionTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 类别
        selectionTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 状态
        
        JScrollPane tableScrollPane = new JScrollPane(selectionTable);
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton selectAllButton = new JButton("全选");
        JButton deselectAllButton = new JButton("全不选");
        buttonPanel.add(selectAllButton);
        buttonPanel.add(deselectAllButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // === 右侧面板：MOD说明 ===
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("MOD说明"));
        
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        descriptionArea.setFont(getUIFont(Font.PLAIN, 12));
        descriptionArea.setText("请选择一个MOD查看说明");
        
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        rightPanel.add(descScrollPane, BorderLayout.CENTER);
        
        // 设置分屏面板
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        // 主对话框布局
        dialog.setLayout(new BorderLayout());
        dialog.add(splitPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel bottomButtonPanel = new JPanel(new FlowLayout());
        JButton confirmButton = new JButton("确认下载");
        JButton skipButton = new JButton("跳过");
        bottomButtonPanel.add(confirmButton);
        bottomButtonPanel.add(skipButton);
        dialog.add(bottomButtonPanel, BorderLayout.SOUTH);
        
        // === 事件监听器 ===
        // 表格选择事件监听器
        selectionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = selectionTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String modName = (String) selectionModel.getValueAt(selectedRow, 1);
                    ModInfo selectedMod = allOptionalMods.stream()
                        .filter(mod -> mod.getFriendlyName().equals(modName))
                        .findFirst()
                        .orElse(null);
                    
                    if (selectedMod != null) {
                        String description = selectedMod.getDescription();
                        if (description == null || description.trim().isEmpty()) {
                            descriptionArea.setText("暂无说明");
                        } else {
                            descriptionArea.setText(description);
                        }
                        // 设置文本域光标到顶部
                        descriptionArea.setCaretPosition(0);
                    }
                }
            }
        });
        
        selectAllButton.addActionListener(e -> {
            for (int i = 0; i < selectionModel.getRowCount(); i++) {
                String status = (String) selectionModel.getValueAt(i, 3);
                if ("📥 待下载".equals(status)) {
                    selectionModel.setValueAt(true, i, 0);
                }
            }
        });
        
        deselectAllButton.addActionListener(e -> {
            for (int i = 0; i < selectionModel.getRowCount(); i++) {
                String status = (String) selectionModel.getValueAt(i, 3);
                if ("📥 待下载".equals(status)) {
                    selectionModel.setValueAt(false, i, 0);
                }
            }
        });
        
        confirmButton.addActionListener(e -> {
            List<ModInfo> selectedMods = new java.util.ArrayList<>();
            for (int i = 0; i < selectionModel.getRowCount(); i++) {
                if ((Boolean) selectionModel.getValueAt(i, 0)) {
                    // Find the corresponding mod from allOptionalMods list
                    String modName = (String) selectionModel.getValueAt(i, 1);
                    ModInfo selectedMod = allOptionalMods.stream()
                        .filter(mod -> mod.getFriendlyName().equals(modName))
                        .findFirst()
                        .orElse(null);
                    if (selectedMod != null && availableMods.contains(selectedMod)) {
                        selectedMods.add(selectedMod);
                    }
                }
            }
            
            dialog.dispose();
            
            if (!selectedMods.isEmpty()) {
                appendLog(String.format("用户选择了 %d 个可选mod\n", selectedMods.size()));
                realDownload(selectedMods, "可选mod", onComplete);
            } else {
                appendLog("用户未选择任何可选mod\n");
                onComplete.run();
            }
        });
        
        skipButton.addActionListener(e -> {
            dialog.dispose();
            appendLog("跳过可选mod选择\n");
            onComplete.run();
        });
        
        dialog.setVisible(true);
    }
    
    private void completeWorkflow() {
        currentStage = WorkflowStage.COMPLETED;
        updateStage(currentStage);
        
        appendLog("\n=== 工作流程完成 ===\n");
        appendLog("所有阶段已完成，Minecraft资源同步工具已准备就绪\n");
        
        updateStatus("工作流程完成！");
        
        // Re-enable controls
        startWorkflowButton.setEnabled(true);
        fetchButton.setEnabled(true);
        
        // Show completion dialog
        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showOptionDialog(this,
                "工作流程已完成！\n所有资源已准备就绪，可以启动Minecraft了。\n\n点击确定将继续启动 Minecraft 主程序。",
                "完成",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"确定", "留在当前"},
                "确定");
            
            // 如果用户点击“确定”，则关闭应用程序
            if (result == JOptionPane.OK_OPTION) {
                appendLog("用户选择关闭应用程序并启动 Minecraft\n");
                // 退出应用程序
                System.exit(0);
            } else {
                appendLog("用户选择继续留在资源同步程序\n");
            }
        });
    }
    
    private void resetWorkflow() {
        currentStage = WorkflowStage.FETCH_DATA;
        updateStage(currentStage);
        startWorkflowButton.setEnabled(true);
        fetchButton.setEnabled(true);
        progressBar.setVisible(false);
        updateStatus("工作流程已重置");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MinecraftResSyncGUI().setVisible(true);
        });
    }
}
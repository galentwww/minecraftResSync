package com.minecraft.sync;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinecraftResSyncGUI extends JFrame {
    
    // UI Components
    private JTextField apiUrlField;
    private JButton fetchButton;
    private JProgressBar progressBar;
    private JTable modTable;
    private DefaultTableModel tableModel;
    private JTextArea statsArea;
    private JLabel statusLabel;
    private JToggleButton themeToggle;
    
    // Data
    private ModListResponse currentResponse;
    
    public MinecraftResSyncGUI() {
        initializeTheme();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultValues();
    }
    
    private void initializeTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
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
        apiUrlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // Buttons
        fetchButton = new JButton("Fetch Mod List");
        fetchButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        
        themeToggle = new JToggleButton("🌙 Dark");
        themeToggle.setSelected(true);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        
        // Table for mods
        String[] columnNames = {"ID", "Name", "Category", "Required", "Filename"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return Boolean.class;
                return String.class;
            }
        };
        
        modTable = new JTable(tableModel);
        modTable.setRowHeight(25);
        modTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        modTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        modTable.setRowSorter(new TableRowSorter<>(tableModel));
        
        // Set column widths
        modTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        modTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Name
        modTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Category
        modTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Required
        modTable.getColumnModel().getColumn(4).setPreferredWidth(300); // Filename
        
        // Stats area
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        statsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Status label
        statusLabel = new JLabel("Ready to fetch mod data");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel - URL input and controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        
        JPanel urlPanel = new JPanel(new BorderLayout(10, 0));
        urlPanel.add(new JLabel("API Endpoint:"), BorderLayout.WEST);
        urlPanel.add(apiUrlField, BorderLayout.CENTER);
        urlPanel.add(fetchButton, BorderLayout.EAST);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(themeToggle);
        
        topPanel.add(urlPanel, BorderLayout.CENTER);
        topPanel.add(controlPanel, BorderLayout.EAST);
        topPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center panel - Split pane with table and stats
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        
        // Left: Table
        JScrollPane tableScrollPane = new JScrollPane(modTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Mod List"));
        splitPane.setLeftComponent(tableScrollPane);
        
        // Right: Stats
        JScrollPane statsScrollPane = new JScrollPane(statsArea);
        statsScrollPane.setBorder(BorderFactory.createTitledBorder("Statistics"));
        statsScrollPane.setPreferredSize(new Dimension(250, 0));
        splitPane.setRightComponent(statsScrollPane);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Bottom panel - Status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(5, 15, 15, 15));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        fetchButton.addActionListener(e -> fetchModData());
        
        apiUrlField.addActionListener(e -> fetchModData());
        
        themeToggle.addActionListener(e -> toggleTheme());
    }
    
    private void setDefaultValues() {
        updateStatus("Ready to fetch mod data");
    }
    
    private void fetchModData() {
        String apiUrl = apiUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an API endpoint URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Disable controls during fetch
        fetchButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Fetching data...");
        updateStatus("Connecting to: " + apiUrl);
        
        // Use SwingWorker for background task
        SwingWorker<ModListResponse, Void> worker = new SwingWorker<ModListResponse, Void>() {
            @Override
            protected ModListResponse doInBackground() throws Exception {
                try {
                    String jsonResponse = HttpClient.get(apiUrl);
                    return JsonParser.parseModListFromString(jsonResponse);
                } catch (Exception e) {
                    // Try fallback to local file
                    updateStatus("Remote failed, trying local fallback...");
                    return JsonParser.parseModListFromFile("/Users/galentwww/IdeaProjects/minecraftResSync/modlist.json");
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
                        updateStatus("Successfully loaded " + response.getData().size() + " mods");
                    } else {
                        updateStatus("Failed to load mod data");
                        JOptionPane.showMessageDialog(MinecraftResSyncGUI.this, 
                            "Failed to fetch data from both remote API and local file", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    updateStatus("Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(MinecraftResSyncGUI.this, 
                        "Error: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Re-enable controls
                    fetchButton.setEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                }
            }
        };
        
        worker.execute();
    }
    
    private void updateTable(ModListResponse response) {
        tableModel.setRowCount(0);
        
        for (ModInfo mod : response.getData()) {
            Object[] row = {
                mod.getId(),
                mod.getFriendlyName(),
                mod.getSubject(),
                mod.isRequired(),
                mod.getRawName()
            };
            tableModel.addRow(row);
        }
    }
    
    private void updateStats(ModListResponse response) {
        StringBuilder stats = new StringBuilder();
        
        stats.append("=== MOD STATISTICS ===\n\n");
        stats.append(String.format("Total Mods: %d\n\n", response.getData().size()));
        
        // Category statistics
        Map<String, Long> subjectCount = response.getData().stream()
            .filter(mod -> mod.getSubject() != null)
            .collect(Collectors.groupingBy(
                ModInfo::getSubject, 
                Collectors.counting()
            ));
        
        stats.append("By Category:\n");
        subjectCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> 
                stats.append(String.format("  %-12s: %d\n", entry.getKey(), entry.getValue()))
            );
        
        // Required vs Optional
        long requiredCount = response.getData().stream()
            .filter(ModInfo::isRequired)
            .count();
        long optionalCount = response.getData().size() - requiredCount;
        
        stats.append(String.format("\nRequired: %d\n", requiredCount));
        stats.append(String.format("Optional: %d\n", optionalCount));
        
        statsArea.setText(stats.toString());
    }
    
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
    
    private void toggleTheme() {
        try {
            if (themeToggle.isSelected()) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                themeToggle.setText("🌙 Dark");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                themeToggle.setText("☀️ Light");
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MinecraftResSyncGUI().setVisible(true);
        });
    }
}
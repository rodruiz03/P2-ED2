import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;

// ============================================================================
// APLICACIÓN PRINCIPAL
// ============================================================================
public class AppCompresion extends JFrame {

    // UI principal
    private JTextField processPathField;
    private JRadioButton compressRadio;
    private JRadioButton encryptRadio;
    private JRadioButton bothRadio;
    private JPasswordField processPasswordField;

    private JTextField recoverSourceField;
    private JTextField recoverDestField;
    private JPasswordField recoverPasswordField;

    private JTable logTable;
    private DefaultTableModel logTableModel;

    private JProgressBar progressBar;
    private JLabel statusLabel;

    private Preferences prefs;
    private String lastDirectory;

    // Gestor de archivos
    private final FileManagerCore fileManager;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new AppCompresion().setVisible(true);
        });
    }

    public AppCompresion() {
        this.fileManager = new FileManagerCore();
        this.prefs = Preferences.userNodeForPackage(AppCompresion.class);
        this.lastDirectory = prefs.get("lastDirectory", System.getProperty("user.home"));

        initUI();
    }

    private void initUI() {
        setTitle("Sistema de Gestión de Archivos Seguros");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 13));
        tabs.addTab("Procesar", createProcessPanel());
        tabs.addTab("Recuperar", createRecoverPanel());
        tabs.addTab("Log", createLogPanel());

        add(tabs, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        statusLabel = new JLabel("Listo");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("Archivo");
        JMenuItem clearLogItem = new JMenuItem("Limpiar log");
        clearLogItem.addActionListener(e -> clearLog());
        JMenuItem saveLogItem = new JMenuItem("Guardar log...");
        saveLogItem.addActionListener(e -> saveLogToFile());
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(clearLogItem);
        fileMenu.add(saveLogItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Ayuda");
        JMenuItem aboutItem = new JMenuItem("Acerca de");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Sistema de gestión de archivos seguros\n"
                        + "Compresión LZ77 + Huffman y cifrado XOR.\n"
                        + "Proyecto de Aplicación.",
                "Acerca de",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(helpMenu);
        return bar;
    }

    private JPanel createProcessPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Origen",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13))
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel pathLabel = new JLabel("Archivo / carpeta:");
        pathLabel.setFont(new Font("Arial", Font.BOLD, 13));
        topPanel.add(pathLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        processPathField = new JTextField(35);
        processPathField.setEditable(false);
        processPathField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        topPanel.add(processPathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        JButton fileBtn = new JButton("Archivo");
        styleButton(fileBtn, new Color(33, 150, 243));
        fileBtn.addActionListener(e -> chooseProcessFile());
        topPanel.add(fileBtn, gbc);

        gbc.gridx = 3;
        JButton folderBtn = new JButton("Carpeta");
        styleButton(folderBtn, new Color(255, 152, 0));
        folderBtn.addActionListener(e -> chooseProcessFolder());
        topPanel.add(folderBtn, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Operación",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13))
        );

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,5,8,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        compressRadio = new JRadioButton("Solo compresión (.cmp)");
        encryptRadio = new JRadioButton("Solo encriptación (.enc)");
        bothRadio = new JRadioButton("Compresión + Encriptación (.ec)", true);

        ButtonGroup group = new ButtonGroup();
        group.add(compressRadio);
        group.add(encryptRadio);
        group.add(bothRadio);

        compressRadio.setFont(new Font("Arial", Font.PLAIN, 13));
        encryptRadio.setFont(new Font("Arial", Font.PLAIN, 13));
        bothRadio.setFont(new Font("Arial", Font.PLAIN, 13));

        centerPanel.add(compressRadio, gbc);
        gbc.gridy++;
        centerPanel.add(encryptRadio, gbc);
        gbc.gridy++;
        centerPanel.add(bothRadio, gbc);

        gbc.gridy++;
        JLabel pwdLabel = new JLabel("Contraseña (para encriptación):");
        pwdLabel.setFont(new Font("Arial", Font.BOLD, 13));
        centerPanel.add(pwdLabel, gbc);

        gbc.gridx = 1;
        processPasswordField = new JPasswordField(18);
        centerPanel.add(processPasswordField, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton processBtn = new JButton("PROCESAR");
        processBtn.setFont(new Font("Arial", Font.BOLD, 15));
        processBtn.setPreferredSize(new Dimension(220, 45));
        styleButton(processBtn, new Color(76, 175, 80));
        processBtn.addActionListener(e -> onProcess());
        bottomPanel.add(processBtn);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRecoverPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Origen y destino",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13))
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel srcLabel = new JLabel("Origen (.cmp/.enc/.ec o carpeta):");
        srcLabel.setFont(new Font("Arial", Font.BOLD, 13));
        topPanel.add(srcLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        recoverSourceField = new JTextField(35);
        recoverSourceField.setEditable(false);
        recoverSourceField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        topPanel.add(recoverSourceField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        JButton browseSource = new JButton("Examinar");
        styleButton(browseSource, new Color(33, 150, 243));
        browseSource.addActionListener(e -> chooseRecoverSource());
        topPanel.add(browseSource, gbc);

        gbc.gridy++;
        gbc.gridx = 0;

        JLabel destLabel = new JLabel("Carpeta destino:");
        destLabel.setFont(new Font("Arial", Font.BOLD, 13));
        topPanel.add(destLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        recoverDestField = new JTextField(35);
        recoverDestField.setEditable(false);
        recoverDestField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        topPanel.add(recoverDestField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        JButton browseDest = new JButton("Examinar");
        styleButton(browseDest, new Color(255, 152, 0));
        browseDest.addActionListener(e -> chooseRecoverDest());
        topPanel.add(browseDest, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Datos adicionales",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13))
        );

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,5,8,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel pwdLabel = new JLabel("Contraseña (si aplica):");
        pwdLabel.setFont(new Font("Arial", Font.BOLD, 13));
        centerPanel.add(pwdLabel, gbc);

        gbc.gridx = 1;
        recoverPasswordField = new JPasswordField(20);
        centerPanel.add(recoverPasswordField, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton recoverBtn = new JButton("RECUPERAR");
        recoverBtn.setFont(new Font("Arial", Font.BOLD, 15));
        recoverBtn.setPreferredSize(new Dimension(220, 45));
        styleButton(recoverBtn, new Color(244, 67, 54));
        recoverBtn.addActionListener(e -> onRecover());
        bottomPanel.add(recoverBtn);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {
                "Fecha/Hora",
                "Operación",
                "Archivo original",
                "Archivo resultante",
                "Duración (s)",
                "Tasa compresión (%)",
                "Estado"
        };
        logTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        logTable = new JTable(logTableModel);
        logTable.setFillsViewportHeight(true);
        logTable.setRowHeight(22);
        logTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(logTable);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
    }

    // ========================================================================
    // ACCIONES DE UI
    // ========================================================================

    private void chooseProcessFile() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            processPathField.setText(f.getAbsolutePath());
            lastDirectory = f.getParent();
            prefs.put("lastDirectory", lastDirectory);
        }
    }

    private void chooseProcessFolder() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            processPathField.setText(f.getAbsolutePath());
            lastDirectory = f.getAbsolutePath();
            prefs.put("lastDirectory", lastDirectory);
        }
    }

    private void chooseRecoverSource() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            recoverSourceField.setText(f.getAbsolutePath());
            lastDirectory = f.isDirectory() ? f.getAbsolutePath() : f.getParent();
            prefs.put("lastDirectory", lastDirectory);
        }
    }

    private void chooseRecoverDest() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            recoverDestField.setText(f.getAbsolutePath());
            lastDirectory = f.getAbsolutePath();
            prefs.put("lastDirectory", lastDirectory);
        }
    }

    private void setBusy(boolean busy, String message) {
        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);
        statusLabel.setText(message);
    }

    private void onProcess() {
        String pathStr = processPathField.getText();
        if (pathStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o carpeta para procesar.");
            return;
        }
        File source = new File(pathStr);
        if (!source.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo o carpeta no existe.");
            return;
        }

        String op;
        if (compressRadio.isSelected()) op = "compress";
        else if (encryptRadio.isSelected()) op = "encrypt";
        else op = "both";

        String password = null;
        if (!op.equals("compress")) {
            password = new String(processPasswordField.getPassword());
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar una contraseña para encriptar.");
                return;
            }
        }

        final String operation = op;
        final String pwd = password;

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                setBusy(true, "Procesando...");
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                try {
                    fileManager.processPath(
                            source.toPath(),
                            operation,
                            pwd,
                            (percent, msg) -> {
                                setProgress(percent);
                                publish(percent);
                                SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                            },
                            AppCompresion.this::addLogEntry
                    );
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            AppCompresion.this,
                            "Procesamiento completado.",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE
                    ));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            AppCompresion.this,
                            "Error durante el procesamiento: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    ));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int v = chunks.get(chunks.size() - 1);
                    progressBar.setValue(v);
                }
            }

            @Override
            protected void done() {
                setBusy(false, "Listo");
                progressBar.setValue(0);
            }
        };
        worker.execute();
    }

    private void onRecover() {
        String srcStr = recoverSourceField.getText();
        String destStr = recoverDestField.getText();
        if (srcStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o carpeta de origen.");
            return;
        }
        if (destStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione una carpeta de destino.");
            return;
        }

        File src = new File(srcStr);
        File destDir = new File(destStr);
        if (!destDir.exists() || !destDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "La carpeta de destino no es válida.");
            return;
        }

        String password = new String(recoverPasswordField.getPassword());
        if (password.isEmpty()) {
            // Permitimos vacío en caso de archivos solo comprimidos (.cmp)
            password = null;
        }

        final String pwd = password;

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                setBusy(true, "Recuperando...");
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                try {
                    fileManager.recoverPath(
                            src.toPath(),
                            destDir.toPath(),
                            pwd,
                            (percent, msg) -> {
                                setProgress(percent);
                                publish(percent);
                                SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                            },
                            AppCompresion.this::addLogEntry
                    );
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            AppCompresion.this,
                            "Recuperación completada.",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE
                    ));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            AppCompresion.this,
                            "Error durante la recuperación: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    ));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int v = chunks.get(chunks.size() - 1);
                    progressBar.setValue(v);
                }
            }

            @Override
            protected void done() {
                setBusy(false, "Listo");
                progressBar.setValue(0);
            }
        };
        worker.execute();
    }

    private void clearLog() {
        logTableModel.setRowCount(0);
    }

    private void saveLogToFile() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setSelectedFile(new File("log_app_compresion.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                // Encabezado
                for (int j = 0; j < logTableModel.getColumnCount(); j++) {
                    pw.print(logTableModel.getColumnName(j));
                    if (j < logTableModel.getColumnCount() - 1) pw.print(";");
                }
                pw.println();

                // Filas
                for (int i = 0; i < logTableModel.getRowCount(); i++) {
                    for (int j = 0; j < logTableModel.getColumnCount(); j++) {
                        Object val = logTableModel.getValueAt(i, j);
                        String text = val == null ? "" : val.toString();
                        // Evitar que un ; dentro del texto rompa el CSV
                        text = text.replace(";", ",");
                        pw.print(text);
                        if (j < logTableModel.getColumnCount() - 1) pw.print(";");
                    }
                    pw.println();
                }

                JOptionPane.showMessageDialog(this,
                        "Log exportado correctamente como CSV.",
                        "Exportación completada",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar log: " + ex.getMessage());
            }
        }
    }

    private void addLogEntry(LogEntry entry) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logTableModel.addRow(new Object[]{
                entry.timestamp.format(fmt),
                entry.operation,
                entry.sourceName,
                entry.destName,
                String.format("%.2f", entry.durationSeconds),
                entry.compressionRate,
                entry.success ? "OK" : "ERROR"
        });
    }

    // ========================================================================
    // MODELO DE LOG Y CALLBACK
    // ========================================================================

    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }

    public interface LogCallback {
        void onLog(LogEntry entry);
    }

    public static class LogEntry {
        public final LocalDateTime timestamp;
        public final String operation;
        public final String sourceName;
        public final String destName;
        public final double durationSeconds;
        public final String compressionRate;
        public final boolean success;

        public LogEntry(String operation,
                        String sourceName,
                        String destName,
                        double durationSeconds,
                        String compressionRate,
                        boolean success) {
            this.timestamp = LocalDateTime.now();
            this.operation = operation;
            this.sourceName = sourceName;
            this.destName = destName;
            this.durationSeconds = durationSeconds;
            this.compressionRate = compressionRate;
            this.success = success;
        }
    }

    // ========================================================================
    // NÚCLEO DE PROCESAMIENTO DE ARCHIVOS
    // ========================================================================

    public static class FileManagerCore {

        private final LZ77Compressor lz77 = new LZ77Compressor();
        private final HuffmanCompressor huffman = new HuffmanCompressor();
        private final SimpleEncryptor encryptor = new SimpleEncryptor();

        public void processPath(Path path,
                                String operation,
                                String password,
                                ProgressCallback progress,
                                LogCallback logger) throws IOException {
            if (Files.isDirectory(path)) {
                processDirectory(path, operation, password, progress, logger);
            } else {
                processSingleFile(path, operation, password, progress, logger);
            }
        }

        private void processDirectory(Path dir,
                                      String operation,
                                      String password,
                                      ProgressCallback progress,
                                      LogCallback logger) throws IOException {
            java.util.List<Path> files = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
                stream.filter(Files::isRegularFile).forEach(files::add);
            }
            int total = files.size();
            int count = 0;
            for (Path p : files) {
                count++;
                int percent = (int)((count * 100.0) / total);
                progress.onProgress(percent, "Procesando " + p.getFileName());
                processSingleFile(p, operation, password, progress, logger);
            }
        }

        private void processSingleFile(Path file,
                                       String operation,
                                       String password,
                                       ProgressCallback progress,
                                       LogCallback logger) {
            String fileName = file.getFileName().toString();
            String baseName = fileName;
            int idx = fileName.lastIndexOf('.');
            if (idx != -1) baseName = fileName.substring(0, idx);

            Path parent = file.getParent();
            if (parent == null) parent = Paths.get(".");

            long start = System.currentTimeMillis();

            try {
                byte[] data = Files.readAllBytes(file);

                if (operation.equals("compress")) {
                    progress.onProgress(10, "Aplicando LZ77...");
                    java.util.List<LZ77Compressor.Token> tokens = lz77.compress(data, progress);
                    progress.onProgress(40, "Codificando tokens...");
                    byte[] tokenBytes = lz77.serializeTokens(tokens);
                    progress.onProgress(60, "Aplicando Huffman...");
                    byte[] compressed = huffman.compress(tokenBytes);
                    Path out = parent.resolve(baseName + ".cmp");
                    Files.write(out, compressed);

                    long originalSize = Files.size(file);
                    long finalSize = Files.size(out);
                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;
                    String rate = originalSize > 0
                            ? String.format("%.2f", 100.0 * (1.0 - (finalSize / (double) originalSize)))
                            : "-";

                    logger.onLog(new LogEntry(
                            "COMPRESS",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            rate,
                            true
                    ));
                } else if (operation.equals("encrypt")) {
                    if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("Contraseña requerida para encriptar.");
                    }
                    progress.onProgress(20, "Encriptando archivo...");
                    byte[] encrypted = encryptor.encrypt(data, password, (p,msg) -> {
                        progress.onProgress(20 + p/2, msg);
                    });
                    Path out = parent.resolve(baseName + ".enc");
                    Files.write(out, encrypted);

                    long finalSize = Files.size(out);
                    long originalSize = Files.size(file);
                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;

                    logger.onLog(new LogEntry(
                            "ENCRYPT",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            "-",  // no aplica tasa de compresión
                            true
                    ));
                } else { // both
                    if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("Contraseña requerida para encriptar.");
                    }
                    progress.onProgress(10, "Aplicando LZ77...");
                    java.util.List<LZ77Compressor.Token> tokens = lz77.compress(data, progress);
                    progress.onProgress(40, "Codificando tokens...");
                    byte[] tokenBytes = lz77.serializeTokens(tokens);
                    progress.onProgress(60, "Aplicando Huffman...");
                    byte[] compressed = huffman.compress(tokenBytes);
                    progress.onProgress(80, "Encriptando resultado...");
                    byte[] encrypted = encryptor.encrypt(compressed, password, (p,msg) -> {
                        progress.onProgress(80 + p/5, msg);
                    });
                    Path out = parent.resolve(baseName + ".ec");
                    Files.write(out, encrypted);

                    long originalSize = Files.size(file);
                    long finalSize = Files.size(out); // ec ~ tamaño comprimido
                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;
                    String rate = originalSize > 0
                            ? String.format("%.2f", 100.0 * (1.0 - (finalSize / (double) originalSize)))
                            : "-";

                    logger.onLog(new LogEntry(
                            "COMPRESS+ENCRYPT",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            rate,
                            true
                    ));
                }

                progress.onProgress(100, "Archivo procesado: " + fileName);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.onLog(new LogEntry(
                        "ERROR_PROCESS",
                        file.getFileName().toString(),
                        "",
                        0.0,
                        "-",
                        false
                ));
            }
        }

        public void recoverPath(Path src,
                                Path destDir,
                                String password,
                                ProgressCallback progress,
                                LogCallback logger) throws IOException {
            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
            }
            if (Files.isDirectory(src)) {
                java.util.List<Path> files = new ArrayList<>();
                try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
                    stream.filter(Files::isRegularFile).forEach(files::add);
                }
                int total = files.size();
                int count = 0;
                for (Path p : files) {
                    count++;
                    int percent = (int)((count * 100.0) / total);
                    progress.onProgress(percent, "Recuperando " + p.getFileName());
                    recoverSingleFile(p, destDir, password, progress, logger);
                }
            } else {
                recoverSingleFile(src, destDir, password, progress, logger);
            }
        }

        private void recoverSingleFile(Path file,
                                       Path destDir,
                                       String password,
                                       ProgressCallback progress,
                                       LogCallback logger) {
            String fileName = file.getFileName().toString();
            long start = System.currentTimeMillis();

            try {
                byte[] data = Files.readAllBytes(file);

                if (fileName.endsWith(".cmp")) {
                    progress.onProgress(20, "Decodificando Huffman...");
                    byte[] tokenBytes = huffman.decompress(data);
                    progress.onProgress(60, "Aplicando LZ77 (descompresión)...");
                    java.util.List<LZ77Compressor.Token> tokens = lz77.deserializeTokens(tokenBytes);
                    byte[] original = lz77.decompress(tokens);
                    String base = fileName.substring(0, fileName.length() - 4);
                    Path out = destDir.resolve(base);
                    Files.write(out, original);

                    long compressedSize = Files.size(file);
                    long originalSize = Files.size(out);
                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;
                    String rate = originalSize > 0
                            ? String.format("%.2f", 100.0 * (1.0 - (compressedSize / (double) originalSize)))
                            : "-";

                    logger.onLog(new LogEntry(
                            "DECOMPRESS",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            rate,
                            true
                    ));
                } else if (fileName.endsWith(".enc")) {
                    if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("Contraseña requerida para desencriptar.");
                    }
                    progress.onProgress(20, "Desencriptando archivo...");
                    byte[] original = encryptor.decrypt(data, password, (p,msg) -> {
                        progress.onProgress(20 + p/2, msg);
                    });
                    String base = fileName.substring(0, fileName.length() - 4);
                    Path out = destDir.resolve(base);
                    Files.write(out, original);

                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;

                    logger.onLog(new LogEntry(
                            "DECRYPT",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            "-",
                            true
                    ));
                } else if (fileName.endsWith(".ec")) {
                    if (password == null || password.isEmpty()) {
                        throw new IllegalArgumentException("Contraseña requerida para desencriptar.");
                    }
                    progress.onProgress(20, "Desencriptando...");
                    byte[] compressed = encryptor.decrypt(data, password, (p,msg) -> {
                        progress.onProgress(20 + p/3, msg);
                    });
                    progress.onProgress(50, "Decodificando Huffman...");
                    byte[] tokenBytes = huffman.decompress(compressed);
                    progress.onProgress(80, "Aplicando LZ77 (descompresión)...");
                    java.util.List<LZ77Compressor.Token> tokens = lz77.deserializeTokens(tokenBytes);
                    byte[] original = lz77.decompress(tokens);
                    String base = fileName.substring(0, fileName.length() - 3);
                    Path out = destDir.resolve(base);
                    Files.write(out, original);

                    long compressedSize = Files.size(file); // ec ~ tamaño comprimido + cifrado
                    long originalSize = Files.size(out);
                    double durationSeconds = (System.currentTimeMillis() - start) / 1000.0;
                    String rate = originalSize > 0
                            ? String.format("%.2f", 100.0 * (1.0 - (compressedSize / (double) originalSize)))
                            : "-";

                    logger.onLog(new LogEntry(
                            "DECRYPT+DECOMPRESS",
                            file.getFileName().toString(),
                            out.getFileName().toString(),
                            durationSeconds,
                            rate,
                            true
                    ));
                } else {
                    logger.onLog(new LogEntry(
                            "SKIP",
                            file.getFileName().toString(),
                            "",
                            0.0,
                            "-",
                            false
                    ));
                }

                progress.onProgress(100, "Archivo recuperado: " + fileName);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.onLog(new LogEntry(
                        "ERROR_RECOVER",
                        file.getFileName().toString(),
                        "",
                        0.0,
                        "-",
                        false
                ));
            }
        }
    }

    // ========================================================================
    // LZ77 SIMPLE
    // ========================================================================

    public static class LZ77Compressor {

        public static class Token {
            public final int offset;
            public final int length;
            public final byte next;

            public Token(int offset, int length, byte next) {
                this.offset = offset;
                this.length = length;
                this.next = next;
            }
        }

        private static final int WINDOW_SIZE = 4096;
        private static final int LOOKAHEAD_SIZE = 32;

        public java.util.List<Token> compress(byte[] data, ProgressCallback progress) {
            java.util.List<Token> tokens = new java.util.ArrayList<>();
            int i = 0;
            int n = data.length;

            while (i < n) {
                int bestLength = 0;
                int bestOffset = 0;

                int startWindow = Math.max(0, i - WINDOW_SIZE);

                int maxLen = Math.min(LOOKAHEAD_SIZE, n - i);

                for (int j = startWindow; j < i; j++) {
                    int len = 0;
                    while (len < maxLen && data[j + len] == data[i + len]) {
                        len++;
                    }
                    if (len > bestLength) {
                        bestLength = len;
                        bestOffset = i - j;
                    }
                }

                if (bestLength >= 3 && i + bestLength < n) {
                    byte next = data[i + bestLength];
                    tokens.add(new Token(bestOffset, bestLength, next));
                    i += bestLength + 1;
                } else {
                    tokens.add(new Token(0, 0, data[i]));
                    i++;
                }

                if (progress != null && n > 0 && i % 1000 == 0) {
                    int pct = (int)((i * 100.0) / n);
                    progress.onProgress(pct, "LZ77: " + pct + "%");
                }
            }

            if (progress != null) {
                progress.onProgress(100, "LZ77 completado");
            }

            return tokens;
        }

        public byte[] decompress(java.util.List<Token> tokens) {
            java.util.List<Byte> out = new java.util.ArrayList<>();
            for (Token t : tokens) {
                if (t.offset == 0 && t.length == 0) {
                    out.add(t.next);
                } else {
                    int size = out.size();
                    int start = size - t.offset;
                    for (int i = 0; i < t.length; i++) {
                        byte b = out.get(start + i);
                        out.add(b);
                    }
                    out.add(t.next);
                }
            }
            byte[] result = new byte[out.size()];
            for (int i = 0; i < out.size(); i++) {
                result[i] = out.get(i);
            }
            return result;
        }

        public byte[] serializeTokens(java.util.List<Token> tokens) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(tokens.size());
            for (Token t : tokens) {
                dos.writeShort(t.offset);
                dos.writeShort(t.length);
                dos.writeByte(t.next);
            }
            dos.flush();
            return baos.toByteArray();
        }

        public java.util.List<Token> deserializeTokens(byte[] data) throws IOException {
            java.util.List<Token> tokens = new java.util.ArrayList<>();
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                int offset = dis.readShort() & 0xFFFF;
                int length = dis.readShort() & 0xFFFF;
                byte next = dis.readByte();
                tokens.add(new Token(offset, length, next));
            }
            return tokens;
        }
    }

    // ========================================================================
    // HUFFMAN
    // ========================================================================

    public static class HuffmanCompressor {

        private static class Node implements Comparable<Node> {
            byte value;
            int freq;
            Node left;
            Node right;

            Node(byte value, int freq) {
                this.value = value;
                this.freq = freq;
            }

            Node(Node left, Node right) {
                this.value = 0;
                this.freq = left.freq + right.freq;
                this.left = left;
                this.right = right;
            }

            boolean isLeaf() {
                return left == null && right == null;
            }

            @Override
            public int compareTo(Node o) {
                return Integer.compare(this.freq, o.freq);
            }
        }

        public byte[] compress(byte[] data) throws IOException {
            if (data.length == 0) return data;

            int[] freq = new int[256];
            for (byte b : data) {
                freq[b & 0xFF]++;
            }

            PriorityQueue<Node> pq = new PriorityQueue<>();
            for (int i = 0; i < 256; i++) {
                if (freq[i] > 0) {
                    pq.add(new Node((byte)i, freq[i]));
                }
            }

            if (pq.size() == 1) {
                // caso especial: solo un símbolo
                pq.add(new Node((byte)0, 0));
            }

            while (pq.size() > 1) {
                Node a = pq.poll();
                Node b = pq.poll();
                pq.add(new Node(a, b));
            }

            Node root = pq.poll();

            Map<Byte, String> codes = new HashMap<>();
            buildCodes(root, "", codes);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // encabezado: longitud original
            dos.writeInt(data.length);

            // número de símbolos
            int symbols = 0;
            for (int f : freq) if (f > 0) symbols++;
            dos.writeInt(symbols);

            // tabla (valor, frecuencia)
            for (int i = 0; i < 256; i++) {
                if (freq[i] > 0) {
                    dos.writeByte((byte)i);
                    dos.writeInt(freq[i]);
                }
            }

            // datos comprimidos
            int currentByte = 0;
            int bitCount = 0;
            for (byte b : data) {
                String code = codes.get(b);
                for (int i = 0; i < code.length(); i++) {
                    currentByte <<= 1;
                    if (code.charAt(i) == '1') {
                        currentByte |= 1;
                    }
                    bitCount++;
                    if (bitCount == 8) {
                        dos.writeByte((byte)currentByte);
                        bitCount = 0;
                        currentByte = 0;
                    }
                }
            }

            if (bitCount > 0) {
                currentByte <<= (8 - bitCount);
                dos.writeByte((byte)currentByte);
            }

            dos.flush();
            return baos.toByteArray();
        }

        private void buildCodes(Node node, String prefix, Map<Byte, String> codes) {
            if (node.isLeaf()) {
                codes.put(node.value, prefix.length() == 0 ? "0" : prefix);
                return;
            }
            buildCodes(node.left, prefix + "0", codes);
            buildCodes(node.right, prefix + "1", codes);
        }

        public byte[] decompress(byte[] compressed) throws IOException {
            if (compressed.length == 0) return compressed;

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(compressed));
            int originalLength = dis.readInt();
            int symbols = dis.readInt();

            int[] freq = new int[256];
            for (int i = 0; i < symbols; i++) {
                int val = dis.readByte() & 0xFF;
                int f = dis.readInt();
                freq[val] = f;
            }

            PriorityQueue<Node> pq = new PriorityQueue<>();
            for (int i = 0; i < 256; i++) {
                if (freq[i] > 0) {
                    pq.add(new Node((byte)i, freq[i]));
                }
            }
            if (pq.size() == 1) {
                pq.add(new Node((byte)0, 0));
            }
            while (pq.size() > 1) {
                Node a = pq.poll();
                Node b = pq.poll();
                pq.add(new Node(a, b));
            }
            Node root = pq.poll();

            byte[] out = new byte[originalLength];
            int outPos = 0;

            Node current = root;

            java.util.List<Byte> remaining = new java.util.ArrayList<>();
            while (dis.available() > 0) {
                remaining.add(dis.readByte());
            }

            for (byte bVal : remaining) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (bVal >> i) & 1;
                    current = (bit == 0) ? current.left : current.right;
                    if (current.isLeaf()) {
                        out[outPos++] = current.value;
                        if (outPos == originalLength) {
                            return out;
                        }
                        current = root;
                    }
                }
            }

            return out;
        }
    }

    // ========================================================================
    // CIFRADO XOR SIMPLE
    // ========================================================================

    public static class SimpleEncryptor {

        public interface InternalProgress {
            void update(int progress, String message);
        }

        public byte[] encrypt(byte[] data, String password, InternalProgress callback) {
            if (password == null || password.isEmpty() || data == null) return data;
            byte[] key = deriveKey(password);
            return xorCipher(data, key, callback);
        }

        public byte[] decrypt(byte[] data, String password, InternalProgress callback) {
            if (password == null || password.isEmpty() || data == null) return data;
            byte[] key = deriveKey(password);
            return xorCipher(data, key, callback);
        }

        private byte[] xorCipher(byte[] data, byte[] key, InternalProgress callback) {
            byte[] out = new byte[data.length];
            int keyLen = key.length;
            for (int i = 0; i < data.length; i++) {
                int k = key[i % keyLen] & 0xFF;
                int extra = (i * 31) & 0xFF;
                out[i] = (byte)((data[i] & 0xFF) ^ k ^ extra);

                if (callback != null && data.length > 0 && i % 50000 == 0) {
                    int pct = (int)((i * 100.0) / data.length);
                    callback.update(pct, "Cifrado: " + pct + "%");
                }
            }
            if (callback != null) {
                callback.update(100, "Cifrado completado");
            }
            return out;
        }

        private byte[] deriveKey(String password) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                return digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                return new byte[]{0};
            }
        }
    }
}

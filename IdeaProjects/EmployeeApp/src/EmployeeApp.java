import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EmployeeApp extends JFrame {

    private JTable employeeTable;
    private DefaultTableModel tableModel;
    private ArrayList<Employee> employees;
    private Connection connection; // Соединение с базой данных

    public EmployeeApp() {
        setTitle("Employee Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        employees = new ArrayList<>();

        // Подключаемся к базе данных
        connectToDatabase();

        // Инициализация таблицы
        String[] columnNames = {"#", "Name", "Birth Date", "Salary", "Delete"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Только столбец "Delete" редактируемый
            }
        };
        employeeTable = new JTable(tableModel);
        employeeTable.setRowHeight(30);

        // Устанавливаем рендерер и редактор для кнопки "Delete"
        employeeTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        employeeTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox(), employeeTable, this));

        JScrollPane scrollPane = new JScrollPane(employeeTable);
        add(scrollPane, BorderLayout.CENTER);

        // Панель для кнопки "Add"
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton addButton = new JButton("Add Employee");
        addButton.setBackground(new Color(50, 120, 200));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> showAddDialog());
        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные из базы данных
        loadEmployeesFromDatabase();

        setVisible(true);
    }

    // Подключение к базе данных
    private void connectToDatabase() {
        try {
            String url = "jdbc:sqlite:employees.db";
            connection = DriverManager.getConnection(url);
            System.out.println("Connected to SQLite database.");
            initializeDatabase(); // Создаем таблицу, если её нет
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to connect to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Создание таблицы, если её нет
    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS employees (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " name TEXT NOT NULL,\n"
                + " birth_date TEXT NOT NULL,\n"
                + " salary REAL NOT NULL\n"
                + ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    // Загрузка сотрудников из базы данных
    private void loadEmployeesFromDatabase() {
        String sql = "SELECT id, name, birth_date, salary FROM employees";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String birthDateStr = rs.getString("birth_date");
                double salary = rs.getDouble("salary");

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date birthDate = dateFormat.parse(birthDateStr);

                employees.add(new Employee(id, name, birthDate, salary));
            }
            updateTable();
        } catch (Exception e) {
            System.err.println("Error loading employees: " + e.getMessage());
        }
    }

    // Добавление сотрудника в базу данных
    private void addEmployeeToDatabase(String name, Date birthDate, double salary) {
        String sql = "INSERT INTO employees(name, birth_date, salary) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String birthDateStr = dateFormat.format(birthDate);

            pstmt.setString(1, name);
            pstmt.setString(2, birthDateStr);
            pstmt.setDouble(3, salary);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding employee: " + e.getMessage());
        }
    }

    // Удаление сотрудника из базы данных
    private void deleteEmployeeFromDatabase(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting employee: " + e.getMessage());
        }
    }

    // Добавление сотрудника
    private void addEmployee(String name, Date birthDate, double salary) {
        addEmployeeToDatabase(name, birthDate, salary); // Добавляем в базу данных
        loadEmployeesFromDatabase(); // Перезагружаем данные из базы
    }

    // Обновление таблицы
    private void updateTable() {
        tableModel.setRowCount(0); // Очищаем таблицу

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        for (int i = 0; i < employees.size(); i++) {
            Employee employee = employees.get(i);
            Object[] rowData = {
                    i + 1,
                    employee.getName(),
                    dateFormat.format(employee.getBirthDate()),
                    "$" + employee.getSalary(),
                    "Delete"
            };
            tableModel.addRow(rowData);
        }
    }

    // Диалог добавления сотрудника
    private void showAddDialog() {
        JDialog addDialog = new JDialog(this, "Add Employee", true);
        addDialog.setSize(400, 300);
        addDialog.setLocationRelativeTo(this);
        addDialog.setLayout(new GridLayout(5, 2, 10, 10));

        JLabel nameLabel = new JLabel("Name:");
        JTextField nameField = new JTextField();

        JLabel birthDateLabel = new JLabel("Birth Date (dd.MM.yyyy):");
        JTextField birthDateField = new JTextField();

        JLabel salaryLabel = new JLabel("Salary:");
        JTextField salaryField = new JTextField();

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(200, 50, 50));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> addDialog.dispose());

        JButton addButton = new JButton("Add");
        addButton.setBackground(new Color(50, 120, 200));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Name cannot be empty");
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                Date birthDate = dateFormat.parse(birthDateField.getText());

                double salary = Double.parseDouble(salaryField.getText());

                addEmployee(name, birthDate, salary);
                addDialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(addDialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        addDialog.add(nameLabel);
        addDialog.add(nameField);
        addDialog.add(birthDateLabel);
        addDialog.add(birthDateField);
        addDialog.add(salaryLabel);
        addDialog.add(salaryField);
        addDialog.add(cancelButton);
        addDialog.add(addButton);

        addDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EmployeeApp::new);
    }

    // Рендерер для кнопки "Delete"
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(200, 50, 50));
            setForeground(Color.WHITE);
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Delete");
            return this;
        }
    }

    // Редактор для кнопки "Delete"
    static class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private JTable employeeTable;
        private EmployeeApp parent;

        public ButtonEditor(JCheckBox checkBox, JTable employeeTable, EmployeeApp parent) {
            super(checkBox);
            this.employeeTable = employeeTable;
            this.parent = parent;
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(new Color(200, 50, 50));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int selectedRow = employeeTable.getEditingRow();
                Employee employee = parent.employees.get(selectedRow);
                parent.deleteEmployeeFromDatabase(employee.getId()); // Удаляем из базы данных
                parent.employees.remove(selectedRow); // Удаляем из списка
                parent.updateTable(); // Обновляем таблицу
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // Класс Employee
    static class Employee {
        private int id;
        private String name;
        private Date birthDate;
        private double salary;

        public Employee(int id, String name, Date birthDate, double salary) {
            this.id = id;
            this.name = name;
            this.birthDate = birthDate;
            this.salary = salary;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Date getBirthDate() {
            return birthDate;
        }

        public double getSalary() {
            return salary;
        }
    }
}
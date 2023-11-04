import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class Menu {
    private HashMap<String,String> clients = new HashMap(); // Username, password
    private final FileWriter writer;

    public Menu() throws IOException {
        String dbPath = "./db/clientsDB.csv";
        writer = new FileWriter(dbPath);
    }


    private void addClient(String username, String password) throws IOException {
        this.clients.put(username,password);

        writer.append(username+','+password+'\n');
        writer.close();
    }

    private Boolean existsClient(String username, String password) {
        if (this.clients.get(username)==null || this.clients.get(username) != password) {
            return false;
        }
        return true;
    }

    public static void loginPage(Menu s) {
        JFrame frame = new JFrame("Login Page");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(new JLabel()); // Empty label for spacing
        panel.add(registerButton);
        panel.add(loginButton);

        frame.add(panel, BorderLayout.CENTER);

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (!s.existsClient(username,password)) {
                    try {
                        s.addClient(username,password);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                mainMenuPage(s);

            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (s.existsClient(username,password)) {
                    mainMenuPage(s);
                } else {
                    JOptionPane.showMessageDialog(frame, "Login failed. Please check your credentials.");
                }
            }
        });

        frame.setVisible(true);
    }


    public static void mainMenuPage(Menu s) {
        JFrame frame = new JFrame("Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        JButton runCode = new JButton("Run Code");
        JButton seeOutputs = new JButton("See Outputs");

        panel.add(runCode);
        panel.add(seeOutputs);

        frame.add(panel, BorderLayout.CENTER);

        runCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCodePage(s);
            }
        });

        seeOutputs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputsPage(s);
            }
        });

        frame.setVisible(true);
    }

    public static void runCodePage(Menu s) {
        JFrame frame = new JFrame("Run Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JLabel urlLabel = new JLabel("Url Label");
        JTextField urlField = new JTextField();
        JButton runCode = new JButton("Run Code");
        JButton goBack = new JButton("<-");

        panel.add(goBack, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(); // Create a new panel for center alignment
        centerPanel.setLayout(new GridLayout(1, 1));
        centerPanel.add(urlLabel);
        centerPanel.add(urlField);
        centerPanel.add(new JLabel()); // Empty label for spacing
        centerPanel.add(runCode);

        // Set the center panel in the center of the panel
        panel.add(centerPanel, BorderLayout.CENTER);

        frame.add(panel, BorderLayout.CENTER);

        runCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: pass the code to the main server
            }
        });

        frame.setVisible(true);
    }


    public static void outputsPage(Menu s) {
        JFrame frame = new JFrame("Outputs Page");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel goBackPanel = new JPanel(new BorderLayout());

        JButton goBack = new JButton("<-");
        goBack.add(goBack, BorderLayout.WEST);



        // TODO: get the list from the main server
        DefaultListModel<String> outputListModel = new DefaultListModel<>();

        JList<String> outputList = new JList<>(outputListModel);
        JScrollPane scrollPane = new JScrollPane(outputList);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Add a double-click listener to the list items to handle item selection
        outputList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = outputList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String selectedOutput = outputListModel.getElementAt(index);
                        // Handle the selected output (e.g., display it or navigate to another page)
                        JOptionPane.showMessageDialog(frame, "Selected: " + selectedOutput);
                    }
                }
            }
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }




    public static void main(String[] args) throws IOException {
        Menu s = new Menu();
        loginPage(s);
    }

}

import javax.annotation.processing.SupportedSourceVersion;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Menu {
    private HashMap<String,String> clients = new HashMap(); // Username, password
    private FileWriter writer = null;

    private String activeUser;

    public void setActiveUser(String activeUser) {
        this.activeUser = activeUser;
    }

    public String getActiveUser() {
        return activeUser;
    }

    public Menu() throws IOException {
        String dbPath = "./db/clientsDB.csv";
        writer = new FileWriter(dbPath, true);
        BufferedReader reader = new BufferedReader(new FileReader(dbPath));

        String line;

        while((line = reader.readLine()) != null) {

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                clients.put(key, value);
            }
        }

    }


    private void addClient(String username, String password) throws IOException {
        this.clients.put(username,password);

        writer.append(username+','+password+'\n');
        writer.close();

        setActiveUser(username);
    }

    private Boolean existsClient(String username, String password) {
        if (this.clients.get(username)==null || !this.clients.get(username).equals(password)) {
            return false;
        }
        setActiveUser(username);
        return true;
    }


    private static void loginPage(Menu s, Client c, ClientHandler clientHandler) {
        JFrame frame = new JFrame("Login Page");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);

        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (!s.existsClient(username, password)) {
                    try {
                        s.addClient(username, password);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                mainMenuPage(s,c, clientHandler);
                frame.setVisible(false);
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (s.existsClient(username, password)) {
                    mainMenuPage(s,c,clientHandler);
                    frame.setVisible(false);
                } else {
                    JOptionPane.showMessageDialog(frame, "Login failed. Please check your credentials.");
                }
            }
        });

        frame.setVisible(true);
    }



    private static void mainMenuPage(Menu s, Client c, ClientHandler clientHandler) {
        JFrame frame = new JFrame("Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
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
                runCodePage(s,c,clientHandler);
                frame.setVisible(false);
            }
        });

        seeOutputs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputsPage(s,c, clientHandler);
                frame.setVisible(false);
            }
        });

        frame.setVisible(true);
    }

    private static void runCodePage(Menu s, Client c, ClientHandler clientHandler) {
        JFrame frame = new JFrame("Run Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
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
                ClientFileInfo cfi = new ClientFileInfo(c,urlField.getText());
                c.addClientFileInfo(cfi);
                clientHandler.toDoFiles.insertToDoFile(cfi); // Acho que isto aqui deveria ser algo como uima mensagem por socket do cliente a pedri ao clientHandler para guardar aquela info, visto isto acho que tb não faz sentido o client estar no ClientHandler, sendo que eles so se comunicam, mas são independentes
                frame.setVisible(false);
            }
        });

        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainMenuPage(s,c,clientHandler);
                frame.setVisible(false);
            }
        });


        frame.setVisible(true);
    }


    private static void outputsPage(Menu s, Client c, ClientHandler clientHandler) { //TODO: Incompleto, "pseudocodigo"
        JFrame frame = new JFrame("Outputs Page");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel goBackPanel = new JPanel(new BorderLayout());

        JButton goBack = new JButton("<-");
        goBack.add(goBack, BorderLayout.WEST);

        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainMenuPage(s,c, clientHandler);
                frame.setVisible(false);
            }
        });


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


    public static void deploy(Client c, ClientHandler clientHandler) throws IOException {
        Menu m = new Menu();
        loginPage(m,c, clientHandler);
    }

    /*
    public static void main(String[] args) throws IOException {
        Menu m = new Menu();
        Client c = new Client();
        loginPage(m,c);
    }
    */
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.SupportedSourceVersion;


public class Menu {
    private Client client; // Client que está a usar o Menu
    private Clients clients; // HashMap<String,Client> dos clientes, não sei se tem/deve estar aqui,  só para conseguir obter o Client após fazer login/register

    public void setActiveUser(String name) {
        this.client = this.clients.getClient(name);
    }

    public Client getClient() {
        return this.client;
    }

    public Menu() throws IOException {
    }

    public int addClient(String username, String password) throws IOException {
        return this.client.regUser(username, password);
    }

    public int existsClient(String username, String password) throws IOException {
        return this.client.hasUser(username, password);
    }


    private void loginPage(Client c) {
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

        // Register Button
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    if (existsClient(username, password) == 1) { // Se não existir um cliente com esse username
                        try {
                            if(addClient(username, password) != 0){ // Register falhou TODO: Verificar, acho que não é preciso esta verificação, já se faz isso antes
                                JOptionPane.showMessageDialog(frame, "Register failed. Please change your username.");
                            } else {
                                mainMenuPage(c);
                                frame.setVisible(false);
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "Account " + username + " already exists. Please change your username or log in.");
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Login Button
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    if (existsClient(username, password) == 0) { // Dados do login corretos
                        mainMenuPage(c);
                        frame.setVisible(false);
                    } else if (existsClient(username, password) == 1){ // Username não existe
                        JOptionPane.showMessageDialog(frame, "Username " + username + " does not exist. Please change it or register");
                    } else if (existsClient(username, password) == 2){ // Password incorreta
                        JOptionPane.showMessageDialog(frame, "Password incorrect. Please try agaain");
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        frame.setVisible(true);
    }

    private static void mainMenuPage(Client c) {
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

        // Run Code Button
        runCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCodePage(c);
                frame.setVisible(false);
            }
        });

        // See Outputs Button
        seeOutputs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputsPage(c);
                frame.setVisible(false);
            }
        });

        frame.setVisible(true);
    }

    private static void runCodePage(Client c) {
        JFrame frame = new JFrame("Run Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JButton goBack = new JButton("<-");
        JLabel urlLabel = new JLabel("URL Label ↓");
        JTextField urlField = new JTextField();
        JButton chooseFile = new JButton("Choose File");
        JButton runCode = new JButton("Run Code");

        panel.add(goBack, BorderLayout.WEST);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(1, 2)); // One row, two columns

        // Painel URL com o Label e com o Field para input
        JPanel urlPanel = new JPanel(new GridLayout(2, 1));
        urlPanel.add(urlLabel);
        urlPanel.add(urlField);

        centerPanel.add(urlPanel);
        centerPanel.add(chooseFile);
        centerPanel.add(runCode);

        // Alinha o texto do Label URL Label
        urlLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        frame.add(panel, BorderLayout.CENTER);

        // Run Code Button
        runCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(urlField.getText().isEmpty()){
                        JOptionPane.showMessageDialog(frame, "URL is empty. Please try again");
                    }
                    else {
                        int[] result = c.sendCode(urlField.getText());
                        if (result[0] == 1) { // Ficheiro não encontrado
                            JOptionPane.showMessageDialog(frame, "URL is not valid. Please try again");
                        } else if (result[0] == 0){
                            int var5 = result[1];
                            JOptionPane.showMessageDialog(frame, "Working on your code, we are expecting to resolve it in " + var5 + " seconds.");
                            runCodePage(c);
                            frame.setVisible(false);
                        } else { // Caso geral, erro desconhecido
                            JOptionPane.showMessageDialog(frame, "Error");
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Choose File Button
        chooseFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = chooser.getSelectedFile();
                    urlField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        // Go Back Button
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainMenuPage(c);
                frame.setVisible(false);
            }
        });


        frame.setVisible(true);
    }


    private static void outputsPage(Client c) { //TODO: Incompleto, "pseudocodigo"
        JFrame frame = new JFrame("Outputs Page");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());

        JButton goBack = new JButton("<-");

        panel.add(goBack, BorderLayout.WEST);

        // Go Back Button
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainMenuPage(c);
                frame.setVisible(false);
            }
        });


        // TODO: get the list from the main server
        DefaultListModel<String> outputListModel = new DefaultListModel<>();

        JList<String> outputList = new JList<>(outputListModel);
        JScrollPane scrollPane = new JScrollPane(outputList);

        panel.add(scrollPane, BorderLayout.CENTER);

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

        frame.add(panel);
        frame.setVisible(true);
    }

    public void deploy(Client c) throws IOException {
        this.client = c;
        loginPage(this.client);
    }

}

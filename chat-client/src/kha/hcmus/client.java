package kha.hcmus;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;


public class client {
    private static MyWindow wnd = null;
    public static void main(String[] args) {
        // write your code here
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                wnd = new MyWindow();
                wnd.setVisible(true);
            }
        });
    }

}

class MyWindow extends JFrame {
    private JLabel welcome_name;
    private JTextField ip_tf;
    private JTextField port_tf;
    private JTextArea message;
    private JList jl;
    private String name;
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Thread receiveMessage;
    public MyWindow() {

        super("Chat application");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                MyWindow.this.setVisible(false);
                MyWindow.this.dispose();
            }
        });
        setPreferredSize(new Dimension(640, 480));
        setLayout(new BorderLayout());
        // set gridbaglayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;

        // create top JPanel
        JPanel jp = new JPanel();
        jp.setLayout(new GridBagLayout());
        jp.add(new JLabel("Host: "), gbc);
        gbc.gridy++;
        jp.add(new JLabel("Port: "), gbc);

        gbc.gridx++;
        gbc.gridy = 0;

        ip_tf = new JTextField("localhost",10);
        jp.add(ip_tf, gbc);
        gbc.gridx++;

        jp.add(Box.createRigidArea(new Dimension(25, 0)));
        gbc.gridx++;
        //gbc.gridheight = GridBagConstraints.VERTICAL;
        JButton connect_btn = new JButton("Connect");
        connect_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        jp.add(connect_btn, gbc);
        gbc.gridx -= 2;
        gbc.gridy++;
        port_tf = new JTextField("7524",10);
        jp.add(port_tf, gbc);

        gbc.gridx++;

        jp.add(Box.createRigidArea(new Dimension(25, 0)));
        gbc.gridx++;

        JButton disconnect_btn = new JButton("Disconnect");
        disconnect_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("!quit");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "No connection to server");
                }
            }
        });
        jp.add(disconnect_btn,gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        welcome_name = new JLabel("");
        jp.add(welcome_name,gbc);
        // add Top panel
        add(jp, BorderLayout.NORTH);

        // create middle JPanel

        JPanel jp2 = new JPanel();
        jp2.setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        JTextPane jtp = new JTextPane();
        jtp.setBorder(new LineBorder(Color.ORANGE, 2));
        StyledDocument doc = jtp.getStyledDocument();

        SimpleAttributeSet left = new SimpleAttributeSet();
        StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
        StyleConstants.setForeground(left, Color.RED);

        SimpleAttributeSet right = new SimpleAttributeSet();
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setForeground(right, Color.BLUE);


        JScrollPane jsp = new JScrollPane(jtp);
        jp2.add(jsp, gbc);

        // add center panel
        add(jp2, BorderLayout.CENTER);

        // add bottom JPanel
        JPanel jp3 = new JPanel();
        jp3.setLayout(new FlowLayout());
        message = new JTextArea(2, 25);
        JScrollPane jsp1 = new JScrollPane(message);

        JButton send_btn = new JButton("Send");
        send_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    doc.insertString(doc.getLength(),"\n"+message.getText(),right);
                    doc.setParagraphAttributes(doc.getLength(), 1, right, false);
                    message.setText("");
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }

                try{
                    dos.writeUTF(message.getText() + "$" + jl.getSelectedValue().toString());
                } catch (NullPointerException | IOException E){
                    JOptionPane.showMessageDialog(null, "Failed to send message");
                }
            }
        });
        JButton sendFile_btn = new JButton("Send File");
        sendFile_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Disconnected from server");
                }
            }
        });

        jp3.add(jsp1);
        jp3.add(send_btn);
        jp3.add(sendFile_btn);
        //add bottom panel
        add(jp3, BorderLayout.SOUTH);

        // left panel
        JPanel jp4 = new JPanel();
        BoxLayout boxLayout = new BoxLayout(jp4, BoxLayout.Y_AXIS);
        jp4.setLayout(boxLayout);
        jp4.add(new JLabel("Clients"));
        jl = new JList();
        /*jl.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                jtp.setText("");
            }
        });*/
        JScrollPane jsp2 = new JScrollPane(jl);
        jsp2.setPreferredSize(new Dimension(110, 350));
        jp4.add(jsp2);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("!getlist");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Disconnected from server");
                }
            }
        });
        jp4.add(refresh);

        add(jp4, BorderLayout.WEST);

        pack();
        setLocationRelativeTo(null);

        receiveMessage = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String msg = dis.readUTF();
                        System.out.println(msg);
                        if (msg.equals("!getlist")) {
                            dos.writeUTF("OK");
                            ObjectInputStream ois = null;
                            try {
                                ois = new ObjectInputStream(dis);
                                Object object = ois.readObject();
                                String[] name = (String[]) object;
                                System.out.println("new jlist updated");
                                jl.setListData(name);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            } catch (ClassNotFoundException ex) {
                                ex.printStackTrace();
                            }
                        }
                        if (msg.equals("!message")){
                            msg = dis.readUTF();
                            doc.insertString(doc.getLength(),"\n"+msg,left);
                            doc.setParagraphAttributes(doc.getLength(), 1,left, false);
                        }
                        if (msg.equals("!file")){
                            String fileName = dis.readUTF();
                            long fileLength = dis.readLong();
                            int bytesRead;
                            byte[] buffer = new byte[1024];
                            OutputStream fout = new FileOutputStream(fileName);
                            DataInputStream fdis = new DataInputStream(s.getInputStream());

                            while (fileLength > 0 && (bytesRead = fdis.read(buffer, 0, (int)Math.min(buffer.length, fileLength))) != -1)
                            {
                                fout.write(buffer, 0, bytesRead);
                                fileLength -= bytesRead;
                            }
                            fout.close();
                            doc.insertString(doc.getLength(),"\n"+dis.readUTF(),left);
                            doc.setParagraphAttributes(doc.getLength(), 1,left, false);
                        }
                    // close socket if server error
                    } catch (IOException e) {
                        try {
                            dis.close();
                            dos.close();
                            s.close();
                        }catch (IOException E){
                            E.printStackTrace();
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private void connect() {
        try {
            InetAddress ip = InetAddress.getByName("localhost");

            this.s = new Socket(this.ip_tf.getText(), Integer.parseInt(this.port_tf.getText()));

            this.dis = new DataInputStream(s.getInputStream());
            this.dos = new DataOutputStream(s.getOutputStream());
            //name = JOptionPane.showInputDialog("Enter your name");
            // sign in
            JTextField username = new JTextField();
            JTextField password = new JPasswordField();
            Object[] message = {
                    "Username:", username,
                    "Password:", password
            };

            int option = JOptionPane.showOptionDialog(null, message, "Login",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new String[]{"Sign In","Sign Up"},"default");
            if (option == JOptionPane.OK_OPTION) {
                dos.writeUTF("!login");
                if (!username.getText().equals("") && !password.getText().equals("")) {
                    dos.writeUTF(username.getText());
                    dos.writeUTF(password.getText());

                    if (dis.readUTF().equals("OK")) {
                        name = username.getText();
                        welcome_name.setText("Current user: " + name);
                        dos.writeUTF(name);
                        receiveMessage.start();
                        return;
                    }
                    else{
                        JOptionPane.showMessageDialog(null, dis.readUTF());
                    }
                }

            } else {
                if (!username.getText().equals("") && !password.getText().equals("")) {
                    dos.writeUTF("!signup");
                    dos.writeUTF(username.getText());
                    dos.writeUTF(password.getText());

                    JOptionPane.showMessageDialog(null, dis.readUTF());
                }
            }

            dis.close();
            dos.close();
            s.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to server");
        }
    }

    private String openAs(){
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Open as");
        int selection = jfc.showOpenDialog(frame);
        if (selection == JFileChooser.APPROVE_OPTION){
            File file = jfc.getSelectedFile();
            return file.getAbsolutePath();
        }
        return "";
    }

    private void sendFile() throws IOException {
        String filepath = openAs();
        File file = new File(filepath);
        byte[] buffer = new byte[(int)file.length()];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DataInputStream fdis = new DataInputStream(bis);
        fdis.readFully(buffer,0,buffer.length);
        dos.writeUTF("!file");
        dos.writeUTF(file.getName());
        dos.writeLong(file.length());
        dos.write(buffer,0,buffer.length);

        // recipient name here
        dos.writeUTF(jl.getSelectedValue().toString());
    }
}
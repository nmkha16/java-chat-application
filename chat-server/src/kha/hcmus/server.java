package kha.hcmus;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static javax.swing.JOptionPane.showMessageDialog;

class Account{
    private String _username;
    private String _password;
    Account(String username,String password){
        _username = username;
        _password = password;
    }

    public String get_password() {
        return _password;
    }

    public String get_username() {
        return _username;
    }

    /*@Override
    public boolean equals(Object obj) {
        boolean flag= false;
        if (obj instanceof Account){
            Account x = (Account) obj;
            if (x._username != _username) return flag;
            if (x._username.equals(_username) && x._password.equals(_password)){
                return true;
            };
        }
        return flag;
    }*/
}

public class server {
    // attribute
    private static ServerSocket ss;
    private static Socket s;
    public static List<Connection_Handler> connectedClients = new ArrayList<>();
    private static List<Account> accountList = new ArrayList<>();

    public static List<Account> getAccountList(){return accountList;}

    public static void main(String[] args) {
        try{
            // read current data for account first
            getAccountList("account.csv");

            ss = new ServerSocket(7524);
            System.out.println("Server opened at port 7524");
            while (true){
                // accept client
                s = ss.accept();
                System.out.println("New client connected: " + s.getInetAddress() +"/"+s.getPort());

                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                // create an client object for socket
                Connection_Handler client = new Connection_Handler(s,dis,dos);
                // add object to List
                connectedClients.add(client);
                new Thread(client).start();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private static void getAccountList(String file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            String[] part;
            line = br.readLine(); // skip header of csv format

            while ((line = br.readLine())!=null){
                part = line.split(",");

                Account acc = new Account(part[0],part[1]);
                accountList.add(acc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Connection_Handler implements Runnable {
    private String username ="!temporary"; // set temp name in case client connect to server just to sign up
    private final Socket client_socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private String fileName;
    private long fileLength;
    // constructor
    public Connection_Handler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.client_socket = s;
        this.dis = dis;
        this.dos = dos;
    }

    @Override
    public void run() {
        String incoming = "";

        // handle client
        while (true) {
            try {
                incoming = dis.readUTF();
                System.out.println(incoming);
            } catch (IOException e) {
                closeClient();
            }

            if(incoming.equals("!login")){
                try {
                    String username = dis.readUTF();
                    String password = dis.readUTF();

                    Account acc= new Account(username,password);

                    for (Account account : server.getAccountList()){
                        if (username.equals(account.get_username())&&password.equals(account.get_password())){
                            dos.writeUTF("OK");
                            this.username = username;
                            continue;
                        }
                    }

                    // no match user/pass found
                    dos.writeUTF("Denied");
                    dos.writeUTF("Incorrect username or password");
                    continue;

                } catch (IOException e) {
                    closeClient();
                    break;
                }
            }

            else if (incoming.equals("!signup")){
                try {
                    String username =dis.readUTF();
                    String password= dis.readUTF();
                    Account acc= new Account(username,password);

                    boolean flag = false;
                    for (Account account : server.getAccountList()){
                        if (username.equals(account.get_username())){
                            dos.writeUTF("Request Denied");
                            flag = true;
                            break;
                        }
                    }
                    if (flag){continue;}
                    //skip adding new account due to existing username
                    server.getAccountList().add(acc);
                    dos.writeUTF("Sign up successfully!!!");
                } catch (IOException e) {
                    closeClient();
                    break;
                }

                //now store new account to file
                try {
                    FileWriter fw = new FileWriter("account.csv");
                    fw.write("username,password\n");
                    // write Account object to csv
                    for (Account p: server.getAccountList()){
                        fw.write(p.get_username()+","+p.get_password()+"\n");
                    }
                    fw.close();
                }catch (IOException e){
                    closeClient();
                    break;
                }

            }

            String[] names = new String[server.connectedClients.size()];
            if (incoming.equals("!quit")) {
                closeClient();
                break;
            }
            // send client current active list of clients
            else if (incoming.equals("!getlist")) {
                for (int i =0;i<server.connectedClients.size();i++){
                    try {
                        if (!server.connectedClients.get(i).username.equals(username))
                            names[i] = server.connectedClients.get(i).username;
                    }catch(Exception e){

                    }
                }
                try {
                    //send list of active client

                    //oos.writeObject(names);
                    dos.writeUTF("!getlist");
                    ObjectOutputStream oos = new ObjectOutputStream(dos);
                    if (dis.readUTF().equals("OK")) {
                        oos.writeObject(names);
                        continue;
                    }
                } catch (IOException e) {
                    closeClient();
                    break;
                }
            }
            else if (incoming.equals("!file")){
                try {
                    // server store the file
                    fileName = dis.readUTF();
                    fileLength = dis.readLong();
                    int bytesRead;
                    byte[] buffer = new byte[1024];
                    OutputStream fout = new FileOutputStream(fileName);
                    DataInputStream fdis = new DataInputStream(client_socket.getInputStream());
                    while (fileLength > 0 && (bytesRead = fdis.read(buffer, 0, (int)Math.min(buffer.length, fileLength))) != -1)
                    {
                        fout.write(buffer, 0, bytesRead);
                        fileLength -= bytesRead;
                    }
                    fout.flush();
                    fout.close();
                    // server send the file to the recipient
                    String recipient = dis.readUTF();

                    // find the recipient
                    for (Connection_Handler client : server.connectedClients) {
                        if (client.username.equals(recipient)) {
                            try {
                                client.dos.writeUTF("!file");

                                File file = new File(fileName);
                                buffer = new byte[(int)file.length()];
                                FileInputStream fis = new FileInputStream(file);
                                BufferedInputStream bis = new BufferedInputStream(fis);
                                fdis = new DataInputStream(bis);
                                fdis.readFully(buffer,0,buffer.length);
                                client.dos.writeUTF(file.getName());
                                client.dos.writeLong(file.length());
                                client.dos.write(buffer,0,buffer.length);

                                client.dos.writeUTF(username+": send you a new file: " + file.getName());

                            } catch (IOException e) {
                                closeClient();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    closeClient();
                    break;
                }

            }

            try {
                // recognize whose message is sent to
                StringTokenizer tokenizer = new StringTokenizer(incoming, "$");
                String mess = tokenizer.nextToken();
                String name = tokenizer.nextToken();
                // search which client to send this message
                for (Connection_Handler client : server.connectedClients) {
                    if (client.username.equals(name)) {
                        try {
                            client.dos.writeUTF("!message");
                            client.dos.writeUTF(username + ": " + mess);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch(Exception e){};
        }
    }
    private void closeClient(){
        for (int i = 0; i < server.connectedClients.size(); i++) {
            if (server.connectedClients.get(i).username.equals(username))
                server.connectedClients.remove(i);
        }
        // close the socket
        try {
            dos.close();
            dis.close();
            client_socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

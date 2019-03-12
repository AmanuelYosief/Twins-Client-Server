package twins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;

    private Writer writer;

    /**
     * Initialise a new Twins server. To start the server, call start().
     * @param port the port number on which the server will listen for connections
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * Start the server.
     * @throws IOException 
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server listening on port: " + port);
                Socket conn= serverSocket.accept();
                System.out.println("Connected to " + conn.getInetAddress() + ":" + conn.getPort());
                session(conn);
            }
        }
    }

    /**
     * Run a Twins protocol session over an established network connection.
     * @param connection the network connection
     * @throws IOException 
     */
    public void session(Socket connection) throws IOException {
        writer = new OutputStreamWriter(connection.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        //  CW specifies a persistent database and that is platform indepedent and created during code
        File file = new File("write.txt");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        
        
        
        String initialMessage;
        while ((initialMessage = reader.readLine()) != null) {
        if(initialMessage.equals("hello"))
        {
        sendMessage("What is your name?");
        String name =  reader.readLine();
         //  if <name> is already registered:
 
         try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            String contents = name;
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.err.println("User's name is " + name);
        sendMessage("When were you born?");
        String dateOfBirth = reader.readLine();
        System.err.println("User's dob is " + dateOfBirth);
        } else {
        initialMessage = "";
        }

       
        
        
        //  else if <name> is not already registered:
                // Server: When were you born?


        // TODO: replace this with the actual protocol logic
        
        // we got a client message, but we didn't look at it,
        // then we sent a completely invalid response!  
        //System.out.println("Closing connection");
        //connection.close();
    }
    }

    /**
     * Send a newline-terminated message on the output stream to the client.
     * @param msg the message to send, not including the newline
     * @throws IOException 
     */
    private void sendMessage(String msg) throws IOException {
        writer.write(msg);
        writer.write("\r\n"); // Windows! Differ to Linux's \n
        // this flush() is necessary, otherwise ouput is buffered locally and
        // won't be sent to the client until it is too late 
        writer.flush();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String usage = "Usage: java twins.Server [<port-number>] ";
        if (args.length > 1) {
            throw new Error(usage);
        }
        int port = 8123;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println("Server host: " + ip.getHostAddress() + " (" + ip.getHostName() + ")");
        } catch (IOException e) {
            System.err.println("could not determine local host name");
        }
        Server server = new Server(port);
        server.start();
        System.err.println("Server loop terminated!"); // not supposed to happen
    }
}

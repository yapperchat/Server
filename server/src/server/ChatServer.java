package server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import constraints.Constraints;
import core.application.Application;
import core.command.Command;
import core.command.CommandRegistrar;
import core.message.AttachmentType;
import core.message.Message;
import core.misc.Misc;
import server.commands.Commands;
import tools.Tools;
import window.ApplicationWindow;

public class ChatServer extends ApplicationWindow implements Application {
	
	private static final long serialVersionUID = 1L;

	private static final int PORT_NUMBER = 4444;
	
	protected static int textSize = 11;
    
    private String serverHost;
    private List<ClientOutThread> toClients;
    private List<ClientInThread> fromClients;
    
    protected HashSet<String> banList;
    
    protected CommandRegistrar commands;
    
    ServerSocket serverSocket;
    
    protected String log;
    
    private GridBagLayout layout;
   
    
    private JTextPane textPane;
    private Constraints c_textPane;
    private StyledDocument doc;
    private Style style;
    
    private JTextField textField;
    private Constraints c_textField;
    
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer();
    }

    public ChatServer() {
    	super("Server " + PORT_NUMBER);
        this.log = "Chat log";
        
        try {
        	this.serverHost = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        	this.serverHost = "";
        }
        Tools.setLookAndFeel();
        
        initActions();
        
        layout = new GridBagLayout();
    	setLayout(layout);
   
    	this.textPane = new JTextPane();
    	this.textPane.setSize(new Dimension(75 * textSize, 20 * textSize));
    	c_textPane = new Constraints(0, 1);
    	
        textPane.setEditable(false);
        textPane.setVisible(true);
        
        doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("style", null);
        
        JScrollPane scroll = new JScrollPane (textPane);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        add(scroll, c_textPane);
    	
    	textField = new JTextField();
        textField.setColumns(50);
        textField.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		process(textField.getText());
        	}
        });
        c_textField = new Constraints(0,2);
        add(textField, c_textField);
        
        //keyField = new JTextField();
        //keyField.setColumns(25);
        //c_keyField = new Constraints(0,0);
        //add(keyField, c_keyField);
        
        pack();
        setVisible(true);
        setResizable(false);
    }

    public List<ClientInThread> getClientOuts() {
        return fromClients;
    }
    
    public List<ClientOutThread> getClientIns() {
    	return toClients;
    }

    private void startServer(){
        toClients = new ArrayList<ClientOutThread>();
        fromClients = new ArrayList<ClientInThread>();
        serverSocket = null;
        
        try {
        	InetAddress addr = InetAddress.getByName(this.serverHost);
            serverSocket = new ServerSocket(PORT_NUMBER, 50, addr);
            acceptClients(serverSocket);
        } catch (IOException e){
            output("[ERROR] COULD NOT LISTEN ON PORT: " + PORT_NUMBER);
            this.stop();
        }
    }
    
    private void acceptClients(ServerSocket serverSocket) {

        output("[BEGIN] SERVER STARTING ON PORT: " + serverSocket.getLocalSocketAddress());
        while (this.isRunning()) {
            try{
                Socket socket = serverSocket.accept();
                output("[ACCEPT] ACCEPTED CLIENT AT: " + socket.getRemoteSocketAddress());
                
                ClientInThread clientIn = new ClientInThread(this, socket);
                ClientOutThread clientOut = new ClientOutThread(this, socket);
                
                Thread clientInThread = new Thread(clientIn);
                Thread clientOutThread = new Thread(clientOut);
                
                clientInThread.start();
                clientOutThread.start();
                
                //Message message = (Message) clientIn.in.readObject();
                
                
                clientOut.addNextMessage(new Message(this.log, Misc.getTime(), "[SERVER]", "<SERVER>"));
                
                toClients.add(clientOut);
                fromClients.add(clientIn);
            } catch (IOException ex) {
                output("[ERROR] ACCEPT FAILED ON: " + PORT_NUMBER);
            }
        }
    }
    
    private void initActions() {
    	this.commands = new CommandRegistrar();
    	this.banList = new HashSet<String>();
    	Commands commands = new Commands();
    	
    	commands.add(new Command() {
    		public void run(String[] args) {
    			output("[COMMAND] IP: " + serverSocket.getInetAddress().toString());
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/getip",
    					"/ip",
    			};
    		}
    		
    		public String getInfo() {
    			return "Gives the server's ip address";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			
    			for (int i = 0; i < fromClients.size(); i++) {
    				ClientInThread client = fromClients.get(i);
    				if (!client.isRunning()) {
    					fromClients.remove(client);
    					toClients.remove(toClients.get(i));
    				}
    			}
    			
    			output("[COMMAND] CLIENTS: " + fromClients.size());
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/clients",
    					"/cs",
    					"/listclients",
    					"/lclients",
    					"/ls",
    			};
    		}
    		
    		public String getInfo() {
    			return "Gives the number of connected clients";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			Message message = new Message(addArgs(args), Misc.getTime(), "[SERVER]", "<server>");
    			
    			send(message);
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/say",
    					"/s",
    			};
    		}
    		
    		public String getInfo() {
    			return "Sends out the specified message.";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			banList.add(args[0]);
    			output("Banned ID " + args[0]);
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/ban",
    					"/b",
    			};
    		}
    		
    		public String getInfo() {
    			return "Bans the specified ID";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			banList.remove(args[0]);
    			output("Unbanned ID " + args[0]);
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/unban",
    					"/ub",
    			};
    		}
    		
    		public String getInfo() {
    			return "Unbans the specified ID";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			output("[COMMAND] BANS:");
    			Iterator<String> iterator = banList.iterator();
    			
    			while (iterator.hasNext()) {
    				output(iterator.next());
    			}
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    					"/bans",
    					"/blist",
    					"/listbans",
    					"/lbans",
    			};
    		}
    		
    		public String getInfo() {
    			return "Lists all current bans";
    		}
    	});
    	this.commands.add(new Command() {
    		public void run(String[] args) {
    			args[0] = addArgs(args);
    			Serializable inst = new Kick();
    			Message message = new Message(("[Kicked]:" + args[0]), Misc.getTime(), "[SERVER]", "<server>", inst, AttachmentType.INSTRUCTION, args);
    			sendMessage(message);
    		}
    		
    		public String[] getTriggers() {
    			return new String[] {
    				"/kick"
    			};
    		}
    		
    		public String getInfo() {
    			return "Sends an alert with the specified message.";
    		}
    	});
    }
    
    public boolean process(String command) {
    	String[] commands = command.split(" ");
    	command = commands[0];
    	if (commands.length > 1) {
    		String[] temp = commands;
    		commands = new String[commands.length - 1];
    		for (int i = 0; i < commands.length; i++) {
    			commands[i] = temp[i + 1];
    		}
    	}
    	for (Command c : this.commands) {
    		for (String trigger : c.getTriggers()) {
    			if (trigger.equals(command)) {
    				c.run(commands);
    				textField.setText("");
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public void output(String message) {
    	output(message, Color.BLACK);
    }
    
    public void output(Message message) {
    	this.output(message.toString());
    }
    
    public void output(String message, Color color) {
    	message = "\n" + message;
    	StyleConstants.setForeground(style, color);
        try {
        	doc.insertString(doc.getLength(), "BLAH ",style);
        	this.textPane.setCaretPosition(this.textPane.getText().length());
        } catch (BadLocationException e){
        }
        StyleConstants.setForeground(style, Color.BLACK);
    }
    
    public static String addArgs(String[] args) {
    	String temp = args[0];
		
		for (int i = 1; i < args.length; i++) {
			temp += (" " + args[i]);
		}
		
		return temp;
    }
    
    private void sendMessage(Message input) {
    	for (ClientOutThread c_out : getClientIns()) {
			c_out.addNextMessage(input);
		}
    }
    
    private void send(Message input) {
    	output("[" + input.getTimestamp() + "] " + input.getID() + ": " + input.getText());
		
		if (input.getAttachmentType() != AttachmentType.INSTRUCTION && input.getAttachmentType() != AttachmentType.INSTRUCTION) {
			this.log = this.log + "\n[" + input.getTimestamp() + "] " + input.getUser() + ": " + input.getText();
		}
		
		sendMessage(input);
    }
    
    /*public byte[] getKey() {
    	int keySize = 16;
    	byte[] key = this.keyField.getText().getBytes();
    	
    	byte[] output = Arrays.copyOf(key, keySize);
    	
    	return output;
    }*/
    
}
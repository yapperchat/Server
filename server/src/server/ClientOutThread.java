package server;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;

import core.command.*;
import core.message.Message;

@SuppressWarnings("unused")
public class ClientOutThread implements Runnable {
	
	private ChatServer server;
	private Socket socket;
	private final LinkedList<Message> messagesToSend;
	
    public void addNextMessage(Message message) {
        synchronized (messagesToSend) {
            messagesToSend.push(message);
        }
    }
	
	public ClientOutThread(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		this.messagesToSend = new LinkedList<Message>();
	}

	@Override
	public void run() {
		try {
            ObjectOutputStream serverOut = new ObjectOutputStream(socket.getOutputStream());
            serverOut.flush();
            
            while (!socket.isClosed() && this.server.isRunning()) {
                if(!messagesToSend.isEmpty()){
                    Message nextSend;
                    synchronized (messagesToSend) {
                        nextSend = messagesToSend.pop();
                    }
                    serverOut.writeObject(nextSend);
                    serverOut.flush();
                }
                
            }
            serverOut.close();
        } catch(Exception ex){
            ex.printStackTrace();
        }
	}

}

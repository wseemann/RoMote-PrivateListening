package Websocket1;

import java.lang.*;
import java.net.*; 
import java.io.*;
import java.util.*;

/**
*   This class encapsulates the server side functionality of the late initialization
*   procedure. It starts the server thread which monitors the activity on a specified
*   port. It waits for client connections on this initial port - portToMonitor. All clients
*   initially connect to this port when they want to initiate a new session with the
*   server. As soon as the server gets a client request in this initial port, it assigns
*   the client a newly generated port number (which is available) to connect to, so that
*   the initial port is freed for other client requests. 
*   The server spawns a new thread connecting to the randomly generated port number and
*   the client and the server go through a series of "handshaking" signals which maintain
*   the state of the server and the client. 
*   The server transfers all the data to the client and sends a bye to the client which closes
*   the connection on that particular port and the port is freed up.
*/

public class LateInitializationServer extends java.lang.Object
{
    
    /**
    *   Constructor for the class.
    */
    public LateInitializationServer() {

    }
    /**
    *   Starts the server deamon process, so that the server can start listening to the
    *   client requests on the port specified. It also starts the server thread.
    *   
    *   @param  portToMonitor   The port that the server should monitor for client requests.
    */
    
    public int StartServerDeamon (int portToMonitor )
    {
		InitilizationServerThread InitSrvrThread = new InitilizationServerThread(portToMonitor);	
		System.out.println("Server Started");
		Session.outprintln("Listening to port:  " + portToMonitor);
		InitSrvrThread.start();
        
        return 1;
    }
}


class InitilizationServerThread extends Thread
{
    /**
    *   Server side Socket
    */
	ServerSocket serverSocket;
	
	/**
	*   Port that the server will be monitoring for all client requests
	*/
	int port;
	
	/**
	*   Randomly generated port number which the server redirects the client
	*   after getting the initial client request
	*/
	static int port_rand=0;
		
	/**
	*   InputStream to communicate with the client socket
	*/
	static DataInputStream dataInputStream = null;
	
	/**
	*   Printstream to communicate with the client socket
	*/
	static PrintStream printStream = null;
    
    /**
	*   Constructor for the class. It takes in a port number which is the port number
	*   that the server will be monitoring for client connections. The constructor
	*   creates a new ServerSocket with the port number specified.
	*
	*   @param  portToMonitor  Port where the initial client requests will be coming in.
	*/
	InitilizationServerThread(int portToMonitor)
	{
		super("InitilizationServerThread");
		port = portToMonitor;

		try
		{
			serverSocket = new ServerSocket(port);
			Session.outprintln("Server Socket created");
		} catch (IOException e) {
			Session.outprintln("Could not create server socket on port: " + port  + ", " + e.toString());
			System.exit(1);
		}
	}

    /**
	*   Puts the thread in a runnable state. It waits for the client requests to come
	*   in. The thread blocks until a client request comes in. when a client request
	*   comes in it generates a random port (which is available)and creates a new
	*   ServerSocket on that port. It initializes the InputStream and PrintStream
	*   associated with the socket and sends the client the new port numbet to connect
	*   to, so that the initial port is  freed up for other client requests
	*   
	*/
	public void run()
	{
		if (serverSocket == null)
			return;

		while (true)
		{
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				Session.outprintln("Main Server thread started: Waiting for Client Connections");
				Session.outprintln("New Client Connection Requested");
					    
			     
			    // Assign the client to listen to a randomly generated port
			    int NextSocket = GetNextPort();
    			ServerSocketThread SocketThread = new ServerSocketThread(NextSocket);
    			SocketThread.start();
    		
                dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                printStream = new PrintStream(new BufferedOutputStream(clientSocket.getOutputStream(), 1024), false);

                Session.outprintln("Sending new Port Number to Client: " + NextSocket);
                printStream.println(NextSocket);
                printStream.flush();
				
				
			} catch (IOException e) {
				Session.outprintln("ftpThread.run(): accept failed on port " + port + ", " + e.toString());
				System.exit(1);
			}

		}
		
	}

    /**
    *   Generates a random port number and checks whether the port is available and 
    *   if it is , it sends this port number out.
    *
    *   @return     New randomly generated port number
    */
    private int GetNextPort()
	{
	 
	 /*Random RandomNumberGenerator = new Random();   
	 double rand_num = RandomNumberGenerator.nextDouble();
	 double port = 1024 + rand_num*(32000 -1024);
	 Session.outprint(port);*/
	 port_rand++;
	 return 9000 + port_rand;
	 
	}
	
}

/**
*   This class invokes a new thread for each client connection. It sets up the initial connection
*   between the client and the server on the randomly generated port number, by creating a server side
*   socket on that port and then waiting for client connection on that port. The thread blocks
*   until a client connection is requested
*/


class ServerSocketThread extends Thread
{
    /**
    *   Server Socket on the new port.
    */
    
    ServerSocket serverSocket;
    
    /*
    *   The new port number where the client will be connecting
    */
	int port;
	 
	/*
	*   Constructor for the class. It takes in a port number which is the port number
	*   that the server will be transferring the data to the client on. The constructor
	*   creates a new ServerSocket with the port number specified.
	*
	*   @param  portToMonitor  New port where the client will connect and data transfer will take place.
	*/    
	ServerSocketThread(int portToMonitor)
	{
		super("ServerSocketThread");
		port = portToMonitor;

		try
		{
			serverSocket = new ServerSocket(port);
			Session.outprintln("Server Socket created");
		} catch (IOException e) {
			Session.outprintln("Could not create server socket on port: " + port  + ", " + e.toString());
			System.exit(1);
		}
	}
    
    /**
	*   Puts the thread in a runnable state. It waits for the client requests to come
	*   in. The thread blocks until a client request comes in. when a client request
	*   comes in it invokes the Clienthandler thread to transfer the data between the 
	*   client and the server    
	*/
	
	public void run()
	{
		if (serverSocket == null)
			return;

		while (true)
		{
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				Session.outprintln("Server thread started on a new Socket: Waiting for Client Connections");
				Session.outprintln("New Client Connection Requested");
				ClientHandler clientHandler = new ClientHandler(this, clientSocket);
				clientHandler.start();
			} catch (IOException e) {
				Session.outprintln("ServerSocketThread.run(): accept failed on port " + port + ", " + e.toString());
				System.exit(1);
			}

		}
		
	}   
    
}

/**  
*   This class encapsulates the server client  interaction functionality after the
*   initial connection has taken place. This creates a new socket on the port specified,
*   maintains the state of the server and then transfers the data over to the client. Once the
*   data has been transferred, a BYE signal is sent to client which closes the connection and
*   the port is freed up
*
*/
class ClientHandler extends Thread
{
    /**
	*   InputStream to communicate with the client socket
	*/
	 DataInputStream dataInputStream = null;
	
	/**
	*   Printstream to communicate with the client socket
	*/    
    
	 PrintStream printStream = null;
	
	
	/**
	*   The Protocol used for transfer of data 
	*/
	private static final int ESTABLISH_CONNECTION= 0;
	private static final int INITIALIZE = 1;
	private static final int END_OF_TRANSMISSION = 2;
	private static final int CLOSE_CONNECTION = 3;

	/**
	*   Maintaining state of the server
	*/
	int state = ESTABLISH_CONNECTION;
    
    /**
    *   Number of Elements transferred
    */
    int noCharXfer = 0;
	
	/**
	*   Reference to the Server Socket
	*/
	ServerSocketThread serverSocketThread;
	
	/**
	*    Reference to the client Socket
	*/
	Socket clientSocket;

	/**
	*   Constructor for the class.
	*
	*   @param  srvrSockThread  Reference to the Server Socket thread.
	*   @param  clntSocket      Reference to the client Socket
	*/
	
	ClientHandler(ServerSocketThread srvrSockThread, Socket clntSocket)
	{
		serverSocketThread = srvrSockThread;
		clientSocket = clntSocket;
	}
    
    /**
	*   Puts the thread in a runnable state. It initializes the InputStream and PrintStream
	*   associated with the socket. It manages all the server client interaction, by writing
	*   and reading from the sockets. Any response from the client is parsed and the next state
	*   of the server is determined. It is reponsible for the data transfer between the 
	*   client and the server
	*/
    
	public void run()
	{
		
		String lineFromClient, processedLine;
		try 
		{
            Session.outprintln("Client thread started");
            
			/**
			*   Input and output streams to communicate with the client
			*/
			dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			printStream = new PrintStream(new BufferedOutputStream(clientSocket.getOutputStream(), 1024), false);

			
			processInput(null);
			boolean done=false;
			while (!done)
			{
				
				lineFromClient = dataInputStream.readLine();
				Session.outprintln("Reading a line from client" + lineFromClient +" Socket" + clientSocket);
				if ( lineFromClient == null) 
					;
				
				processedLine = processInput(lineFromClient);
				if ( processedLine.equals("void") )
					continue;
				
				if (processedLine.equals("BYE"))
				{
				    Session.outprintln("LOOP :Got a BYE"+" Socket" + clientSocket);
				    done =true;
				//	break;
				}
			}

		} catch (IOException e) {
			Session.outprintln("ServerSocketThreadServer SOCKET:PROBLEM " + e.toString() + " SOCKET" + clientSocket);
		} finally {
			try 
			{
				if (printStream != null)
					printStream.close();
				if (dataInputStream != null)
					dataInputStream.close();
				if (clientSocket != null)
				{
					clientSocket.close();
					Session.outprintln("Closing Port" + clientSocket);
				}
			} catch (IOException e) {
				//Might get an error while closing the stream
				System.err.println("ServerSocketThreadServer:" + e.toString());
			}
			
		}
	}
	
	/**
	*   Actually implements the state machine of the server. At any point in time, the
	*   server is one of the states given below:    
	*   1.  ESTABLISH_CONNECTION
	*   2.  INITIALIZE
	*   3.  END_OF_TRANSMISSION
	*   4.  CLOSE_CONNECTION
	*   Depending  on the response from the client the server moves from one state to the next.
	*   Depending on what state the server is in it parse various messages and sends out the
	*   initialization data to the client
	*
	*   @return     Response to be sent to the client.
	*/
	
	private String processInput(String in)
	{
		
		/**
		*   Response to be sent to the client
		*/
		String out;
		
		/**
		*   Number of data elements transferred
		*/
		int charSent =0;
		
		/**
		*   Response from the client
		*/
		String inputLine= null;
		
		/**
		*   Determine the state of the Server
		*/
		switch (state)
		{
		case ESTABLISH_CONNECTION:
		/**
		*   State: ESTABLISH_CONNECTION
		*   Establish a connection with client.
		*   Send a "CONNECTION ESTABLISHED: message to the client
		*   Move on to the next state :INITIALIZE
		*/
			printStream.println("CONNECTION ESTABLISHED");
			printStream.flush();
			Session.outprintln("Sending CONNECTION ESTABLISHED");
			printStream.flush();
			state = INITIALIZE;
			break;
			
		case INITIALIZE:
        /**
        *   State: INITIALIZE
        *   Wait for the client to send a INITIALIZE message
        *   This serves as a confirmation that the client received the CONNECTION ESTABLISHED
        *   message
        *   After getting an "INITIALIZE" message from client, transfer all the data
        *   Send a "EOT" - End of Transmission message to the client
        *   Send Number of data Elements transferred to the client
        *   Move on to the next state: END_OF_TRANSMISSION
        */
              	
              	if(in.equalsIgnoreCase("INITIALIZE"))
            	{
            
	
	            while ( Session.InitializationData.hasMoreElements() )
	            {
	                String s = (String) Session.InitializationData.nextElement();
	                printStream.println ( s );
	            }
	                
				printStream.println("EOT");
				printStream.flush();
				printStream.println(charSent);
				printStream.flush();
			    state = END_OF_TRANSMISSION;
			    break;
			    
			}
			else 
			//Don't update the state varible
			break;
					
		case END_OF_TRANSMISSION:
		/**
		*   State: END_OF_TRANSMISSION
		*   Send a "BYE" message to the client
		*   Move on to the next state :CLOSE_CONNECTION:
		*/
			if(in.equalsIgnoreCase("OK"))
			{
			    Session.outprintln("Sending BYE");
			    printStream.println("BYE");
			    printStream.flush();
			}
			else
			{
			     Session.outprintln("Sending BYE");
			     printStream.println("BYE"); 
			     printStream.flush();
			 }
			   state = CLOSE_CONNECTION;
			  break;
		case CLOSE_CONNECTION: 
		/**
		*   State: CLOSE_CONNECTION:
		*   Send a "BYE" message to the client
		*   
		*/
    		Session.outprintln("Sending BYE");
    	  	 return("BYE"); 	
		default:

		}
		return("void");
	}
	
}

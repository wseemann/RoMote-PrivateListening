package Websocket1;

import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
*   This class encapsulates the client side functionality of the late initialization
*   procedure. It starts the client thread which connects to the server on a specified
*   port to initialize a new seesion with the server. The server assigns the client 
*   a newly generated port number (which is available) to connect to, so that
*   the initial port is freed for other client requests. The client closes that connection
*   and spawns a new thread connecting to the port number assigned by the server and
*   the client and the server go through a series of "handshaking" signals which maintain
*   the state of the server and the client. 
*   The server transfers all the data to the client and sends a bye to the client which closes
*   the connection on that particular port and the port is freed up.
*/

public final class LateInitializationClient extends java.lang.Object
{
    /**
    *   Remote host where the server is located.
    */
    InetAddress machine;

    /**
    *   This function starts the TCP client given the server parameters.
    *   @param TCPServerAddress TCP Server's address object
    *   @param ServicePort      TCP Server's port number
    */
    public void StartClient ( String TCPServerAddress, int ServicePort ) {
        try
        {
    	    try
    	    {
        	    machine = InetAddress.getByName ( TCPServerAddress );
        	}
        	catch (UnknownHostException e)
        	{
        	    machine = InetAddressFactory.newInetAddress ( TCPServerAddress );
        	}
        }
        catch (Exception e)
        {
            System.err.println (e);
            System.exit (1);
        }
		new InitilizationClient( machine , ServicePort );

    }
    
}

/**
*   The class starts up a new thread to connect to the client on the specified port number
*/

class InitilizationClient extends java.lang.Object
{
    /**
    *   Initial Port to connect to on the server
    */
    
	private static int portToMonitor;
	
	/**
    *   Remote host where the server is located.
    */
	private static InetAddress machine = null;
	
	/**
	*   Starts up a new thread to get a new port number from the server
	*
	*   @param  machine         Remote host where the server is located.
	*   @param  ServicePort     Initial Port to connect to on the Server.
	*/
		
	InitilizationClient( InetAddress machine, int ServicePort )
	{
		/**
		*   Create a thread to connect to the server 
		*/
		InitilizationGetPortThread InitGetPortThread = new InitilizationGetPortThread(machine, ServicePort);	
		Session.outprintln("Client Started: on port" + portToMonitor);
		InitGetPortThread.start();
	}

}



class InitilizationGetPortThread extends Thread
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
	*   Client side Socket
	*/
	Socket	clientSocket;
	
	/**
	*   Port to connect to on the server
	*/
	int port;
	
	/**
	*   Remote host where the server is located.
	*/
	InetAddress host;
	
	/**
	*   Creates a new client socket to connect to the server on the specified
	*   port and initializes the input stream and print stream associated
	*   with the socket to communicate with the client
	*
	*   @param  machine         Remote host where the server is located.
	*   @param  ServicePort     Initial Port to connect to on the Server.
	*/
    InitilizationGetPortThread(InetAddress machine, int portToMonitor)
	{
		super("InitilizationClientThread");
		host = machine;
		port = portToMonitor;
		InetAddress localhost;

		try
		{
			/**
			*   Create a new Client Socket;
			*/
		    clientSocket = new Socket(machine,port);
			Session.outprintln("Client Socket Created Succesfully at port" + portToMonitor);
			dataInputStream = new DataInputStream(clientSocket.getInputStream());
			printStream = new PrintStream(clientSocket.getOutputStream());
			
			
		} catch (IOException e) {
			System.err.println("InitilizationGetPortThread:" + e.toString() + ":Unable to connect to server");
			System.err.println ( e );
			System.exit(1);
		} catch (Exception e) {
			System.err.println("InitilizationGetPortThread:" + e.toString() + ":Unable to connect to server");
			System.err.println ( e );
		}
	}
/**
*   Puts the thread in a runnable state. It waits for a response from the server to redirect
*   it to a new port number. On receiving the new port number, it starts a new thread initializing
*   it with the remote host machine and the new port number received from the client. 
*   The connection to the original server on the original port is closed
*
*/
	public void run()
	{
		String inputLine = null;
		
		if (clientSocket == null)
			return;
		
		try
		{
			String lineFromServer;
			Session.outprintln("Client:: Initial Thread Running");

			lineFromServer = dataInputStream.readLine();
			   
	        InitilizationClientThread InitClient = new InitilizationClientThread(host,Integer.valueOf(lineFromServer).intValue());	
		    Session.outprintln("Client Started: on new port " + Integer.valueOf(lineFromServer).intValue());
		    InitClient.start();
		     
            try 
            {
                if (printStream != null)
                	printStream.close();
                if (dataInputStream != null)
                	dataInputStream.close();
                if (clientSocket != null);
                	//clientSocket.close();
            } 
			catch (IOException e) {
				//Might get an error while closing the stream
				System.err.println("InitilizationServer: " + e.toString());
    			System.err.println ( e );
				
			}

		} catch (IOException e) {
			System.err.println("InitilizationClient: " + e.toString());
			System.err.println ( e );
			
		} 
		
	}
    
}

/**  
*   This class encapsulates the server client  interaction functionality after the
*   initial connection has taken place. This creates a new socket on the port specified by the server,
*   maintains the state of the client and then manages transfer of data from the server. Once the
*   data has been transferred, a BYE signal is sent to server which closes the connection and
*   the port is freed up
*
*/

class InitilizationClientThread extends Thread
{
    /**
    *   Client Socket on the new port.
    */
	Socket	clientSocket;
	
	/*
    *   The new port number where the client will be connecting
    */
	int port;
	
	/*
    *   Remote host where the server is located.
    */
	
	InetAddress host;
	
	 /**
	*   InputStream to communicate with the client socket
	*/
	DataInputStream dataInputStream = null;
	
	/**
	*   Printstream to communicate with the client socket
	*/ 
	PrintStream printStream = null;
	
	
	Vector Data;
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
	
    int noCharXfer = 0;

    /**
	*   Constructor for the class. Creates a new client socket and initializes
	*   the the input stream and print stream associated with the socket to 
	*   communicate with the client
	*
	*   @param  machine            Remote host where the server is located.
	*   @param  portToMonitor      Initial Port to connect to on the Server.
	*/
	

	InitilizationClientThread(InetAddress machine, int portToMonitor)
	{
		super("InitilizationClientThread");
		//fileName = fName;
		host = machine;
		port = portToMonitor;

		try
		{

			//Create a new Client Socket;
			clientSocket = new Socket(host,port);
			Session.outprintln("Client Socket Created Succesfully");
			dataInputStream = new DataInputStream(clientSocket.getInputStream());
			printStream = new PrintStream(clientSocket.getOutputStream());
			Data = new Vector();
			
		} catch (IOException e) {
			System.err.println("InitilizationClientThread:" + e.toString() + ":Unable to connect to server");
			System.err.println ( e );
			System.exit(1);
		} catch (Exception e) {
			System.err.println("InitilizationClientThread:" + e.toString() + ":Unable to connect to server");
			System.err.println ( e );
		}
	}

    /**
	*   Puts the thread in a runnable state. It manages all the server client interaction, by writing
	*   and reading from the sockets. Any response from the server is parsed and the next state
	*   of the client is determined. It is reponsible for the data transfer between the 
	*   client and the server
	*/
	public void run()
	{
		String inputLine = null;
		
		if (clientSocket == null)
			return;
		
		try
		{
			String lineFromServer, processedLine;
			Session.outprintln("Client:: Thread Running");
            boolean done = false;
           	while (!done)
			{
				lineFromServer = dataInputStream.readLine();
//				Session.outprintln("LINE FROM SERVER"  + lineFromServer);
				
				if ( lineFromServer != null) 
					{
					    processedLine = processInput(lineFromServer);
					
				        if ( processedLine.equals("void") )
					    continue;
				    
				        if (processedLine.equals("BYE"))
				        {   
				            done = true;
				          //  Session.outprintln(Data);
					        break;
				        }
				    }
				   else{
				           done = true;
				          // Session.outprintln(Data);
					        break;
				   }
			}

		} catch (IOException e) {
			System.err.println("InitilizationClient: " + e.toString());
			System.err.println ( e );
		} finally {
			try 
			{
				if (printStream != null)
					printStream.close();
				if (dataInputStream != null)
					dataInputStream.close();
				if (clientSocket != null)
					clientSocket.close();
			} catch (IOException e) {
				//Might get an error while closing the stream
				System.err.println("InitilizationServer: " + e.toString());
    			System.err.println ( e );
				
			}
			
		}
	}
	
	/**
	*   Actually implements the state machine of the client. At any point in time, the
	*   client is one of the states given below:    
	*   1.  ESTABLISH_CONNECTION
	*   2.  INITIALIZE
	*   3.  END_OF_TRANSMISSION
	*   4.  CLOSE_CONNECTION
	*   Depending  on the response from the server the client moves from one state to the next.
	*   Depending on what state the client is in it parse various messages and sends out the
	*   initialization data to the server.
	*
	*   @return     Response to be sent to the Server.
	*/
	
	public String processInput(String in)
	{
		/**
		*   Response to be sent to the server
		*/
		String out;
		
		switch (state)
		{
  		case ESTABLISH_CONNECTION:
  		/**
		*   State: ESTABLISH_CONNECTION
		*   Establish a connection with server.
		*   Wait for a "CONNECTION ESTABLISHED: message from the server
		*   Send a "INITIALIZE" message to the server.
		*   Move on to the next state :INITIALIZE
		*/
			if(in.equalsIgnoreCase("CONNECTION ESTABLISHED"))
			{
                Session.outprintln("Entering ESTABLISH CONNECTION");
    			printStream.println("INITIALIZE");
    		    printStream.flush();
    			state = INITIALIZE;
    			Session.outprintln("Rcvd: ESTABLISH CONNECTION from Server - Sending INITIALIZE");
			
			}
			break;
		case INITIALIZE:
		/**
        *   State: INITIALIZE
        *   Wait for the client to send data 
        *   Unless the data is "EOT", store the data
        *   If the message is "EOT" - End of Transmission message to the client,
        *   it means all the data has been transferred.
        *   Move on to the next state: END_OF_TRANSMISSION
        */

			if(in.equalsIgnoreCase("EOT"))
			{
			    state = END_OF_TRANSMISSION;
			    Session.outprintln("Rcvd: EOT from Server");
			}
			else
			{
			    
				noCharXfer += in.length();
				//Session.outprintln("Receiving Data");
				//Session.outprintln(in);
				Data.addElement(in);
			}
			break;
			
		case END_OF_TRANSMISSION:
		    /**
		    *   State: END_OF_TRANSMISSION
		    *   Wait for the server to send the number of characters
		    *   it transferred over. 
		    *   Compare that with the number of characters received
		    *   If they are the same , send "OK", else send "NOK"
		    *   Move on to the next state :CLOSE_CONNECTION:
		    */
		    
		    
			int noCharRecvd = Integer.valueOf(in).intValue();
			Session.outprintln("noCharRecvd" + noCharRecvd + "noCharXfer" + noCharXfer);
			if ( noCharRecvd == noCharXfer )
			{
				printStream.println("OK");
				Session.outprintln("Rcvd: Correct No. of Characters");
				printStream.flush();
				state = CLOSE_CONNECTION;
			}
			else
			{
				printStream.println("NOK");
				printStream.flush();
				Session.outprintln("Rcvd: Wrong No. of Characters");
				state = CLOSE_CONNECTION;
			}
			
			break;
			
		case CLOSE_CONNECTION: 
    		/**
    		*   State: CLOSE_CONNECTION:
    		*   Send a "BYE" message to the server
    		*   
    		*/
		    Session.outprintln("Rcvd BYE");
    		return("BYE");
    		
    		
		default:
			// should never come here because we are maintaining the states
			
			
		}
		return("void");
	}
}		


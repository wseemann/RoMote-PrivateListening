package Websocket1;

import java.net.*;
import java.lang.*;
import java.util.*;
import java.net.*;



/*
*   This class encapsulates the functionality to construct and send out  RTP Packets and also
*   to receive RTP Packets. It provides a seperate thread to receive and send out RTP 
*   Packets.
*
*   
*/


public class RTPThreadHandler extends Thread
{
    
    /**
    *   Multicast Port for RTP Packets
    */
    int 	m_mcastPort;
    
    /**
    *   Sender Address for RTP Packets
    */
    InetAddress m_InetAddress;

     /**
    *   Sender Port for RTP Packets
    */
    int m_sendPort;
    
    /**
    *   Multicast Socket for sending RTP
    */
    DatagramSocket m_sockSend;
    
    /**
    *   Initialize Random Number Generator
    */
    static Random RandomNumGenerator = new Random();
    
    /**
    *   Random Offset -32 bit
    */
    public static final short RandomOffset = (short) Math.abs ( RandomNumGenerator.nextInt() & 0x000000FF) ;
    
    /**
    *   RTP Header Length = 12
    */
    public static final int RTP_PACKET_HEADER_LENGTH = 12;

    /**********************************************************************************************
                          RTP Header related fields
    
    **********************************************************************************************/
    
    /**
    *   payload type decimal 88 (hex 0x58)
    */
    static byte     PayloadType = 0;

    /**
    *   First Byte of header
    */
                                                  // +-+-+-+-+-+-+-+-+
    static byte     VPXCC []= {(byte) 0x80};      // |V=2|P|X|   CC  |
                                                  // +-+-+-+-+-+-+-+-+
                                                  //  1 0 0 0 0 0 0 0 = 0x80


    /**
    *   Second Byte of header
    */
    static byte     M_PT [] = new byte [1];
                                                // +-+-+-+-+-+-+-+-+
                                                // |M|     PT      |
                                                // +-+-+-+-+-+-+-+-+
                                                //  0 1 0 1 1 0 0 0

    /**
    *   Sequence Number
    */
    static long    sequence_number;            // 16 bits
    
    /**
    *   TimeStamp
    */
    static long    timestamp;                  // 32 bits


   /**
   *    Constructor for the class. Takes in a TCP/IP Address and a port number. It initializes a
   *    a new multicast socket according to the multicast address and the port number given    
   *
   *   @param   MulticastAddress    Dotted representation of the Multicast address.
   *   @param   SendFromLocalPort   Port used to send RTP Packets.
   *   @param   MulticastPort       Port for Multicast group (for receiving RTP Packets).
   *
   *
   */
  

    RTPThreadHandler( InetAddress MulticastAddress, int SendFromLocalPort, int MulticastPort )
    {
        m_InetAddress = MulticastAddress;
        m_mcastPort = MulticastPort;
        m_sendPort = SendFromLocalPort;

        Random rnd = new Random();  // Use time as default seed

        // Start with a random sequence number
        sequence_number = (long) ( Math.abs(rnd.nextInt()) & 0x000000FF);
        timestamp = Session.CurrentTime() + RandomOffset;

        
        Session.outprintln ("RTP Session SSRC: " + Long.toHexString(Session.SSRC) );
        Session.outprintln (" Starting Seq: " + sequence_number );
        /////////////
        //Initialize a Multicast Sender Port to send RTP Packets
        Session.outprintln ( "Openning local port " + m_sendPort + " for sending RTP..");
        try
        {
            m_sockSend = new DatagramSocket ( m_sendPort );
        }
    	catch ( SocketException e)
    	{
    	    System.err.println ( e );
    	}

    	catch ( java.io.IOException e )
    	{
    		System.err.println (e);
    	}
        Session.outprintln ( "Successfully openned local port " + m_sendPort );
        
    }


   /**
   *  Constructs a datagram, assembles it into an RTP packet and sends it out.
   *
   *
   *   @param data  Payload to be sent out as part of the RTP Packet.
   */

    public int SendPacket ( byte[] data )
    {

        M_PT[0]    = (byte) ((0x0 << 8) | ( Session.getPayloadType() ));
                                    // +-+-+-+-+-+-+-+-+
                                    // |M|     PT      |
                                    // +-+-+-+-+-+-+-+-+
                                    //  0 1 0 1 1 0 0 0

        timestamp = Session.CurrentTime() + RandomOffset;

        byte ts[] = new byte[4];         // timestamp is 4 bytes
        ts = PacketUtils.LongToBytes ( timestamp, 4 );

        byte seq[] = new byte[2];       // sequence is 2 bytes
        seq = PacketUtils.LongToBytes ( sequence_number, 2 );

        byte ss[] = new byte[4];
        ss = PacketUtils.LongToBytes ( Session.SSRC, 4 );

        ////////////////////////////////////////////////////////
        // Construct the header by appending all the above byte
        // arrays into RTPPacket
        ////////////////////////////////////////////////////////
        byte RTPPacket [] = new byte [0];
        // Append the compound version, Padding, Extension and CSRC Count bits
        RTPPacket  = PacketUtils.Append ( RTPPacket, VPXCC );

        // Append the compound Marker and payload type byte
        RTPPacket  = PacketUtils.Append ( RTPPacket,  M_PT );

        // Append the sequence number
        RTPPacket  = PacketUtils.Append ( RTPPacket, seq );

        // Append the 4 timestamp bytes
        RTPPacket  = PacketUtils.Append ( RTPPacket, ts );


        // Append the 4 SSRC bytes
        RTPPacket  = PacketUtils.Append ( RTPPacket, ss );

        // Append the data packet after 12 byte header
        RTPPacket  = PacketUtils.Append ( RTPPacket, data );

        sequence_number++;

        // Create a datagram packet from the RTP byte packet and set ttl and send
        DatagramPacket pkt = new DatagramPacket ( RTPPacket, RTPPacket.length,
                                                    m_InetAddress, m_mcastPort );
        try
        {
        	System.out.println("fuck!------->");
            m_sockSend.send( pkt ); //, (byte) 5 ); // TODO: Change Hardcoded TTL - WA

            //Update own status to Active Sender
            Source S1 = Session.GetMySource();
            S1.ActiveSender = true;
            Session.TimeOfLastRTPSent = Session.tc = Session.CurrentTime() ;
            Session.PacketCount++;
            Session.OctetCount+=data.length;

        }


    	catch ( java.io.IOException e )
    	{
    		System.err.println (e);
    		System.exit (1);
    	}

        return 1;

    }


   /**
   *   Starts the RTPReceiver Thread 
   */

    public void run()
    {
        StartRTPReceiver();
    }


   /**
   *   Sends test packet (For debugging only).
   */
    public void StartRTPReceiver()
    {
        Session.outprintln ("RTP Thread started ");
        Session.outprintln ("RTP Group: " + m_InetAddress + "/" + m_mcastPort);

	    byte buf[] = new byte[1024];
	    DatagramPacket packet = new DatagramPacket( buf, buf.length );
	    
        PayloadType = Session.getPayloadType();

       	try
    	{

       		DatagramSocket s = new DatagramSocket ( m_mcastPort );
            //s.joinGroup ( m_InetAddress );

    		while (1==1)
    		{
    			s.receive( packet );
    			if ( ValidateRTPPacketHeader ( packet.getData() ) )
                {
        			long SSRC = 0;
        			int TimeStamp = 0;
        			short SeqNo = 0;
        			byte PT = 0;

                   	PT = (byte) ((buf[1] & 0xff) & 0x7f);
                	SeqNo =(short)((buf[2] << 8) | ( buf[3] & 0xff)) ;
                	TimeStamp =(((buf[4] & 0xff) << 24) | ((buf[5] & 0xff) << 16) | ((buf[6] & 0xff) << 8) | (buf[7] & 0xff)) ;
        			SSRC = (((buf[8] & 0xff) << 24) | ((buf[9] & 0xff) << 16) | ((buf[10] & 0xff) << 8) | (buf[11] & 0xff));

                    Session.outprintln("RTP (");
                	Session.outprintln ( "ssrc=0x" + Long.toHexString(SSRC) + "\tts=" + TimeStamp + "\tseq=" + SeqNo + "\tpt=" + PT );
                	Session.outprintln(")");

                	// Create a RTPPacket and post it with Session.
                	// If there are any interested actionListeners, they will get it.
                	RTPPacket rtppkt = new RTPPacket();
                	rtppkt.CSRCCount = 0;
                	rtppkt.SequenceNumber = SeqNo;
                	rtppkt.TimeStamp = TimeStamp;
                	rtppkt.SSRC = SSRC;

                	// the payload is after the fixed 12 byte header
                	byte payload [] = new byte [ packet.getLength() - RTP_PACKET_HEADER_LENGTH ];

                	for ( int i=0; i < payload.length; i++ )
                	    payload [i] = buf [ i+RTP_PACKET_HEADER_LENGTH ];

                	rtppkt.data = payload;              
                	Runnable runnable = new Runnable() {
						@Override
						public void run() {
							sendTestPacket(rtppkt.data);							
						}
                	};
                	Thread thread = new Thread(runnable);
                	thread.start();
                	
                	if (Session.EnableLoopBack)
                	{
                	    Session.postAction ( rtppkt );
                	}
                	else
                	{
                	    if (SSRC != Session.SSRC)
                	    {
                	        Session.postAction ( rtppkt );
                	    }
                	}

                	// Get the source corresponding to this SSRC
                	Source RTPSource =  Session.GetSource(SSRC);

                	//Set teh Active Sender Property to true
                	RTPSource.ActiveSender =true;

                	//Set the time of last RTP Arrival
                	RTPSource.TimeOfLastRTPArrival = Session.tc = Session.CurrentTime() ;

                	//Update the sequence number
                	RTPSource.updateSeq(SeqNo);

                	// if this is the first RTP Packet Received from this source then
                	// store the seq no. as its base
                	if (RTPSource.NoOfRTPPacketsRcvd == 0)
                	    RTPSource.base_seq = SeqNo;


                	// Increment the total number of RTP Packets Received
                	RTPSource.NoOfRTPPacketsRcvd++;
		        }
                else
                {
                    System.err.println (    "RTP Receiver: Bad RTP Packet received");
                    System.err.println (    "From : " + packet.getAddress() + "/" + packet.getPort() + "\n" +
                                            "Length : " + packet.getLength()
                                       );
                }
            }

    		//s.leaveGroup( m_InetAddress );

    		//s.close();
    	}
    	catch ( SocketException se)
    	{
    	    System.err.println ( se );
    	}

    	catch ( java.io.IOException e )
    	{
    		Session.outprintln (" IO exception" );
    	}
    }


  /**
  *    Validates RTP Packet.
  *    Returns true or false corresponding to the test results.
  *
  *   @param - packet[] The RTP Packet to be validated.
  *   @return  True if validation was successful, False otherwise.
  */


   public boolean ValidateRTPPacketHeader ( byte packet[] )
   {
        boolean VersionValid = false;
        boolean PayloadTypeValid = false;

        // +-+-+-+-+-+-+-+-+
        // |V=2|P|X|   CC  |
        // +-+-+-+-+-+-+-+-+

        // Version MUST be 2
        if ( ( (packet[0] & 0xC0) >> 6 ) == 2 )
            VersionValid = true;
        else
            VersionValid = false;

        // +-+-+-+-+-+-+-+-+
        // |M|     PT      |
        // +-+-+-+-+-+-+-+-+
        //  0 1 0 1 1 0 0 0

        // Payload Type must be the same as the session's
        if ( ( packet [1] & 0x7F ) == Session.getPayloadType() )
            PayloadTypeValid = true;
        else
            PayloadTypeValid = false;

        return ( VersionValid && PayloadTypeValid );

   }


  /**
  *   Sends test packet (For debugging only).
  *   
  */

   public int sendTestPacket(byte [] data)
   {
        byte AppPkt[] = ( new String( ":testpacket:")).getBytes();

        Session.outprintln ( "Sending RTP Packet from : " + m_sendPort );
        
        this.SendPacket( data );


        /*try
        {
            sleep ( 1000 );
        }
        catch ( InterruptedException e)
        {
            ;
        }*/
        return (1);
   }

}


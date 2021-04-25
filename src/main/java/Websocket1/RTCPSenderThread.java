package Websocket1;

import java.lang.*;
import java.net.*;
import java.util.*;


/*
*   This class encapsulates the functionality to construct and send out an RTCP Packet. This
*   class provides a seperate thread to send out RTCP Packets. The thread is put to sleep
*   for a specified amount of time ( as calculated using various RTCP parameters and reception
*   feedback). When the thread moves out of the blocked (or sleep) state, it determines what
*   kind of a RTCP Packets needs to be send out , constructs the appropriate RTCP packets and
*   sends them
*/


public class RTCPSenderThread extends Thread
{
    /**
    *   Sender Port for RTCP Packets
    */   	  	
   	private int  m_SendFromPort;
   	
   	/**
    *   Sender Address for RTCP Packets
    */
	private InetAddress m_InetAddress;
	
	/**
    *   Multicast Socket for sending RTCP
    */
	MulticastSocket m_RTCPSenderSocket;
	
	/**
    *   Multicast Port for RTCP Packets
    */
	private int m_MulticastRTCPPort;
	
	/**
    *   Packet for RTCP Packets
    */
	private static int packetcount;
	
	/**
    *   Flag used to determine when to terminate after sending a BYE
    */
	private boolean WaitingForByeBackoff = false;
    
    /**
    *   Initialize Random Number Generator
    */
	Random rnd = new Random();


   /**
   *    Constructor for the class. Takes in a TCP/IP Address and port numbers for sending
   *    and receiving RTCP Packets.
   *    
   *   @param   MulticastGroupIPAddress Dotted representation of the Multicast address.
   *   @param   RTCPSendFromPort        Port used to send RTCP Packets
   *   @param   RTCPGroupPort           Port for Multicast group (for receiving RTP Packets).
   *
   */

   
    public RTCPSenderThread ( InetAddress MulticastGroupIPAddress, int RTCPSendFromPort, int RTCPGroupPort )
    {
        // TODO: Perform sanity check on group address and port number - WA
        m_InetAddress = MulticastGroupIPAddress;
        m_MulticastRTCPPort = RTCPGroupPort;
        m_SendFromPort = RTCPSendFromPort;

    }

   /**
   *   Starts the RTCPSender Thread
   */
 
    public void run()
    {
        StartRTCPSender();
    }
   
   
   /**
   *    
   *    Initializes the thread by creating a multicast socket on a specified address and a port
   *    It manages the thread initialization and blocking of the thread (i.e putting it to sleep
   *    for a specified amount of time) This function also implements the BYE backoff algorithm
   *    with Option B. The BYE Backoff Algorithm is used in order to avoid a flood of BYE packets
   *    when many users leave the system
   *
   *    Note : if a client has never sent an RTP or RTCP Packet, it will not send a BYE Packet
   *    when it leaves the group.
   *    For More Information : See the Flowchart
   *
   */
   
   
    public void StartRTCPSender()
    {

        Session.outprintln ("RTCP Sender Thread started ");

        Session.outprintln ("RTCP Group: " + m_InetAddress.toString() + ":" + m_MulticastRTCPPort);
        Session.outprintln ("RTCP Local port for sending: " + m_SendFromPort );

        // Create a new socket and join group
		try
		{
			m_RTCPSenderSocket = new MulticastSocket ( m_SendFromPort );
    	}
		catch ( UnknownHostException e )
		{
			Session.outprintln("Unknown Host Exception");
		}
        catch ( java.io.IOException e )
        {
            Session.outprintln ("IOException");
        }


        // flag terminates the endless while loop
        boolean terminate = false;

        while ( !terminate )
        {
            // Update T and Td (Session level variables)
            Session.CalculateInterval();

            // If inturrepted during this sleep time, continue with execution
            int sleepResult = SleepTillInterrupted( Session.T );

            if ( sleepResult == 0 )
            {
                // Sleep was interrupted, this only occurs if thread
                // was terminated to indicate a request to send a BYE packet
                WaitingForByeBackoff = true;
                Session.IsByeRequested = true;
            }

            // See if it is the right time to send a RTCP packet or reschedule          {{A True}}
            if ( (Session.TimeOfLastRTCPSent + Session.T) <= Session.CurrentTime() )
            {
                // We know that it is time to send a RTCP packet, is it a BYE packet    {{B True}}
                if ( ( Session.IsByeRequested && WaitingForByeBackoff ) )
                {
                    // If it is bye then did we ever sent anything                      {{C True}}
                    if ( Session.TimeOfLastRTCPSent > 0 && Session.TimeOfLastRTPSent > 0 )
                    {
                        // ** BYE Backoff Algorithm **
                        // Yes, we did send something, so we need to send this RTCP BYE
                        // but first remove all sources from the table
                        Session.RemoveAllSources();

                        // We are not active senders anymore
                        Session.GetMySource().ActiveSender = false;
                        Session.TimeOfLastRTCPSent = Session.CurrentTime();
                    }
                    else // We never sent anything and we have to quit :( do not send BYE {{C False}}
                    {
                        terminate = true;
                    }
                }
                else                                                                    // {{B False}}
                {
                    byte CompoundRTCPPacket [] = AssembleRTCPPacket();
                    SendPacket ( CompoundRTCPPacket );

                    // If the packet just sent was a BYE packet, then its time to terminate.
                    if ( Session.IsByeRequested && ! WaitingForByeBackoff )             // {{D True}}
                    {
                        // We have sent a BYE packet, so its time to terminate
                        terminate = true;
                    }
                    else                                                                // {{D False}}
                    {
                        Session.TimeOfLastRTCPSent = Session.CurrentTime();
                    }

                }
            }
            else // This is not the right time to send a RTCP packet, just reschedule   // {{A False}}
            {;}

            WaitingForByeBackoff = false;
            Session.tn = Session.CurrentTime() + Session.T;
            Session.pmembers = Session.GetNumberOfMembers();

        }

    }



   /**
   *    Provides a wrapper around java sleep to handle exceptions
   *    in case when session wants to quit.
   *    Returns 0 is sleep was interrupted and 1 if all
   *    the sleep time was consumed.
   *
   *   @param     Seconds   No. of seconds to sleep
   *   @return    0 if interrupted, 1 if the sleep progressed normally  
   */

   
    int SleepTillInterrupted ( double Seconds )
    {
        try
        {
            sleep ( (long) Seconds * 1000 );
            Session.outprintln ( "In sleep function after sleep." );
        }
        catch ( InterruptedException e )
        {
            Session.outprintln ( "Interrupted" );
            return (0);
        }

        Session.outprintln ("Just woke up after try");
        return (1);
    }


   /**
   *    Top Level Function to assemble a compound RTCP Packet. This function determines what kind of RTCP 
   *    Packet needs to be created and sent out. If this source  is a sender (ie. generating
   *    RTP Packets), then a Sender Report (SR) is sent out otherwise a Receiver Report (RR) is sent
   *    out. An SDES Packet is appended to the SR or RR PAcket. If a BYE was requested by the
   *    application , a BYE PAcket is sent out.
   *
   *
   *   @return  The Compound RTCP Packet 
   */

   
   public byte[] AssembleRTCPPacket ()
   {
        byte packet[] = new byte [0];

        // Determine if the packet is SR or RR
        Source sMe = Session.GetSource ( Session.SSRC );

        //
        // Generate an SR packet if I am an active sender and did send an
        // RTP packet since last time I sent an RTCP packet.
        //
        if ( ( sMe.ActiveSender ) && ( Session.TimeOfLastRTCPSent < Session.TimeOfLastRTPSent ) )
            packet = PacketUtils.Append ( packet, AssembleRTCPSenderReportPacket() );
        else
            packet = PacketUtils.Append ( packet, AssembleRTCPReceiverReportPacket() );


        // Append an SDES packet
        packet = PacketUtils.Append ( packet, AssembleRTCPSourceDescriptionPacket() );

        // Append a BYE packet if necessary
        if ( Session.IsByeRequested )
            packet = PacketUtils.Append ( packet, AssembleRTCPByePacket( "Quitting") );

        return packet;
   }

  /*****************************************************************************************
   *
   *    Functions to assemble RTCP packet components.
   *
  *******************************************************************************************/

   /**
   *   Creates a Sender Report RTCP Packet. 
   *
   *
   *   @return  The Sender Report Packet. 
   */


   private byte[] AssembleRTCPSenderReportPacket ()
   {
    /*
            0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |V=2|P|    RC   |   PT=SR=200   |             length            | header
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         SSRC of sender                        |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |              NTP timestamp, most significant word             | sender
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ info
       |             NTP timestamp, least significant word             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         RTP timestamp                         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     sender's packet count                     |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                      sender's octet count                     |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                 SSRC_1 (SSRC of first source)                 | report
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
       | fraction lost |       cumulative number of packets lost       |   1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |           extended highest sequence number received           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                      interarrival jitter                      |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         last SR (LSR)                         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                   delay since last SR (DLSR)                  |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                 SSRC_2 (SSRC of second source)                | report
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
       :                               ...                             :   2
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                  profile-specific extensions                  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

       */


        final int FIXED_HEADER_SIZE = 4; // 4 bytes

        // construct the first byte containing V, P and RC
        byte V_P_RC;
        V_P_RC =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x00 )  // take only the right most 5 bytes i.e. 00011111 = 0x3F
                            );

        // SSRC of sender
        byte ss[] = PacketUtils.LongToBytes ( Session.SSRC, 4);

        // Payload Type = SR
        byte PT[] = PacketUtils.LongToBytes ( (long)RTCPConstants.RTCP_SR, 1);

        // Get NTP Time and put in 8 bytes
         byte NTP_TimeStamp[] = PacketUtils.LongToBytes (Session.CurrentTime(), 8 );
        //byte NTP_TimeStamp[] = new byte [8];
        byte RTP_TimeStamp[] = PacketUtils.LongToBytes ( (long) Session.tc + RTPThreadHandler.RandomOffset , 4 );

        byte SenderPacketCount[] = PacketUtils.LongToBytes ( Session.PacketCount, 4);
        byte SenderOctetCount[] = PacketUtils.LongToBytes ( Session.OctetCount, 4);

        // Create an initial report block, this will dynamically grow
        // as each report is appended to it.
      //  byte ReportBlock [] = new byte [0];

        // Append all the sender report blocks
        byte ReceptionReportBlocks [] = new byte [0];

        ReceptionReportBlocks = PacketUtils.Append ( ReceptionReportBlocks, AssembleRTCPReceptionReport() );

        // Each reception report is 24 bytes, so calculate the number of sources in the
        // reception report block and update the reception block count in the header
        byte ReceptionReports = (byte) (ReceptionReportBlocks.length / 24 );

        // Reset the RC to reflect the number of reception report blocks

        V_P_RC = (byte) ( V_P_RC | (byte) ( ReceptionReports & 0x1F ) ) ;
        Session.outprintln("RC: " + ReceptionReports);

        // Get an SDES Packet
        byte SDESPacket [] = AssembleRTCPSourceDescriptionPacket();

        // Length is 32 bit words contained in the packet -1
        byte length[] = PacketUtils.LongToBytes ( (FIXED_HEADER_SIZE + ss.length +
                                                NTP_TimeStamp.length +
                                                RTP_TimeStamp.length +
                                                SenderPacketCount.length +
                                                SenderOctetCount.length +
                                                ReceptionReportBlocks.length
                                                ) /4 -1 ,
                                                2);
        // SDESPacket.length
        // Append all the above components and construct a Sender Report packet
        byte SRPacket [] = new byte [1];
        SRPacket[0] = V_P_RC;
        SRPacket = PacketUtils.Append (SRPacket, PT );
        SRPacket = PacketUtils.Append (SRPacket, length );
        SRPacket = PacketUtils.Append (SRPacket, ss );
        SRPacket = PacketUtils.Append (SRPacket, NTP_TimeStamp );
        SRPacket = PacketUtils.Append (SRPacket, RTP_TimeStamp );
        SRPacket = PacketUtils.Append (SRPacket, SenderPacketCount );
        SRPacket = PacketUtils.Append (SRPacket, SenderOctetCount );
        SRPacket = PacketUtils.Append (SRPacket, ReceptionReportBlocks );


        return SRPacket;
   }

 
   /**
   *   Creates a Receiver Report RTCP Packet. 
   *
   *
   *   @return  byte[] The Receiver Report Packet 
   */
 

   private byte[] AssembleRTCPReceiverReportPacket ()
   {
    /*
          0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |V=2|P|    RC   |   PT=RR=201   |             length            | header
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         SSRC of sender                        |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                 SSRC_1 (SSRC of first source)                 | report
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
       | fraction lost |       cumulative number of packets lost       |   1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |           extended highest sequence number received           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                      interarrival jitter                      |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         last SR (LSR)                         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                   delay since last SR (DLSR)                  |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                 SSRC_2 (SSRC of second source)                | report
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
       :                               ...                             :   2
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                  profile-specific extensions                  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


      */
         final int FIXED_HEADER_SIZE = 4; // 4 bytes

        // construct the first byte containing V, P and RC
        byte V_P_RC;
        V_P_RC =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x00 )  // take only the right most 5 bytes i.e. 00011111 = 0x1F
                            );

        // SSRC of sender
        byte ss[] = PacketUtils.LongToBytes ( Session.SSRC, 4);

        // Payload Type = RR
        byte PT[] = PacketUtils.LongToBytes ( (long)RTCPConstants.RTCP_RR, 1);

        byte ReceptionReportBlocks [] = new byte [0];

        ReceptionReportBlocks = PacketUtils.Append ( ReceptionReportBlocks, AssembleRTCPReceptionReport() );

        // Each reception report is 24 bytes, so calculate the number of sources in the
        // reception report block and update the reception block count in the header
        byte ReceptionReports = (byte) (ReceptionReportBlocks.length / 24 );

        // Reset the RC to reflect the number of reception report blocks
        V_P_RC = (byte) ( V_P_RC | (byte) ( ReceptionReports & 0x1F ) ) ;

        byte length[] = PacketUtils.LongToBytes ( (FIXED_HEADER_SIZE + ss.length +
                                                   ReceptionReportBlocks.length
                                                ) /4 -1 ,
                                                2);

        byte RRPacket [] = new byte [1];
        RRPacket[0] = V_P_RC;
        RRPacket = PacketUtils.Append (RRPacket, PT );
        RRPacket = PacketUtils.Append (RRPacket, length );
        RRPacket = PacketUtils.Append (RRPacket, ss );
        RRPacket = PacketUtils.Append (RRPacket, ReceptionReportBlocks );

        Session.outprintln("RRPacket" + RRPacket[1]);
        return RRPacket;
   }

   
   /**
   *   Creates an Source Description SDES RTCP Packet. 
   *
   *
   *   @return  The SDES Packet. 
   */
   
   private byte[] AssembleRTCPSourceDescriptionPacket ()
   {
        /* following figure from draft-ietf-avt-rtp-new-00.txt
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |V=2|P|    SC   |  PT=SDES=202  |             length            | header
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                          SSRC/CSRC_1                          | chunk
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   1
       |                           SDES items                          |
       |                              ...                              |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                          SSRC/CSRC_2                          | chunk
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   2
       |                           SDES items                          |
       |                              ...                              |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+        */

        final int FIXED_HEADER_SIZE = 4; // 4 bytes
        // construct the first byte containing V, P and SC
        byte V_P_SC;
        V_P_SC =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x01 )
                            );

        byte PT[] = PacketUtils.LongToBytes ( (long)RTCPConstants.RTCP_SDES, 1);

        /////////////////////// Chunk 1 ///////////////////////////////
        byte ss[] = PacketUtils.LongToBytes ( (long) Session.SSRC, 4 );


        ////////////////////////////////////////////////
        // SDES Item #1 :CNAME
        /*    0                   1                   2                   3
              0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             |    CNAME=1    |     length    | user and domain name         ...
             +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ */

        byte item = RTCPConstants.RTCP_SDES_CNAME;
        byte user_and_domain[] = new byte [ Session.getCName().length()];
        user_and_domain = Session.getCName().getBytes();


        // Copy the CName item related fields
        byte CNAME_Header[] = { item, (byte) user_and_domain.length };

        // Append the header and CName Information in the SDES Item Array
        byte SDESItem[]= new byte[0] ;
        SDESItem = PacketUtils.Append ( SDESItem, CNAME_Header );
        SDESItem = PacketUtils.Append ( SDESItem, user_and_domain );




        //Variable to keep track of whether to send e-mail info out or not.
        //E-mail info is sent out once every 7 packets


        if( ( Math.IEEEremainder ( packetcount , (double) 7 ) ) == 0)
            {
                // Construct  the e-mail information

                byte item2    = RTCPConstants.RTCP_SDES_EMAIL;
                byte email[]  = new byte [Session.getEMail().length()];
                email         =   Session.getEMail().getBytes();
                byte EMAIL_Header[] = {item2 , (byte) email.length};

                SDESItem = PacketUtils.Append (SDESItem, EMAIL_Header);
                SDESItem = PacketUtils.Append (SDESItem, email);

            }
                packetcount++;

        int remain = (int) Math.IEEEremainder ( SDESItem.length
                                                , (double) 4 );


        int PadLen = 0;
        // e.g. remainder -1, then we need to pad 1 extra
        // byte to the end to make it to the 32 bit boundary
        if ( remain < 0 )
            PadLen = Math.abs ( remain );
        // e.g. remainder is +1 then we need to pad 3 extra bytes
        else if ( remain > 0 )
            PadLen = 4-remain;


        // Assemble all the info into a packet
        //byte SDES[] = new byte [ FIXED_HEADER_SIZE + ss.length + user_and_domain.length + PadLen];
        byte SDES [] = new byte[2];

        // Determine the length of the packet (section 6.4.1 "The length of the RTCP packet
        // in 32 bit words minus one, including the heade and any padding")
        byte SDESlength[] = PacketUtils.LongToBytes ( ( FIXED_HEADER_SIZE +
                                                        ss.length +
                                                        SDESItem.length +
                                                        PadLen)/4-1,
                                                        2 );

        SDES [0] = V_P_SC;
        SDES [1] = PT[0];
        SDES = PacketUtils.Append ( SDES, SDESlength );
        SDES = PacketUtils.Append ( SDES, ss );
        SDES = PacketUtils.Append (SDES, SDESItem);



        // Append necessary padding fields
        byte PadBytes [] = new byte [ PadLen ];
        SDES = PacketUtils.Append ( SDES, PadBytes );

        return SDES;
   }

   
    /**
    *   Creates the Reception reports by determining which source need to be included
    *   and makes calls to AssembleRTCPReceptionReportBlock function to generate the individual
    *   blocks. The function returns the fixed length RTCP Sender Info ( 5*32 bits or 20 bytes ).
    *
    *   @return The RTCP Reception Report Blocks 
    */
   

    private byte[] AssembleRTCPReceptionReport()
    {
        byte ReportBlock [] = new byte [0];

        // Keeps track of how many sender report blocks are generated.  Make sure
        // that no more than 31 blocks are generated.
        int ReceptionReportBlocks = 0;

        Enumeration ActiveSenderCollection = Session.GetSources();

        // Iterate through all the sources and generate packets for those
        // that are active senders.
        while ( ReceptionReportBlocks < 31 && ActiveSenderCollection.hasMoreElements() )
        {
            Source s = (Source) ActiveSenderCollection.nextElement();

           // Session.outprintln ( "\ns.TimeoflastRTPArrival : " + s.TimeOfLastRTPArrival + "\t"
          //                   + "Session.TimeOfLastRTCPSent : " + Session.TimeOfLastRTCPSent + "\n" );

            if ( (s.TimeOfLastRTPArrival > Session.TimeOfLastRTCPSent ) && (s.SSRC != Session.SSRC)  )
            {
                ReportBlock = PacketUtils.Append ( ReportBlock, AssembleRTCPReceptionReportBlock ( s ) );
                ReceptionReportBlocks++;
            }

            // TODO : Add logic for more than 31 Recption Reports - AN

        }

        return ReportBlock;
    }

   
   /**
   *
   *    Constructs a fixed length RTCP Reception.
   *    Report block ( 6*32 bits or 24 bytes ) for a particular source.
   *
   *   @param Source  The source for which this Report is being constructed.
   *   @return        The RTCP Reception Report Block 
   *
   */
  
   private byte[] AssembleRTCPReceptionReportBlock(Source RTPSource )
   {
    /*
         +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |                 SSRC_1 (SSRC of first source)                 | report
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
       | fraction lost |       cumulative number of packets lost       |   1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |           extended highest sequence number received           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                      interarrival jitter                      |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         last SR (LSR)                         |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                   delay since last SR (DLSR)                  |
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

      */

        byte RRBlock [] = new byte [ 0 ];

        //Update all the statistics associated with this source
        RTPSource.UpdateStatistics();

         //SSRC_n - source identifier - 32 bits
        byte SSRC[] = PacketUtils.LongToBytes ( (long) RTPSource.SSRC, 4 );

        //fraction lost -   8 bits
        byte fraction_lost[] = PacketUtils.LongToBytes ( (long) RTPSource.fraction, 1 );


        // cumulative number of packets lost -  24 bits
        byte pkts_lost[] = PacketUtils.LongToBytes ( (long) RTPSource.lost, 3 );

        // extended highest sequence number received - 32 bits
        byte last_seq[] = PacketUtils.LongToBytes ( (long) RTPSource.last_seq, 4);

        // interarrival jitter - 32 bits
        byte jitter[] = PacketUtils.LongToBytes ( (long) RTPSource.jitter, 4);

        // last SR timestamp(LSR) - 32 bits
        byte lst[] = PacketUtils.LongToBytes ( (long) RTPSource.lst, 4);

        // delay since last SR (DLSR)   32 bits
        byte dlsr[] =  PacketUtils.LongToBytes ( (long) RTPSource.dlsr, 4);

        RRBlock = PacketUtils.Append ( RRBlock, SSRC );
        RRBlock = PacketUtils.Append ( RRBlock, fraction_lost);
        RRBlock = PacketUtils.Append ( RRBlock, pkts_lost );
        RRBlock = PacketUtils.Append ( RRBlock, last_seq );
        RRBlock = PacketUtils.Append ( RRBlock, jitter );
        RRBlock = PacketUtils.Append ( RRBlock, lst );
        RRBlock = PacketUtils.Append ( RRBlock, dlsr );
       // Session.outprintln("fraction_lost" + RRBlock[4]);

        return RRBlock;
    }

  
    /**
    *
    *   Constructs a "BYE" packet (PT=BYE=203)
    *
    *   @param   ReasonsForLeaving.
    *   @return  The BYE Packet .
    *
    */
  

    private byte[] AssembleRTCPByePacket ( String ReasonsForLeaving )
    {
        /*
        7.6 BYE: Goodbye RTCP packet
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |V=2|P|    SC   |   PT=BYE=203  |             length            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                           SSRC/CSRC                           |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       :                              ...                              :
       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
       |     length    |               reason for leaving             ... (opt)
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       */

        final int FIXED_HEADER_SIZE = 4; // 4 bytes
        // construct the first byte containing V, P and SC
        byte V_P_SC;
        V_P_SC =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x01 )
                            );

        // Generate the payload type byte
        byte PT[] = PacketUtils.LongToBytes ( (long)RTCPConstants.RTCP_BYE, 1);


        // Generate the SSRC
        byte ss[] = PacketUtils.LongToBytes ( (long) Session.SSRC, 4 );

        byte TextLength [] = PacketUtils.LongToBytes ( ReasonsForLeaving.length() , 1);

        // Number of octects of data (excluding any padding)
        int DataLen = FIXED_HEADER_SIZE + ss.length + 1 + ReasonsForLeaving.length();

        // Calculate the pad octects required
        int PadLen = PacketUtils.CalculatePadLength( DataLen );
        byte PadBytes [] = new byte [ PadLen ];

        // Length of the packet is number of 32 byte words - 1
        byte PacketLength [] =  PacketUtils.LongToBytes ( ( DataLen + PadLen )/4 -1, 2);

        ///////////////////////// Packet Construction ///////////////////////////////
        byte Packet[] = new byte [1];

        Packet[0] = V_P_SC;
        Packet = PacketUtils.Append ( Packet, PT );
        Packet = PacketUtils.Append ( Packet, PacketLength );
        Packet = PacketUtils.Append ( Packet, ss );
        Packet = PacketUtils.Append ( Packet, TextLength );
        Packet = PacketUtils.Append ( Packet, ReasonsForLeaving.getBytes() );
        Packet = PacketUtils.Append ( Packet, PadBytes );

        return Packet;
    }

  
    /**
    *   Sends the byte array RTCP packet.
    *   Zero return is error condition
    *
    *   @param      packet[] packet to be sent out.
    *   @return     1 for success, 0 for failure. 
    */
  
    private int SendPacket ( byte packet [])
    {
   			DatagramPacket DGram = new DatagramPacket( packet, packet.length, m_InetAddress, m_MulticastRTCPPort );

   			// Set ttl=5 and send
            try
            {
       			m_RTCPSenderSocket.send ( DGram, (byte) 5 );
       			return (1);
            }
        	catch ( java.io.IOException e )
        	{
        	    System.err.println ("Error: While sending the RTCP Packet");
        		System.err.println (e);
        		return (0);
        	}
    }

}

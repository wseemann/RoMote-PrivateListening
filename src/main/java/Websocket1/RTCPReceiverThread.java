package Websocket1;

import java.lang.*;
import java.net.*;
import java.util.*;


/*
*   This class encapsulates the functionality to receive and parse out RTCP packets. This
*   class provides a seperate thread to receive these RTCP Packets. Depending on the kind of RTCP
*   packet received (e.g. SR, RR, SDES, BYE) the receiver parses them out correctly and 
*   updates several session and source level statistics. Each packet having been parsed
*   posts an event to the application corresponding to the packet that the receiver
*   received
*/


public class RTCPReceiverThread extends Thread
{
    /**
    *   Receiver Port for RTCP Packets
    */   	
   	private int  m_port;
   	
   	/**
    *   Multicast Address for RTCP Packets
    */
	private InetAddress m_InetAddress;

    /**
    *    Constructor for the class. Takes in a TCP/IP Address and a port number
    *    
    *   @param   MulticastGroupIPAddress Dotted representation of the Multicast address.
    *   @param   RTCPGroupPort           Port for Multicast group (for receiving RTP Packets).
    *
    */
    
    RTCPReceiverThread ( InetAddress MulticastGroupIPAddress, int RTCPGroupPort)
    {
        m_InetAddress = MulticastGroupIPAddress;
        m_port = RTCPGroupPort;
    }

   /**
    *   Starts the RTCPReceiver Thread
    */
    public void run()
    {
        StartRTCPReceiver();
    }

    /** 
    *   The thread creates a new multicast socket according to the Multicast address and
    *   port provided in the constructor. Then the thread waits in idle state for the reception
    *   of a RTCP Packet. As soon as a RTCP Packet is received, the receiver first validates the 
    *   RTCP Packet accoring to the following rules
    *   RTCP Header Validity Checks
	*			 1) Version should always =2
	*			 2) The payload type field of the first RTCP Packet should be SR or RR
	*			 3) The Padding Bit (P) should be zero for the first packet
	*			 4) The length fields of individual RTCP Packets must total to the
	*			    overall length of the compound RTCP Packet
	*
	*   After the packet has been validated, it is parsed differently according to the type
	*   of packet (e.g. Sender Report, Receive Report, BYE, SDES) etc.
	*   For each packet parsed, session and source level statistics are updated and an 
	*   event is posted for the application corresponding to the type of packet that
	*   was parsed.
	*
    */
    public void StartRTCPReceiver()
    {

        Session.outprintln ("RTCP Receiver Thread started ");
        Session.outprintln ("RTCP Group: " + m_InetAddress + ":" + m_port);

		try
		{
			DatagramSocket socket = new DatagramSocket ( m_port );
			//socket.joinGroup ( m_InetAddress );

			byte packet[] = new byte[1024];
			DatagramPacket Header = new DatagramPacket( packet, packet.length );
			int i= 0;

			while (1==1)
			{
			    // Preliminary Information - Time Pkt Received, Length of the Packet
				// hostname , IP address :: portnumber
                Session.outprint ("\n");

        		Session.outprint( new Long ((new Date()).getTime()).toString() + "   " + "RTCP" + "  ");

				socket.receive(Header);
 				Session.outprint("Len " + Header.getLength() +  "  "  +"from " + Header.getAddress()+ ":" +	Header.getPort() + "\n");



				/**********************************************************************
				 RTCP Header Validity Checks
				 1) Version should always =2
				 2) The payload type field of the first RTCP Packet should be SR or RR
				 3) The Padding Bit (P) should be zero for the first packet
				 4) The length fields of individual RTCP Packets must total to the
				    overall length of the compound RTCP Packet
				  **********************************************************************/

				// RTCP Header Validity Check 2
				int PayloadType_1 = (int) (packet[1] & 0xff);
				System.out.println("PACKET------>" + PayloadType_1);
				if  ((PayloadType_1 !=  RTCPConstants.RTCP_SR) &
				    (PayloadType_1 !=   RTCPConstants.RTCP_RR ) )
 					Session.outprint("RTCP Header Check Fail : First Payload type not a SR or RR\n");


				//RTCP Header Validity Check 3

				if ((((packet[0] & 0xff) >> 5) & 0x01) != 0)
					Session.outprint("RTCP Header Check Fail : First Padding bit not zero\n");

				//Since a RTCP Packet may contain many types of RTCP Packets
				// Keep parsing the packets until the no. of bytes parsed  = total no of bytes read

				int BytesRead = 0;
				int TotalBytesRead =0;

				while(TotalBytesRead < Header.getLength())
				{
					// RTCP Header Validity Check 1

					byte version = (byte)((packet[BytesRead] & 0xff) >> 6);
					if ( version != 0x02)
 						Session.outprint( "RTCP Header Check Fail : Wrong Version\n");

					// Check the length of this particular packet
					short length = (short)((packet[BytesRead+2] << 8) | (packet [BytesRead +3] & 0xff));


					//Check the Payload type of this packet
					int pt =  ((packet[BytesRead+1] & 0xff) );

    				if(pt == RTCPConstants.RTCP_SR)
    				{
    					// Create an RTCP SR Packet and post it for interested
                         // listeners
     			        RTCPSenderReportPacket RTCPSRPacket = new RTCPSenderReportPacket();
     			        SenderInfo senderInfo = new SenderInfo();

    					// Check if there are any reception reports
    					byte RC = (byte)((packet[BytesRead] & 0xff) & 0x1f);
     					Session.outprint("RC" + RC +"\n");

     					long ssrc = (((packet[BytesRead + 4] & 0xff) << 24) | ((packet[BytesRead+5] & 0xff) << 16) | ((packet[BytesRead +6] & 0xff) << 8) | (packet[BytesRead +7] & 0xff));


                        //Update the Sender SSRC for the SR Packet
                        RTCPSRPacket.SenderSSRC = ssrc;

    					// Increment the bytes
    					BytesRead +=8;

     					Session.outprint("(SR ssrc=0x" + Long.toHexString(ssrc) + "    count= " + RC
    							 +"   len =  " + length + "\n");


                        // Get the source from the Session corresponding to this particular SSRC
    					Source Sender_Source = Session.GetSource(ssrc);
    					if (Sender_Source != null)
    					{
    					    // Update all the source parameters and statistics
    					    Sender_Source.TimeOfLastRTCPArrival = Session.CurrentTime();

    					}


    					if (length!=1) // Not an empty packet
    					{
     						long ntp_sec =(  ((packet[BytesRead] & 0xff) << 24) |
    								((packet[BytesRead +1] & 0xff) << 16) |
    								((packet[BytesRead+2] & 0xff) << 8) |
    								  (packet[BytesRead+3] & 0xff) );

     						long ntp_frac =(((packet[BytesRead+4] & 0xff) << 24) | ((packet[BytesRead+5] & 0xff) << 16) | ((packet[BytesRead+6] & 0xff) << 8) |   (packet[BytesRead+7] & 0xff));
     						long rtp_ts = (((packet[BytesRead+8] & 0xff) << 24) | ((packet[BytesRead+9] & 0xff) << 16) | ((packet[BytesRead+10] & 0xff) << 8) |   (packet[BytesRead+11] & 0xff));
     						long psent = (((packet[BytesRead+12] & 0xff) << 24) | ((packet[BytesRead+13] & 0xff) << 16) | ((packet[BytesRead+14] & 0xff) << 8) |  (packet[BytesRead+15] & 0xff));
     						long osent =  (((packet[BytesRead+16] & 0xff) << 24) | ((packet[BytesRead+17] & 0xff) << 16) | ((packet[BytesRead+18] & 0xff) << 8) | (packet[BytesRead+19] & 0xff));

    					    //Set the lst - middle 32 bits out of NTPTimeStamp
    					    Sender_Source.lst = ((packet[BytesRead+6] & 0xff) << 24) |   ((packet[BytesRead+7] & 0xff)<<16)|(((packet[BytesRead+8] & 0xff) << 8) | (packet[BytesRead+9] & 0xff) ) ;

                             //Set the SenderInfo part of the SR Packet to be thrown out
                             senderInfo.SenderOctetCount     =           osent;
    					    //Set the arrival time of this SR report
    					    Sender_Source.TimeofLastSRRcvd = Sender_Source.TimeOfLastRTCPArrival;
                             senderInfo.SenderPacketCount    =           psent;
                             senderInfo.RTPTimeStamp         =           rtp_ts;
                             senderInfo.NTPTimeStampLeastSignificant=    ntp_frac;
                             senderInfo.NTPTimeStampMostSignificant=     ntp_sec;

                             RTCPSRPacket.SenderInfo = senderInfo;

     						Session.outprint("ntp = " +  ntp_sec + " " + ntp_frac + "   ts=   " +
    								rtp_ts + "  psent =  " + psent + "  osent   " + osent
    								+ "\n" + ")" + "\n");

    						BytesRead +=20;
    					}

    					//Parse the reports
    					for(int j=0; j<RC;j++)
    					{
     					 	long Rcvr_ssrc = (((packet[BytesRead] & 0xff) << 24) | ((packet[BytesRead+1] & 0xff) << 16) | ((packet[BytesRead+2] & 0xff) << 8) | (packet[BytesRead +3 ] & 0xff));
     					 	double FractionLost = (packet[BytesRead+4] &  0xff);
     					 	long CumPktsLost = (((     ((packet[BytesRead+4] & 0xff) << 24)
    					 				| ((packet[BytesRead+5] & 0xff) << 16)
    					 				| ((packet[BytesRead+6] & 0xff) << 8)
    					 				| (packet[BytesRead+7] & 0xff)) )
    					 				 & 0xffffff);

     					 	long ExtHighSqRcvd =(((packet[BytesRead + 8] & 0xff) << 24) | ((packet[BytesRead + 9] & 0xff) << 16) | ((packet[BytesRead+10] & 0xff) << 8) | (packet[BytesRead+11] & 0xff));
     						long IntJitter =(((packet[BytesRead+12] & 0xff) << 24) | ((packet[BytesRead+13] & 0xff) << 16) | ((packet[BytesRead+14] & 0xff) << 8) | (packet[BytesRead+15] & 0xff));
     						long LastSR = (((packet[BytesRead+16] & 0xff) << 24) | ((packet[BytesRead+17] & 0xff) << 16) | ((packet[BytesRead+18] & 0xff) << 8) | (packet[BytesRead+19] & 0xff));
     						long Delay_LastSR = (((packet[BytesRead+20] & 0xff) << 24) | ((packet[BytesRead+21] & 0xff) << 16) | ((packet[BytesRead+22] & 0xff) << 8) | (packet[BytesRead+23] & 0xff));

    					    // Update the statistics -  only if the rcvr_ssrc matches your
    					    //own ssrc

    					    Source Reception_Source = Session.GetMySource();

    					    // Check if  sender report contains information about this particular source
    					    if ( Rcvr_ssrc== Reception_Source.SSRC)
    					    {
    					            // Create a new report block and set its attributes
    					            RTCPSRPacket.containsReportBlock =true;
    					            ReportBlock reportblock = new ReportBlock();
    					            reportblock.FractionLost    =   FractionLost ;
     			        	        reportblock.CumulativeNumberOfPacketsLost   =  CumPktsLost;
     			        	        reportblock.ExtendedHighestSequenceNumberReceived   =   ExtHighSqRcvd;
     			        	        reportblock.InterarrivalJitter  =   IntJitter;
     			        	        reportblock.LastSR  =   LastSR;
                                    reportblock.Delay_LastSR = Delay_LastSR;

                                   //Set the Sender Report Packet's Report Block to this Report Block
                                    RTCPSRPacket.ReportBlock = reportblock;

    					    }


    						//Print the statistics
     						Session.outprint("(ssrc=0x" + Long.toHexString(Rcvr_ssrc) + "    fraction =  " + FractionLost
    								+"     lost =  " + CumPktsLost + "     last_seq =  " + ExtHighSqRcvd
    								+ "   jit  =   " + IntJitter + "  lsr =  " + LastSR
    								+ "    dlsr = " + Delay_LastSR + "\n");
    						BytesRead+=24;


    					}

    					// Update Average RTCP Packet size
    					Session.avg_rtcp_size = 1/16*(length*4 + 1 ) + 15/16*(Session.avg_rtcp_size);

    					if (ssrc != Session.SSRC)
    					{
    					//Post the SR Packet only if its not the same packet sent out by this source
    					Session.postAction ( RTCPSRPacket );
    					}

    				}

    				if (pt == RTCPConstants.RTCP_RR)

    				{

    					// Create an RTCP RR Packet and post it for interested
                        // listeners
     			        RTCPReceiverReportPacket RTCPRRPacket = new RTCPReceiverReportPacket();


    					// Check if there are any reception reports
    					byte RC = (byte)((packet[BytesRead] & 0xff) & 0x1f);

     					long ssrc = (((packet[BytesRead+4] & 0xff) << 24) | ((packet[BytesRead+5] & 0xff) << 16) | ((packet[BytesRead+6] & 0xff) << 8) | (packet[BytesRead+7] & 0xff));

     					Session.outprint("( RR ssrc=0x" + Long.toHexString(ssrc) + "    count= " + RC
    							 +"   len =  " + length  + "\n" + ")" + "\n");

    					// Get the source from the Session corresponding to this particular SSRC
    					Source Sender_Source = Session.GetSource(ssrc);

    					//Set the Sender SSRC of the RR Packet
    					RTCPRRPacket.SenderSSRC = ssrc;


    					if (Sender_Source != null)
    					{
    					    // Update all the source parameters and statistics
    					    Sender_Source.TimeOfLastRTCPArrival = Session.CurrentTime();
    					}
    					//Increment the Bytes read by the length of this packet
    					BytesRead +=8;

    					//Parse the reports
    					for(int j=0; j<RC;j++)
    					{
     					 	long Rcvr_ssrc = (((packet[BytesRead] & 0xff) << 24) | ((packet[BytesRead+1] & 0xff) << 16) | ((packet[BytesRead+2] & 0xff) << 8) | (packet[BytesRead +3 ] & 0xff));

    					 	byte FractionLost = (byte)(packet[BytesRead+4] &  0xff);
     					 	long CumPktsLost = (((     ((packet[BytesRead+4] & 0xff) << 24)

    					 				| ((packet[BytesRead+5] & 0xff) << 16)
    					 				| ((packet[BytesRead+6] & 0xff) << 8)
    					 				| (packet[BytesRead+7] & 0xff)) )
    					 				 & 0xffffff);

     					 	long ExtHighSqRcvd =(((packet[BytesRead + 8] & 0xff) << 24) | ((packet[BytesRead + 9] & 0xff) << 16) | ((packet[BytesRead+10] & 0xff) << 8) | (packet[BytesRead+11] & 0xff));
     						long IntJitter =(((packet[BytesRead+12] & 0xff) << 24) | ((packet[BytesRead+13] & 0xff) << 16) | ((packet[BytesRead+14] & 0xff) << 8) | (packet[BytesRead+15] & 0xff));
     						long LastSR = (((packet[BytesRead+16] & 0xff) << 24) | ((packet[BytesRead+17] & 0xff) << 16) | ((packet[BytesRead+18] & 0xff) << 8) | (packet[BytesRead+19] & 0xff));
     						long Delay_LastSR = (((packet[BytesRead+20] & 0xff) << 24) | ((packet[BytesRead+21] & 0xff) << 16) | ((packet[BytesRead+22] & 0xff) << 8) | (packet[BytesRead+23] & 0xff));


    						// Print the statistics
     						Session.outprint("(ssrc=0x" + Long.toHexString(Rcvr_ssrc) + "    fraction =  " + FractionLost
    								+"     lost =  " + CumPktsLost + "     last_seq =  " + ExtHighSqRcvd
    								+ "   jit  =   " + IntJitter + "  lsr =  " + LastSR
    								+ "    dlsr = " + Delay_LastSR + "\n");
    						BytesRead+=24;

    						// Update the statistics - only if the rcvr_ssrc matches your
    					    //own ssrc

    					     Source Reception_Source = Session.GetMySource();
    					    if ( Rcvr_ssrc== Reception_Source.SSRC)
    					    {
    					     // Update all the source parameters and statistics


    					     // Create a new report block and set its attributes
                              RTCPRRPacket.containsReportBlock =true;
    					      ReportBlock reportblock = new ReportBlock();
    					      reportblock.FractionLost    =   FractionLost ;
     			        	  reportblock.CumulativeNumberOfPacketsLost   =  CumPktsLost;
     			        	  reportblock.ExtendedHighestSequenceNumberReceived   =   ExtHighSqRcvd;
     			        	  reportblock.InterarrivalJitter  =   IntJitter;
     			        	  reportblock.LastSR  =   LastSR;
                              reportblock.Delay_LastSR = Delay_LastSR;

                               //Set the Receiver Report Packet's Report Block to this Report Block
                               RTCPRRPacket.ReportBlock = reportblock;

    					    }

    					    // Update Average RTCP Packet size
    					    Session.avg_rtcp_size = 1/16*(length*4 + 1 ) + 15/16*(Session.avg_rtcp_size);

                            if (ssrc != Session.SSRC)
                            {
                                //Post the RR Packet only if its not the same packet sent out by this source
                                Session.postAction ( RTCPRRPacket );
                            }

    					}

    				}

    				if (pt ==RTCPConstants.RTCP_SDES)
    				{
    					int len = (length+1) * 4 ;
    					byte SC = (byte)((packet[BytesRead] & 0xff) & 0x1f);
    					BytesRead+=4;
    					len-=4 ;//Keep track of no. of bytes read from this package

    					for(int j=0; j<SC;j++) //Parse the packet for all sources
    				        {
    				        	//Read in the SSRC of the source
     				        	long ssrc = (((packet[BytesRead] & 0xff) << 24) | ((packet[BytesRead+1] & 0xff) << 16) | ((packet[BytesRead+2] & 0xff) << 8) | (packet[BytesRead+3] & 0xff));


     				        	Session.outprint("(SDES ssrc=0x" + Long.toHexString(ssrc) + "    count= " + SC

    							 +"   len =  " + length + "\n" + ")" + "\n");

    				        	// Increment the Bytes Read
    				        	BytesRead+=4;
    				        	len-=4;

    				        	// Note that we don't know how many items, so have to check if the byte is null or not
    				        	//while (((byte)(packet[BytesRead] & 0xff)) != 0x00)
    				        	while ( (((byte)(packet[BytesRead] & 0xff)) != 0x00) && (len > 0))
    				        	{
    				        		byte  name= (byte)(packet[BytesRead] & 0xff);
    				        		String ItemType="";
    				        		if (name == RTCPConstants.RTCP_SDES_END) ItemType = "BYE" ;
    				        		if (name == RTCPConstants.RTCP_SDES_CNAME) ItemType = "CNAME" ;
    				        		if (name == RTCPConstants.RTCP_SDES_NAME) ItemType = "NAME" ;
    				        		if (name == RTCPConstants.RTCP_SDES_EMAIL) ItemType = "EMAIL" ;
    				        		if (name == RTCPConstants.RTCP_SDES_PHONE) ItemType = "PHONE" ;
    				        		if (name == RTCPConstants.RTCP_SDES_LOC) ItemType = "LOC" ;
    				        		if (name == RTCPConstants.RTCP_SDES_TOOL) ItemType = "TOOL" ;
    				        		if (name == RTCPConstants.RTCP_SDES_NOTE) ItemType = "NOTE" ;
    				        		if (name == RTCPConstants.RTCP_SDES_PRIV) ItemType = "PRIV" ;

    				        		byte Fieldlength =  (byte)(packet[BytesRead+1] &0xff);
    				        		BytesRead +=2;
    				        		len-=2;

    							String text ="";

    				        		for (j = 0 ; j<Fieldlength ;j++)
    				        		{
    				        		    char character  =(char) ((packet[BytesRead+j] ) & 0xff);
    				        			text += character;

    				       		 	}

    				        		BytesRead += Fieldlength;
    				        		len-= Fieldlength;

     				        		Session.outprint(ItemType + "=" + "\"" + text + "\"  ");

                                     // Create an RTCP SDES Packet and post it for interested
                                     // listeners
     			        	        RTCPSDESPacket rtcpSdesPkt = new RTCPSDESPacket();
     			        	        SDESItem sdesItem = new SDESItem();

     			        	        sdesItem.Type = name;
     			        	        sdesItem.Value = text;

     			        	        rtcpSdesPkt.SDESItem = sdesItem;

                                    // Post Action if the packet was not generated by this source
                                    if (ssrc != Session.SSRC)
                                    {
     			        	            Session.postAction ( rtcpSdesPkt );
     			        	        }
    				        	}

    				            	//Check for null bytes and increment the count - in each chunk
    				            	// the item list is terminated by null octets to the next 32 bit
    				            	//word boundary

    				        	while ((((byte)(packet[BytesRead] & 0xff)) == 0x00) &&
    				        			(len>0) )
    				        		{
    				        			BytesRead++;
    				        			len--;
    				        		}
    				        }
    				        // Update Average RTCP Packet size
    					    Session.avg_rtcp_size = 1/16*(length*4 + 1 ) + 15/16*(Session.avg_rtcp_size);
    				}

    				if (pt== RTCPConstants.RTCP_BYE)
    				{

    					byte SC = (byte)((packet[BytesRead] & 0xff) & 0x1f);
    					BytesRead+=4;
     					Session.outprint("(BYE" +"    count= " + SC
    							 +"   len =  " + length + "\n" + ")" + "\n");

    					//Construct a BYE Packet Array
                        RTCPBYEPacket RTCPBYEPacketArray[] = new RTCPBYEPacket[SC];

    					for(i=0; i< SC; i++)
    					{
    					    // For each source get the SSRC
     						long ssrc = (((packet[BytesRead ] & 0xff) << 24) | ((packet[BytesRead+1] & 0xff) << 16) | ((packet[BytesRead +2] & 0xff) << 8) | (packet[BytesRead +3] & 0xff));

     						if(Session.IsByeRequested==false)
    						{
    						    //  Ask the Session to remove the source object corresponding
    					        //  to that SSRC

    						    Session.RemoveSource(ssrc);

   	                        }

                            else if(Session.IsByeRequested==true)
                            {
                                // If a BYE has been requested by this particular member and it
                                // receives a BYE from some other source , then add that to the
                                //list of members
                                //- NOTE: This is true for only BYE Packets not any other RTCP
                                // or RTP Packets

                                Session.GetSource(ssrc);

                            }

                            //To make the transmission rate of RTCP Packets more adaptive to changes in
                            //group membership, the "reverse reconsideration algorithm is implemented when a BYE
                            //Packet is received.

                            Session.tn = Session.tc + (Session.GetNumberOfMembers()/Session.pmembers) * (Session.tn - Session.tc);
                            Session.TimeOfLastRTCPSent = Session.tc - (Session.GetNumberOfMembers()/Session.pmembers) * (Session.tc - Session.TimeOfLastRTCPSent);
                            // Reschedule the next RTCP Packet for transmission at time tn
                            // which is now earlier
                            Session.pmembers = Session.GetNumberOfMembers();


                            //Increment the Bytes read by the length of this packet
                            BytesRead += 4;
     						Session.outprint("ssrc=0x" + Long.toHexString(ssrc));

     						Session.outprintln("In the Bye Packet " + i);
     						RTCPBYEPacketArray[i] = new RTCPBYEPacket();
     						RTCPBYEPacketArray[i].SSRC = ssrc;


    					}
    					byte Fieldlength =  (byte)(packet[BytesRead] &0xff);

                        BytesRead ++;

    					String text ="";

    	        		for (int j = 0 ; j < Fieldlength ;j++)
    	        		{
    	        			char character  =(char) ((packet[BytesRead+j] ) & 0xff);
    	        			text += character;
    	       		 	}

                        BytesRead += Fieldlength;

                        Session.outprint("len = " + Fieldlength );

                        Session.outprint("Reasons for leaving=" + "\"" + text + "\"  ");


                        // Read through the null padding bytes and update counters.
                        while ((((byte)(packet[BytesRead] & 0xff)) == 0x00) && BytesRead < Header.getLength()  )
                            BytesRead++;

                        // Update Average RTCP Packet size
                        Session.avg_rtcp_size = 1/16*(length*4 + 1 ) + 15/16*(Session.avg_rtcp_size);


                            for(i=0; i< SC; i++)
                            {
                                RTCPBYEPacketArray[i].ReasonForLeaving = text;

                                //Post the action i.e. generate an event if the packet was not generated from this
                                //source

                                if (RTCPBYEPacketArray[i].SSRC != Session.SSRC)
                                {
             			            Session.postAction (RTCPBYEPacketArray[i]);
             			        }
                            }


    				}

    				if (pt == RTCPConstants.RTCP_APP)
    				{


    					//Increment the Bytes read by the length of this packet
    					BytesRead+=4*(length+1);

    				}
    				TotalBytesRead = BytesRead;

			    }


				// RTCP Header Validity Check 4
 				Session.outprintln ("TotalBytesRead: " + TotalBytesRead + " Header.getLength" + Header.getLength() );


				if ( TotalBytesRead!= Header.getLength())
 					Session.outprintln ( "RTCP Header Check Fail : Bytes Read do not Match Total Packet Length\n");

				i++;

                // Every time a RTCP Packet is received , update the other users timeout
                // i.e remove them from the member or the sender lists if they have
                // not been active for a while

                Session.UpdateSources();

   			}
			/*try
			{
				socket.leaveGroup( m_InetAddress );
			}

			catch ( java.io.IOException e)
			{
 				System.err.println(e);
			}
			socket.close();*/
		}
		catch ( UnknownHostException e )
		{
 			System.err.println (e);
		}
	   catch ( java.io.IOException e )
	    {
 		    System.err.println (e);
		}
    }
}

 package Websocket1;

        import java.lang.*;
        import java.util.*;
        import java.net.*;
        import java.lang.*;
        import java.util.*;


    /**
    *   This class maintains session related information and provides startup functions.
    */

     public class Session extends java.lang.Object
        {

            /**
            *   Canonical end-point identifier SDES Item.
            *
            */
            private  static String       CNAME;

            /**
            *   Electronic mail Address SDES Item.
            *
            */
            private  static String       EMAIL;

            /**
            *   Bandwidth Available to the session.
            *
            */
            private  static double       BANDWIDTH;

            /**
            *   Payload type for this session.
            *
            */
            private  static byte         PayloadType;

            /**
            *   Synchronization Source identifier for this source.
            *
            */
            public  static long         SSRC;

            /**
            *   Total Number of RTP data packets sent out by this source since starting transmission.
            *
            */
            public  static long         PacketCount;

            /**
            *   Total Number of payload octets (i.e not including header or padding)
            *   sent out by this source since starting transmission.
            *
            */
            public  static long         OctetCount;

            /**
            *   Multicast Address.
            *
            */
            public  static InetAddress      m_InetAddress;

            /**
            *   Reference to the RTP Sender and Receiver Thread.
            *
            */
            public  static RTPThreadHandler         m_RTPHandler;


            /**
            *   Reference to the RTCP Sender and Receiver Thread handler.
            *
            */
            public  static RTCPThreadHandler        m_RTCPHandler;

            /**
            *   The startup time for the application.
            *
            */
             public  static long             AppStartupTime;

            /**
            *   Reference to the late Initialization Server.
            *
            */
            private static LateInitializationServer LateInitSrvr;


            /**
            *   The Late Initialization Client.
            *
            */
            private static LateInitializationClient LateInitClient;

            /**
            *   Datasource which will be used to as a late-arrival repository. Server
            *   server will send objects out to late-coming clients and late coming
            *   clients will save the received data in this enumeration.
            */
            public static Enumeration InitializationData;

            /**
            *   This variable determines whether the RTP loopback packets will
            *   generate events. The user can choose to disable loopback.  Default
            *   is true.
            */
            public static boolean EnableLoopBack = true;

            /**
            *   This variable determines whether Debug information will be printed or not.
            *   Default is false.
            *
            */
            public static boolean DebugOutput = false;

            /**
            *   Initialize the Random Number Generator.
            *
            */
            private static Random       rnd = new Random();

            /**
            *   Reference to the RTP event action listener.
            *
            */
            private static RTP_actionListener  m_RTP_actionListener = null;

            /**
            *   Reference to the RTCP event action listener.
            *
            */
            private static RTCP_actionListener m_RTCP_actionListener = null;


            /**
            *    RTCP Related state variables.
            *      (Sec. 6.3 draft-ietf-avt-rtp-new.ps)
            */

            /**
            *   The last time an RTCP packet was transmitted.
            */
            public static double TimeOfLastRTCPSent = 0;

            /**
            *    The current time.
            */
            public static double tc = 0;

            /**
            *    The next scheduled transmission time of an RTCP packet.
            */
            public static double tn = 0;

            /**
            *   The estimated number of session members at time tp.
            */
            public static int pmembers = 0;

            /**
            *   The target RTCP bandwidth, i.e., the total bandwidth that
            *   will be used for RTCP packets by all members of this session, in
            *   octets per second. This should be 5 parameter supplied to the
            *   application at startup.
            */
            public static double rtcp_bw = 0;

            /**
            *   Flag that is true if the application has sent data since the
            *   2nd previous RTCP report was transmitted.
            */
            public static boolean we_sent = false;

            /**
            *   The average RTCP packet size sent by this user.
            */
            public static double avg_rtcp_size = 0;

            /**
            *   Flag that is true if the application has not yet sent an
            *   RTCP packet.
            */
            public static boolean initial = true;

            /**
            *   Average size of the packet constructed by the application
            */
            public static double avg_pkt_sz = 0;

            /**
            *   True if session instantiator requested a close.
            */
            public static boolean IsByeRequested = false;


            /**
            *   Deterministic time interval for next RTCP transmission.
            */
            public static double Td = 0;

            /**
            *   Ramdomized time interval for next RTCP transmission.
            */
            public static double T = 0;

            /**
            *   Time this source last sent an RTP Packet
            */
            public static double TimeOfLastRTPSent = 0;

            /**
            *   A hastable that stores all the sources subscribed to this multicast group
            */
            private static Hashtable   SourceMap;


            /**
            *   Updates all the sources to see whether the sources are still
            *   active members or senders. Iterate through all the sources
            *   and adjust their status according to the following criteria
            *
            *   1.  If Time of last RTPSent < tc - T, then drop them from the list
            *        of active senders
            *   2.  If time of last RTCPSent < tc - 5Td then remove them from the
            *        members list except if its your own SSRC
            *
            *   @return number of sources updated.
            *
            */
            public static int UpdateSources()
            {
                // iterate through the sources updating their flags
                Enumeration SourceCollection  = GetSources();

                int n = 0;

                while ( SourceCollection.hasMoreElements() )
                {
                    Source s = (Source) SourceCollection.nextElement();


                  // If Time of last RTPSent < tc - T, then drop them from the list of active senders

                  if(( s.TimeOfLastRTPArrival < Session.CurrentTime() - T*1000)  && s.ActiveSender ==true)
                    {
                        s.ActiveSender =false;
                      Session.outprintln("Degrading from a sender to member");
                      Session.outprintln("SSRC = 0x" + Integer.toHexString((int)s.SSRC));
                        n++;
                    }


                  // If time of last RTCPSent < tc - 5Td then remove them from the
                  // members list except if its your own SSRC -NOte Td is in seconds
                  // and rest of the times are in milliseconds

                  if ( (s.TimeOfLastRTCPArrival < (Session.CurrentTime() - 5*Td*1000) )  &&  s.SSRC != SSRC )
                    {
                      //Session.outprintln();
                      //Session.outprintln("Td" + Td);
                      //Session.outprintln("Removing Source : " + "SSRC = 0x" + Integer.toHexString((int)s.SSRC));
                      Session.outprintln("Removing Source : " + "SSRC = 0x" + Integer.toHexString((int)s.SSRC));
                      //Session.outprintln("Current Time "  + Session.CurrentTime());
                      //Session.outprintln ("s.TimeOfLastRTCPArrival" + s.TimeOfLastRTCPArrival);
                        RemoveSource (s.SSRC );
                      Session.outprintln("No. of members" + GetNumberOfMembers ());
                      Session.outprintln("No. of senders" + GetNumberOfActiveSenders ());
                      Session.outprintln();
                      Session.outprintln();
                      Session.outprintln();
                      Session.outprintln();

                        n++;
                    }
                }

                // return the number of sources updated
                return (n);
            }


            /**
            *   The only constructor. Requires CNAME and session bandwidth.
            *   Initializes the SSRC to a randomly generated number.
            *
            *   @param  MulticastGroupIPAddress Dotted decimal representation of the Multicast group IP address.
            *   @param  MulticastGroupPort  Port number of the Multcast group.
            *   @param  RTCPGroupPort       Port on which the session will receive ( and send to ) the RTCP packets.
            *   @param  RTPSendFromPort     Local port from which the RTP packets are sent out (must be different from MulticastGroupPort).
            *   @param  RTCPSendFromPort    Local port from which the RTCP packets are sent out (must be different from RTCPGroupPort).
            *   @param  bandwidth           Bandwidth available to the session.
            */
            public Session (String  MulticastGroupIPAddress,
                            int     MulticastGroupPort ,
                            int     RTCPGroupPort,
                            int     RTPSendFromPort,
                            int     RTCPSendFromPort,
                            double  bandwidth)

            {
                BANDWIDTH = bandwidth;

                SourceMap = new Hashtable();

                m_InetAddress = GetInetAddress ( MulticastGroupIPAddress );

                // Create a new RTP Handler thread (but do not start it yet)
                m_RTPHandler = new RTPThreadHandler ( m_InetAddress, RTPSendFromPort , MulticastGroupPort );

                // Create a new RTCP Handler thread (but do not start it yet)
                //  Set the sendto and recvfrom ports
                m_RTCPHandler = new RTCPThreadHandler ( m_InetAddress, RTCPSendFromPort, RTCPGroupPort );

                // Initilize session level variables
                Initialize();

              Session.outprintln ( "SSRC: 0x" + Long.toHexString(SSRC) + " CName: " + CNAME );

            }

            /**
            *   Set the Payload type.
            */
            public synchronized static void setPayloadType ( int payloadType ) {
                PayloadType = (byte) payloadType;
            }

            /**
            *   Get the Payload type.
            */
            public synchronized static byte getPayloadType ( ) {
                return PayloadType;
            }

            /**
            *   Set the CNAME.
            */
            public synchronized static void setCName ( String cname ) {
                CNAME = cname;
            }

            /**
            *   Get the CNAME.
            */
            public synchronized static String getCName () {
                return CNAME;
            }

            /**
            *   Set the Email.
            */
            public synchronized static void setEMail ( String email ) {
                EMAIL = email;
            }

            /**
            *   Get the Email.
            */
            public synchronized static String getEMail ( ) {
                return EMAIL;
            }

            /**
            *   Starts the RTP Receiver thread.
            */
            public synchronized static void StartRTPReceiverThread( )
            {
              m_RTPHandler.start();
            }

            /**
            *   Starts the RTCP Receiver thread.
            */
            public synchronized static void StartRTCPReceiverThread()
            {
                m_RTCPHandler.startRTCPReceiverThread();
            }

            /**
            *   Starts the RTCP Sender thread
            */
            public synchronized static void StartRTCPSenderThread()
            {
                m_RTCPHandler.startRTCPSenderThread();
            }

            /**
            *   Starts the RTCP Sender thread
            *
            */
            public synchronized static void StopRTCPSenderThread()
            {
                m_RTCPHandler.stopRTCPSenderThread();
            }

            /**
            *   Retrieves a source object from the map using the given
            *   SSRC as a key.  If the source does not exist, it is added to the
            *   map and newly created source object is returned.
            *
            *   @param  keySSRC The SSRC to look for in the map, if it doesn't exist
            *                   a new source is created and returned.
            *   @return    The source corresponding the given SSRC, this source may be
            *               extracted from the map or newly created.
            */
            public synchronized static Source GetSource ( long keySSRC )
            {
                Source s;

                if ( SourceMap.containsKey ( new Long( keySSRC) ) )
                    s = (Source) SourceMap.get ( new Long( keySSRC) );
                else    // source doesn't exist in the map, add it
                {
                    s = new Source ( keySSRC );
                    AddSource ( keySSRC, s);

                }

                return s;
            }

            /**
            *   Removes a source from the map.
            *
            *   @param  sourceSSRC The source with this SSRC has to be removed.
            */
            public synchronized static int RemoveSource ( long sourceSSRC )
            {
                if (SourceMap.get ( new Long(sourceSSRC)) != null )
              {
                    SourceMap.remove ( new Long(sourceSSRC) );
                  Session.outprintln("Removing Source : " + "SSRC = 0x" + Integer.toHexString((int)sourceSSRC));
                  Session.outprintln("No. of members" + GetNumberOfMembers ());
                  Session.outprintln("No. of senders" + GetNumberOfActiveSenders ());
              }
                else
                {
                    System.err.println ( "Trying to remove SSRC which doesnt exist :"+
                                        sourceSSRC );
                }

                return 0;
            }

            /**
            *   Creates and return a InetAddress object.
            *
            *   @param MulticastAddress    Dotted decimal IP address from which a <b> InetAddress </b> object
            *                              will be created and returned.
            *   @return                    Desired InetAddress object.
            */
            public synchronized static InetAddress GetInetAddress ( String MulticastAddress )
            {
                InetAddress ia = null;
        	    try
        	    {
            	    try
            	    {
                	    ia = InetAddress.getByName ( MulticastAddress );
                	}
                	catch (UnknownHostException e)
                	{
                	    ia = InetAddressFactory.newInetAddress ( MulticastAddress );
                	}
                }
                catch (Exception e)
                {
                    System.err.println (e);
                    System.exit (1);
                }

                return (ia);
            }

            /**
            *   Returns the number of members.
            *
            *   @return Total number of members.
            */
            public static int GetNumberOfMembers ()
            {
                // Go through the map and return the total number of sources.
                return SourceMap.size();
            }

            /**
            *   Returns the number of active senders.
            *
            *   @return Number of senders.
            */
            public static int GetNumberOfActiveSenders()
            {
                // Hasttable
              Enumeration SourceCollection  = GetSources();
                int i=0;
              while ( SourceCollection.hasMoreElements() )
              {
                  Source s = (Source) SourceCollection.nextElement();

                  if ( s.ActiveSender == true )
                  {
                       i++;
                  }
              }
                return (i);
            }

            /**
            *   Calculates the next interval, sets the T and Td class level static variables.
            *
            *   Method to calculate the RTCP transmission interval T.
            *   from Section A7 Computing the RTCP Transmission Interval
            *   ( with minor modifications )
            *
            *   @return The Calculated Interval
            */
            static synchronized double CalculateInterval ()
            {
                long td = 0;
                // Update T and Td ( same as rtcp_interval() function in rfc.

                int members = GetNumberOfMembers();
                int senders = GetNumberOfActiveSenders();

                /*
                * Minimum average time between RTCP packets from this site (in
                * seconds). This time prevents the reports from `clumping' when
                * sessions are small and the law of large numbers isn't helping
                * to smooth out the traffic. It also keeps the report interval
                * from becoming ridiculously small during transient outages like
                * a network partition.
                */
                final long RTCP_MIN_TIME = (long) 5.;
                /*
                * Fraction of the RTCP bandwidth to be shared among active
                * senders. (This fraction was chosen so that in a typical
                * session with one or two active senders, the computed report
                * time would be roughly equal to the minimum report time so that
                * we don't unnecessarily slow down receiver reports.) The
                * receiver fraction must be 1 � the sender fraction.
                */
                final double RTCP_SENDER_BW_FRACTION = (double) 0.25;
                final double RTCP_RCVR_BW_FRACTION = (1-RTCP_SENDER_BW_FRACTION);
                double t; /* interval */
                double rtcp_min_time = RTCP_MIN_TIME;
                double n; /* no. of members for computation */
                /*
                * Very first call at application start�up uses half the min
                * delay for quicker notification while still allowing some time
                * before reporting for randomization and to learn about other
                * sources so the report interval will converge to the correct
                * interval more quickly. */
                if (initial)
                {
                    rtcp_min_time /= 2;
                }
                /*
                * If there were active senders, give them at least a minimum
                * share of the RTCP bandwidth. Otherwise all participants share
                * the RTCP bandwidth equally.
                */

                n = members;

                if (senders > 0 && senders < members * RTCP_SENDER_BW_FRACTION)
                {
                    if ( GetMySource().ActiveSender )
                    {
                        rtcp_bw *= RTCP_SENDER_BW_FRACTION;
                        n = senders;
                    }
                    else
                    {
                        rtcp_bw *= RTCP_RCVR_BW_FRACTION;
                        n -= senders;
                    }
                }

                /*
                * The effective number of sites times the average packet size is
                * the total number of octets sent when each site sends a report.
                * Dividing this by the effective bandwidth gives the time
                * interval over which those packets must be sent in order to
                * meet the bandwidth target, with a minimum enforced. In that
                * time interval we send one report so this time is also our
                * average time between reports.
                */
                t = (double) avg_rtcp_size  *n / rtcp_bw;
                if (t < rtcp_min_time) t = rtcp_min_time;
                /*
                * To avoid traffic bursts from unintended synchronization with
                * other sites, we then pick our actual next report interval as a
                * random number uniformly distributed between 0.5*t and 1.5*t.
                */
                double noise = (rnd.nextDouble() + 0.5);

                Session.Td = t;
                Session.T = t * (double) noise;
                return t;
            }

            /**
            *   Initialize the Session level variables.
            *
            */
            public int Initialize()
            {
                AppStartupTime = CurrentTime();
                TimeOfLastRTCPSent =  AppStartupTime ;
                tc = CurrentTime();
                pmembers = 1;
                we_sent = true;
                rtcp_bw = (double) 0.05 * (double) Session.BANDWIDTH;
                initial = true;
                avg_pkt_sz = 0; // TODO: Set the the size of the first packet generated by app
                SSRC = (long) Math.abs( rnd.nextInt() ) ;

                PacketCount = 0;
                OctetCount = 0;


                // Set the next transmission time to the interval
                tn = T;

                // Add self as a source object into the SSRC table maintained by the session
              Session.AddSource ( Session.SSRC, new Source( Session.SSRC) );

                return (0);
            }

            /**
            *   Returns a self source object.
            *
            *   @return My source object.
            */
            public static synchronized Source GetMySource()
            {
                Source s = (Source) SourceMap.get ( new Long( SSRC ) );
                return s;

            }

            /**
            *   Adds an SSRC into the table, if SSRC Exists, error code -1
            *   is returned.
            *
            *   @param  newSSRC SSRC of the source being added.
            *   @param  src     Source object of the source being added.
            *   @return -1 if the source with this SSRC already exists, 1 otherwise (source
            *           added).
            */

            public static int AddSource ( long newSSRC , Source src)
            {

                if ( SourceMap.containsKey ( new Long ( newSSRC) ) )
                {
                    return -1;
                }
                else
                {
                  SourceMap.put ( new Long ( newSSRC), src );
                  Session.outprintln("Adding Source : " + "SSRC = 0x" + Integer.toHexString((int)newSSRC));
                  Session.outprintln("No. of members" + GetNumberOfMembers ());
                  Session.outprintln("No. of senders" + GetNumberOfActiveSenders ());

                }
                return 1;
            }

            /**
            *   Returns all active senders as an iterable enumeration.
            *
            *   @return Enumeration of all active senders.
            */
            public static synchronized Enumeration GetActiveSenders ()
            {
                Enumeration EnumAllMembers = SourceMap.elements();

                Vector VectActiveSenders = new Vector();

                Source s;

                // Go through this enumeration and for each source
                // if it is and active sender, add into a temp vector
                while ( EnumAllMembers.hasMoreElements() )
                {
                    s = (Source) EnumAllMembers.nextElement();
                    if ( s.ActiveSender )
                        VectActiveSenders.addElement ( s );
                }

                // Return the enumeration of the temp vector.
                return ( VectActiveSenders.elements() );
            }

            /**
            *   Return an iterable enumeration of all sources
            *   contained in the Map.
            *
            *   @return Enumeration of all the sources (members).
            */
            public static synchronized Enumeration GetSources ()
            {
                return SourceMap.elements();
            }

            /**
            *   Returns current time from the Date().getTime() function.
            *
            *   @return The current time.
            */
            public static long CurrentTime()
            {
                tc = (new Date()).getTime();
                return (long)tc;
            }

            /**
            *   Function removes all sources from the members table (except self).
            *   Returns number of sources removed.
            *
            *   @return Number of sources successfully removed.
            */
            public static synchronized int RemoveAllSources()
            {
                Enumeration SourceCollection  = GetSources();

                int n = 0;

                while ( SourceCollection.hasMoreElements() )
                {
                    Source s = (Source) SourceCollection.nextElement();

                    if ( s.SSRC != SSRC )
                    {
                        RemoveSource ( s.SSRC );
                        n++;
                    }
                }

                pmembers = 1;
                CalculateInterval();

                return (n);
            }

          /**
          *   Register RTCP action listener.
          *   The instantiators of the session who are interested
          *   in getting RTCP information, may wish to implement
          *   the RTCP_actionListener interface and then call
          *   this function to register themselves so that the
          *   incoming RTCP information can be handed over to them.
          *
          *   @param listener who implements the RTCP_actionListener interface and
          *           will be the one to which all RTCP actions will be posted.
          */
          public static synchronized void addRTCP_actionListener ( RTCP_actionListener listener )
          {
              m_RTCP_actionListener = listener;
          }

          /**
          *   Register RTP action listener.
          *   The instantiators of the session must implement
          *   RTP_actionListener and must register with this function
          *   only once.
          *
          *   @param listener who implements the RTP_actionListener interface and
          *           will be the one to which all RTP actions will be posted.
          */
          public static synchronized void addRTP_actionListener ( RTP_actionListener listener )
          {
              m_RTP_actionListener = listener;
          }

          /**
          *   Post the RTCP RR packet to the actionListener. (if any
          *   is registered)
          *
          *   @param    RRpkt The Receiver Report packet received.
          */
          public static synchronized void postAction ( RTCPReceiverReportPacket RRpkt )
          {
              if ( m_RTCP_actionListener != null )
                  m_RTCP_actionListener.handleRTCPEvent ( RRpkt );
          }

          /**
          *   Function posts the RTCP SR packet to the actionListener. (if any
          *   is registered)
          *
          *   @param SRpkt The sender report packet received.
          */
          public static synchronized void postAction ( RTCPSenderReportPacket SRpkt )
          {
              if ( m_RTCP_actionListener != null )
                  m_RTCP_actionListener.handleRTCPEvent ( SRpkt );

          }

          /**
          *   Function posts the SDES packet to the actionListener. (if any
          *   is registered)
          *
          *   @param sdespkt The SDES packet received.
          */
          public static synchronized void postAction ( RTCPSDESPacket sdespkt )
          {
              if ( m_RTCP_actionListener != null )
                  m_RTCP_actionListener.handleRTCPEvent ( sdespkt );
          }

          /**
          *   Function posts the RTCP BYE packet to the actionListener. (if any
          *   is registered)
          *
          *   @param byepkt The BYE packet received.
          */
          public static synchronized void postAction ( RTCPBYEPacket byepkt )
          {
              if ( m_RTCP_actionListener != null )
                  m_RTCP_actionListener.handleRTCPEvent ( byepkt );
          }

          /**
          *   Function posts the RTP packet to the actionListener. (if any
          *   is registered).
          *
          *   @param rtppkt The RTP Packet received.
          */
          public static synchronized void postAction ( RTPPacket rtppkt )
          {
              if ( m_RTP_actionListener != null )
                  m_RTP_actionListener.handleRTPEvent ( rtppkt );
              else
                  System.err.println ("ERROR: No RTP Action Listener registered :(");
          }

          /**
          *     Print a newline. Provided to enable the
          *     debug console print messages in the source code.  By setting the DebugOutput flag,
          *     user can control the debug message output.
          *     @see Session#DebugOutput
          *     @see System.out.println
          */
          public static synchronized void outprintln () {
              if ( DebugOutput ) System.out.println ();
          }


        /*
        *     Print a string. Provided to enable the
        *     debug console print messages in the source code.  By setting the DebugOutput flag,
        *     user can control the debug message output.
        *     @see Session#DebugOutput
        *     @see System.out.println
        */
       public static synchronized void outprintln ( String s ) {


              if ( DebugOutput ) System.out.println ( s );
          }

        /*
        *     Print a string. Provided to enable the
        *     debug console print messages in the source code.  By setting the DebugOutput flag,
        *     user can control the debug message output.
        *     @see Session#DebugOutput
        *     @see System.out.print
        */
          public static synchronized void outprint ( String s ) {
              if ( DebugOutput ) System.out.print ( s );
          }



          /**
          *   Starts the Late Initialization Server Deamon.
          *
          *
          */
          public static synchronized void StartLateInitializationDeamon () {
            if ( DebugOutput ) LateInitSrvr.StartServerDeamon ( 9606 );
          }

          /**
          *   Starts the Late Initialization Client process.
          *
          *
          *   @param TCPServerAddress Dotted-decimal string IP address of the
          *                            LateInitilizationServer
          *   @param ServicePort    The port on the server which is serving the late
          *                         initialization client.
          */

          public static synchronized void StartLateInitializationClient
                            (   String TCPServerAddress,
                                int ServicePort
                             )
          {
            LateInitClient.StartClient ( TCPServerAddress, ServicePort);
          }


      }

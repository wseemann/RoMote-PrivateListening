package Websocket1;

import java.lang.*;
import java.net.*;
    /**
    *   This class creates and starts the RTCP sender and receiver threads
    *   
    */
public class RTCPThreadHandler extends java.lang.Object
{
    /**
    *   Reference to the RTCP Receiver Thread
    *   
    */
    private RTCPReceiverThread rtcpReceiverThread;
    
    /**
    *   Reference to the RTCP Sender Thread
    *   
    */
    private RTCPSenderThread rtcpSenderThread;

    /**
    *   Constructor creates the sender and receiver
    *   threads. (Does not start the threads)
    *
    *   @param  MulticastGroupIPAddress Dotted representation of the Multicast address. 
    *   @param  RTCPSendFromPort        Port used to send RTCP Packets.
    *   @param  RTCPGroupPort           Port for Multicast group (for receiving RTCP Packets).
    */
    public RTCPThreadHandler (  InetAddress MulticastGroupIPAddress, 
                                int RTCPSendFromPort,
                                int RTCPGroupPort
                             )
    {
        // create an rtcpSender thread
        rtcpSenderThread = new RTCPSenderThread ( MulticastGroupIPAddress, RTCPSendFromPort, RTCPGroupPort );

        // create an rtcpReceiver thread
        rtcpReceiverThread = new RTCPReceiverThread ( MulticastGroupIPAddress, RTCPGroupPort );
    }
 
    /**
    *   Starts the RTCP Sender thread.
    */
    public void startRTCPSenderThread()
    {
        rtcpSenderThread.start();
    }

    /**
    *   Starts the RTCP Receiver thread.
    */
    public void startRTCPReceiverThread()
    {
        rtcpReceiverThread.start();
    }

    /**
    *   Interrupts a running RTCP sender thread.  This will
    *   cause the sender to send BYE packet and finally terminate.
    */
    public void stopRTCPSenderThread()
    {
        rtcpSenderThread.interrupt();
    }
}
package Websocket1;
/**
*    This class encapsulates all the necessary parameters of a
*    RTCP Packet that needs to be handed to the Application 
*    when a RTCP Packet is received.
*/
public class RTCPPacket
{
    /**
    *   The synchronization source identifier for the originator of this
    *   RTCP packet. This class represents the least common denominator
    *   in an RTCP Packet. It is equivalent to an abstract base class from
    *   which RTCPSenderReportPacket and RTCPReceiverReportPacket derive
    *   from.
    */
    
    public long SenderSSRC;
    
    /**
    *   Reception Report Block contained in this packet (if any)
    *
    */
    
    public ReportBlock ReportBlock;
    
    /*
    *   Indicates whether this RTCP Packet contains a Reception.
    *   Report Block
    */
    
    public boolean containsReportBlock;
}
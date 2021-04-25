package Websocket1;

/**
*    This class encapsulates all the necessary parameters of a
*    Sender Report that needs to be handed to the Application 
*    when a Sender Report is received.
*    Note: this class derives from RTCPPacket
*/
public class RTCPSenderReportPacket extends RTCPPacket
{
    /*
    * The Sender Info Block Contained in this Sender Report
    *
    */
    public SenderInfo SenderInfo;
}
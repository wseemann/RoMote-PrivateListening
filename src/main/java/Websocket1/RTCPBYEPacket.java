package Websocket1;

import java.lang.*;
/**
*    This class encapsulates all the necessary parameters of a
*    BYE Packet that needs to be handed to the Application 
*    when a BYE Packet is received.
*/
public class RTCPBYEPacket
{
    /**
    *   SSRC of the source that sent a BYE Packet.
    */
    public long SSRC;
    
    /**
    *   Reason for Leaving. 
    */
    public String ReasonForLeaving;
}
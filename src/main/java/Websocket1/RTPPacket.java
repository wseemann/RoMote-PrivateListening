package Websocket1;

import java.lang.*;

/**
*    This class encapsulates all the necessary parameters of a
*    RTP Packet that needs to be handed to the Application 
*    when a RTP Packet is received.
*/

public class RTPPacket
{
    /**
    *   The CSRC count contains the number of CSRC identifiers that
    *   follow the fixed header.
    */
    public long CSRCCount;
    
    /**
    *   The sequence number increments by one for each RTP data packet
    *   sent, and may be used by the receiver to detect packet loss and
    *   to restore packet sequence. The initial value of the sequence
    *   number is random (unpredictable) to make known-plaintext attacks
    *   on encryption more difficult, even if the source itself does not
    *   encrypt, because the packets may flow through a translator that
    *   does.
    */
    public long SequenceNumber;
    
    /**
    *   The timestamp reflects the sampling instant of the first octet
    *   in the RTP data packet. 
    */
    public long TimeStamp;
    
    /**
    *  The SSRC field identifies the synchronization source. This
    *  identifier is chosen randomly, with the intent that no two
    *  synchronization sources within the same RTP session will have
    *  the same SSRC identifier. 
    */
    public long SSRC;
    
    /**
    *   The actual payload contained in a RTP Packet
    */
    public byte data[];
}
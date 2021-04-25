package Websocket1;

import java.lang.*;

/**
*    This class encapsulates all the necessary parameters of a
*    Sender Report that needs to be handed to the Application 
*    when a Sender Report is received
*/
    
public class SenderInfo
{
    /**
    *   Indicates the wallclock time when this report was sent so that
    *   it may be used in combination with timestamps returned in
    *   reception reports from other receivers to measure round-trip
    *   propagation to those receivers. 
    *   NTPTimeStampMostSignificant -   Most significant 32 bits
    *                                   of the NTP Time stamp 
    */
    public long NTPTimeStampMostSignificant;
    
    /**
    *   Least significant 32 bits of the NTP Time stamp 
    */
    public long NTPTimeStampLeastSignificant;
    
    /**
    *   Corresponds to the same time as the NTP timestamp (above), but
    *   in the same units and with the same random offset as the RTP
    *   timestamps in data packets. This correspondence may be used for
    *   intra- and inter-media synchronization for sources whose NTP
    *   timestamps are synchronized, and may be used by media-
    *   independent receivers to estimate the nominal RTP clock
    *   frequency.
    */
    public long RTPTimeStamp;
    
    /**
    *   The total number of RTP data packets transmitted by the sender
    *   since starting transmission up until the time this SR packet was
    *   generated.  The count is reset if the sender changes its SSRC
    *   identifier.
    */
    
    public long SenderPacketCount;
    
    /**
    *   The total number of payload octets (i.e., not including header
    *   or padding) transmitted in RTP data packets by the sender since
    *   starting transmission up until the time this SR packet was
    *   generated. The count is reset if the sender changes its SSRC
    *   identifier. This field can be used to estimate the average
    *   payload data rate.
    *   
    */
    
    public long SenderOctetCount;
           
}
package Websocket1;

import java.lang.*;
/**
*    This class encapsulates all the necessary parameters of a
*    Reception Report that needs to be handed to the Application 
*    when a Reception Report is received.
*/
public class ReportBlock
{
    /**
    *   The fraction of RTP data packets from source SSRC_n lost since
    *   the previous SR or RR packet was sent, expressed as a fixed
    *   point number with the binary point at the left edge of the
    *   field. (That is equivalent to taking the integer part after
    *   multiplying the loss fraction by 256.) This fraction is defined
    *   to be the number of packets lost divided by the number of
    *   packets expected. 
    *
    */
    
    public double FractionLost ;
    
    /**
    *   The total number of RTP data packets from source SSRC_n that
    *   have been lost since the beginning of reception. This number is
    *   defined to be the number of packets expected less the number of
    *   packets actually received, where the number of packets received
    *   includes any which are late or duplicates. Thus packets that
    *   arrive late are not counted as lost, and the loss may be
    *   negative if there are duplicates.  The number of packets
    *   expected is defined to be the extended last sequence number
    *   received less the initial sequence number received.
    *
    */
    public long CumulativeNumberOfPacketsLost;
    
    /**
    *   The low 16 bits contain the highest sequence number received in
    *   an RTP data packet from source SSRC_n, and the most significant
    *   16 bits extend that sequence number with the corresponding count
    *   of sequence number cycles
    */
    
    public long ExtendedHighestSequenceNumberReceived;
    
    /**
    *   An estimate of the statistical variance of the RTP data packet
    *   interarrival time, measured in timestamp units and expressed as
    *   an unsigned integer. The interarrival jitter J is defined to be
    *   the mean deviation (smoothed absolute value) of the difference D
    *   in packet spacing at the receiver compared to the sender for a
    *    pair of packets. 
    */
    
    public double InterarrivalJitter;
    
    /**
    *   The middle 32 bits out of 64 in the NTP timestamp 
    *   received as part of the most recent RTCP sender
    *   report (SR) packet from source SSRC_n.  If no SR has been
    *   received yet, the field is set to zero.
    */
    
    public double LastSR;
    
    /**
    *   The delay, expressed in units of 1/65536 seconds, between
    *   receiving the last SR packet from source SSRC_n and sending this
    *   reception report block.  If no SR packet has been received yet
    *   from SSRC_n, the DLSR field is set to zero.
    */
    public long Delay_LastSR;
}
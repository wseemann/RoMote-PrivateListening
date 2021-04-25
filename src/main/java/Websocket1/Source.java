package Websocket1;

import java.lang.*;

/**
*   This class encapsulates all the per source state information. Every source keeps
*   track of all the other sources in the multicast group, from which it has received a
*   RTP or RTCP Packet. It is necessry to keep track of per state source information
*   in order to provide effective reception quality feedback to all the sources that
*   are in the multicast group. 
*/

class Source extends Object
{
      
    /**
    *   Constructor requires an SSRC for it to be a valid source. The constructor initializes
    *   all the source class members to a default value
    *
    *   @param   sourceSSRC SSRC of the new source
    */
    
    Source (long sourceSSRC)
    {
        long time = Session.CurrentTime();
        SSRC = sourceSSRC;
        fraction =0;
        lost =0;
        last_seq=0;
        jitter =0;
        lst =0;
        dlsr=0;
        ActiveSender =false;
        TimeOfLastRTCPArrival= time;
        TimeOfLastRTPArrival =  time;
        TimeofLastSRRcvd=time;
        NoOfRTPPacketsRcvd=0;
        base_seq=0;
        expected_prior = 0;
        received_prior = 0;
    }
    
    /**
    *   source SSRC uint 32.
    */

    public long SSRC;   // unsigned 32 bits
    
    /**
    *   Fraction of RTP data packets from source SSRC lost since the previous
    *   SR or RR packet was sent, expressed as a fixed point number with the
    *   binary point at the left edge of the field.  To get the actual fraction
    *   multiply by 256 and take the integral part
    */
    public double fraction; // 8 bits
    
    /**
    *   Cumulative number of packets lost (signed 24bits).
    *
    */
    public long lost; // signed 24 bits
    
    /**
    *   extended highest sequence number received.
    */
    public long last_seq; // unsigned 32 bits
    
    /**
    *   Interarrival jitter.
    */
    public long jitter; // unsigned 32 bits
    
    /**
    *   Last SR Packet from this source.
    */
    public long lst;    // unsigned 32 bits
    
    /**
    *   Delay since last SR packet.
    */
    public double dlsr;
    
    /**
    *   Is this source and ActiveSender.
    */
    public boolean ActiveSender;
    
    /**
    *   Time the last RTCP Packet was received from this source.
    */
    public double TimeOfLastRTCPArrival;
    
    /**
    *   Time the last RTP Packet was received from this source.
    */
    public double TimeOfLastRTPArrival;

    /**
    *   Time the last Sender Report RTCP Packet was received from this source.
    */
    public double TimeofLastSRRcvd;
    
    /**
    *   Total Number of RTP Packets Received from this source
    */
    public int NoOfRTPPacketsRcvd;
    
    /**
    *   Sequence Number of the first RTP packet received from this source
    */
    public long base_seq;
    
    /**
    *   Number of RTP Packets Expected from this source 
    */
    public long expected;
    
    /**
    *   No of  RTP Packets expected last time a Reception Report was sent 
    */
    public long expected_prior;
    
    /**
    *   No of  RTP Packets received last time a Reception Report was sent
    */
    public long received_prior;    
	
	/**
    *   Highest Sequence number received from this source
    */
	public long max_seq;
	
	/**
    *   Keep track of the wrapping around of RTP sequence numbers, since RTP Seq No. are 
    *   only 16 bits
    */
	public long cycles;
	
	/**
    *  Since Packets lost is a 24 bit number, it should be clamped at WRAPMAX = 0xFFFFFFFF
    */
	public long WRAPMAX = 0xFFFFFFFF;

    /**
    *   Returns the extended maximum sequence for a source
    *   considering that sequences cycle.
    *
    *   @return  Sequence Number 
    *
    */
    
    public long getExtendedMax( )
    {
        return ( cycles + max_seq );
    }
    
    /**
    *   This safe sequence update function will try to 
    *   determine if seq has wrapped over resulting in a
    *   new cycle.  It sets the cycle -- source level 
    *   variable which keeps track of wraparounds.
    *
    *   @param seq  Sequence Number
    *
    */
   
    public void updateSeq( long seq )
    {
        // If the diferrence between max_seq and seq 
        // is more than 1000, then we can assume that 
        // cycle has wrapped around.
        if ( max_seq == 0 )
            max_seq = seq;
        else
        {
            if ( max_seq - seq  > 0.5*WRAPMAX )
                cycles += WRAPMAX;

        max_seq =  seq;
        }
        
    }
 
    
    /**
    *   Updates the various statistics for this source e.g. Packets Lost, Fraction lost
    *   Delay since last SR etc, according to the data gathered since a last SR or RR was sent out. 
    *   This method is called prior to sending a Sender Report(SR)or a Receiver Report(RR) 
    *   which will include a Reception Report block about this source.
    *   
    */
    
    public int UpdateStatistics()
    {
        //Set all the relevant parameters
                            
        // Calculate the highest sequence number received in an RTP Data Packet from this source
        last_seq = getExtendedMax();
        
        // Number of Packets lost = Number of Packets expected - Number of Packets actually rcvd
        expected = getExtendedMax() - base_seq +1;
        lost  = expected - NoOfRTPPacketsRcvd;
        
        // Clamping at 0xffffff
        if (lost > 0xffffff) 
            lost = 0xffffff;
        
        // Calculate the fraction lost
        long expected_interval = expected - expected_prior;
        expected_prior = expected;
        
        long received_interval = NoOfRTPPacketsRcvd - received_prior;
        received_prior = NoOfRTPPacketsRcvd;
        
        long lost_interval = expected_interval - received_interval;
        
        if(expected_interval ==0 || lost_interval <=0) 
            fraction =0;
        else
            fraction = (lost_interval << 8) / expected_interval;
              
        //dlsr - express it in units of 1/65336 seconds
        dlsr = (TimeofLastSRRcvd - Session.CurrentTime())/ 65536;
              
        return 0;
    }
}

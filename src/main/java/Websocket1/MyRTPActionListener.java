package Websocket1;

public class MyRTPActionListener implements RTP_actionListener, RTCP_actionListener {

	@Override
	public void handleRTPEvent(RTPPacket pkt) {
		System.out.println("Got RTP shit!");		
	}

	@Override
	public void handleRTCPEvent(RTCPReceiverReportPacket RRpkt) {
		System.out.println("Got RTCP shit!");	
	}

	@Override
	public void handleRTCPEvent(RTCPSenderReportPacket SRpkt) {
		System.out.println("Got RTCP shit!");	
	}

	@Override
	public void handleRTCPEvent(RTCPSDESPacket sdespkt) {
		System.out.println("Got RTCP shit!");	
	}

	@Override
	public void handleRTCPEvent(RTCPBYEPacket byepkt) {
		System.out.println("Got RTCP shit!");	
	}
}

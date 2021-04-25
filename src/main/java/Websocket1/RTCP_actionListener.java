package Websocket1;

public interface RTCP_actionListener
{
    public void handleRTCPEvent ( RTCPReceiverReportPacket RRpkt);
    public void handleRTCPEvent ( RTCPSenderReportPacket SRpkt);
    public void handleRTCPEvent ( RTCPSDESPacket sdespkt);
    public void handleRTCPEvent ( RTCPBYEPacket byepkt);
}
package Websocket1;

import java.lang.*;


/*
*   This class provides constants associated with RTCP Packets
*/

public class RTCPConstants extends Object
{

    /**
    *   Version =2
    */
    public static final byte VERSION    =   2;
    
    /**
    *   Padding =0
    */
    public static final byte PADDING    =   0;
   

    /**
    *                   RTCP TYPES
    */

	
	
	public static final int RTCP_SR    = 	(int) 200;
	public static final int RTCP_RR    = 	(int) 201;
	public static final int RTCP_SDES  =	(int) 202;
	public static final int RTCP_BYE   = 	(int) 203;
	public static final int RTCP_APP   = 	(int) 204;
	
	

    /**
    *                   SDES TYPES
    */

	
	public static final byte RTCP_SDES_END    = (byte) 0;
	public static final byte RTCP_SDES_CNAME  =	(byte) 1;
	public static final byte RTCP_SDES_NAME   =	(byte) 2;
	public static final byte RTCP_SDES_EMAIL  =	(byte) 3;
	public static final byte RTCP_SDES_PHONE  =	(byte) 4;
	public static final byte RTCP_SDES_LOC    =	(byte) 5;
	public static final byte RTCP_SDES_TOOL   =	(byte) 6;
	public static final byte RTCP_SDES_NOTE   =	(byte) 7;
	public static final byte RTCP_SDES_PRIV   =	(byte) 8;

}
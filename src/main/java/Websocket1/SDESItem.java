package Websocket1;

import java.lang.*;
/**
*    This class encapsulates all the necessary parameters of a
*    SDES Item that needs to be handed to the Application 
*    when a SDES Packet is received.
*/
public class SDESItem
{
    /**
    *   Type of SDES Item e.g. CNAME, EMAIL etc.
    */
    public int Type;
    
    /**
    *   Value of the SDES Item 
    */
    public String Value;
}
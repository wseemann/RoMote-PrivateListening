package Websocket1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class InetAddressFactory {

  // Use a byte array like {199, 1, 32, 90} to build
  // an InetAddressObject   
  public static InetAddress newInetAddress(byte addr[]) 
    throws UnknownHostException  {
 
     try {
       InetAddress ia = InetAddress.getByAddress(addr);
       //ia.address  = addr[3] & 0xFF;
       //ia.address |= ((addr[2] << 8) & 0xFF00);
       //ia.address |= ((addr[1] << 16) & 0xFF0000);
       //ia.address |= ((addr[0] << 24) & 0xFF000000);
       return ia;
     }
     catch (Exception e) {  // primarily ArrayIndexOutOfBoundsExceptions
       throw new UnknownHostException(e.toString());
     }
   
 
  } // end newInetAddress
  
  
  // Use a String like 199.1.32.90 to build
  // an InetAddressObject
  public static InetAddress newInetAddress(String s) 
   throws UnknownHostException {
   
     // be ready for IPv6
     int num_bytes_in_an_IP_address = 4;
     byte addr[] = new byte[num_bytes_in_an_IP_address];
     StringTokenizer st = new StringTokenizer(s, ".");
     
     // make sure the format is correct
     if (st.countTokens() != addr.length) {
       throw new UnknownHostException(s 
        + " is not a valid numeric IP address");
     }
 
     for (int i = 0; i < addr.length; i++) {
       int thisByte = Integer.parseInt(st.nextToken());
       if (thisByte < 0 || thisByte > 255) {
              throw new UnknownHostException(s 
        + " is not a valid numeric IP address");
       }
       
       // check this
       if (thisByte > 127) thisByte -= 256;
       addr[i] = (byte) thisByte;
       
     }  // end for

     return newInetAddress(addr);
 
  } // end newInetAddress            
  
} // end InetAddressFactory 

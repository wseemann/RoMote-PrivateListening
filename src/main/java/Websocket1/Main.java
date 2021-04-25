package Websocket1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

public class Main {
	public static void main(String[] args) throws UnknownHostException {

		Session session = new Session("192.168.1.10", 6970, 5150, 6971, 5151, 10000);
		session.DebugOutput = true;
		session.setCName("poop");
		session.setEMail("wseemann@gmail.com");
		session.setPayloadType(97);
		session.addRTP_actionListener(new MyRTPActionListener());
		session.addRTCP_actionListener(new MyRTPActionListener());
		session.StartRTCPReceiverThread();
		session.StartRTCPSenderThread();
		session.StartRTPReceiverThread();

		Scanner s = new Scanner(System.in);
		System.out.println("Press enter to continue.....");
		s.nextLine();
		session.StopRTCPSenderThread();
		//server.close();
		System.exit(0);
	}

	public static class EchoServer extends Thread {

		private DatagramSocket socket;
		private boolean running;
		private byte[] buf = new byte[1000];

		File file = new File("/Users/wseemann/Desktop/rtcp.pcap");
		OutputStream os = null;

		public EchoServer() {
			try {
				socket = new DatagramSocket(6970);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			running = true;

			try {
				os = new FileOutputStream(file);

				while (running) {
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					System.out.println("Waiting for data...");
					socket.receive(packet);

					InetAddress address = packet.getAddress();
					int port = packet.getPort();
					packet = new DatagramPacket(buf, buf.length, address, port);
					String received = new String(packet.getData(), 0, packet.getLength());

					System.out.println("port: " + port);
					System.out.println("address: " + address);
					System.out.print("Packet: " + received);
					byte[] bytes = packet.getData();

					// Create a string from the byte array with "UTF-8" encoding
					String string = new String(bytes, StandardCharsets.UTF_8);
					os.write(bytes);

					//if (received.equals("end")) {
						running = false;
						continue;
					//}
					//socket.send(packet);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				close();
			}
		}

		public void close() {
			if (socket != null || !socket.isClosed()) {
				socket.close();
			}

			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

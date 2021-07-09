package my_protocol;

import framework.IRDTProtocol;
import framework.Utils;

import java.util.Arrays;

/**
 * @version 10-07-2019
 *
 *          Copyright University of Twente, 2013-2019
 *
 **************************************************************************
 *          Copyright notice * * This file may ONLY be distributed UNMODIFIED. *
 *          In particular, a correct solution to the challenge must NOT be
 *          posted * in public places, to preserve the learning effect for
 *          future students. *
 **************************************************************************
 */
public class MyProtocol extends IRDTProtocol {

	// change the following as you wish:
	static final int HEADERSIZE = 1; // number of header bytes in each packet
	static final int DATASIZE = 100; // max. number of user data bytes in each packet

	boolean ACKReceived = false;

	int totalACK;
	
	int i;

	@Override
	public void sender() {
		System.out.println("Sending...");

		// read from the input file
		Integer[] fileContents = Utils.getFileContents(getFileID());

		if (fileContents.length % DATASIZE == 0) {
			totalACK = fileContents.length / DATASIZE;
		} else {
			totalACK = fileContents.length / DATASIZE + 1;
		}

		System.out.println(totalACK);

		// keep track of where we are in the data
		int filePointer = 0;
		
		i = 1;

		while (i <= totalACK) {
			
			ACKReceived = false;

			// create a new packet of appropriate size
			int datalen = Math.min(DATASIZE, fileContents.length - filePointer);

			Integer[] pkt = new Integer[HEADERSIZE + datalen];

			// write something random into the header byte
			pkt[0] = i;
			

			// copy databytes from the input file into data part of the packet, i.e., after
			// the header
			System.arraycopy(fileContents, filePointer, pkt, HEADERSIZE, datalen);

			// send the packet to the network layer

			getNetworkLayer().sendPacket(pkt);
			System.out.println("Sent one packet with header=" + pkt[0]);

			// schedule a timer for 1000 ms into the future, just to show how that works:
			framework.Utils.Timeout.SetTimeout(10000, this, pkt);

			filePointer = filePointer + datalen;

			// and loop and sleep; you may use this loop to check for incoming acks...

			while (!ACKReceived) {
				Integer[] packet = getNetworkLayer().receivePacket();
				if (packet != null) {
					System.out.println("ACK received" + packet[0]);
					
					ACKReceived = true;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					ACKReceived = true;
				}
			}
			
			i = i+1;
		}
	}	
	
	@Override
	public void TimeoutElapsed(Object tag) {
		Integer[] z = (Integer[]) tag;
		// handle expiration of the timeout:
		
		if (!ACKReceived && i == (int) z[0]){
			getNetworkLayer().sendPacket(z);
			framework.Utils.Timeout.SetTimeout(1000, this, z);
			System.out.println("Timer expired with tag=" + z[0]);
		}

	}

	@Override
	public Integer[] receiver() {
		System.out.println("Receiving...");

		// create the array that will contain the file contents
		// note: we don't know yet how large the file will be, so the easiest (but not
		// most efficient)
		// is to reallocate the array every time we find out there's more data
		Integer[] fileContents = new Integer[0];

		System.out.println("pkt receiver" + fileContents);
		
		int fileLength = 0;

		// loop until we are done receiving the file
		boolean stop = false;

		while (!stop) {

			// try to receive a packet from the network layer
			Integer[] packet = getNetworkLayer().receivePacket();

			// if we indeed received a packet
			if (packet != null) {

				Integer[] ACK = new Integer[1];
				ACK[0] = packet[0];
				getNetworkLayer().sendPacket(ACK);

				System.out.println("ACK sent with number " + ACK[0]);

				// tell the user
				System.out.println("Received packet, length=" + packet.length + "  first byte=" + packet[0]);

				// append the packet's data part (excluding the header) to the fileContents
				// array, first making it larger
				int oldlength = fileContents.length;

//                System.out.println("old length" + oldlength);

				int datalen = packet.length - HEADERSIZE;

//                System.out.println("datalen receiver" + datalen);

				fileContents = Arrays.copyOf(fileContents, oldlength + datalen);

//                System.out.println("oldlength+datalen " + oldlength+datalen);

				System.arraycopy(packet, HEADERSIZE, fileContents, oldlength, datalen);
				
				fileLength = fileLength  + datalen;
				
				// Sizes in bytes are: 248, 2085, 6267, 21067, 53228, 141270
				stop = fileLength >= 2085;

			} else {
				// wait ~10ms (or however long the OS makes us wait) before trying again
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					stop = true;
				}
			}
		}

		// return the output file
		return fileContents;
	}
}

// rtphello
// A very simple program for testing and illustrating basic features of ccRTP.
// Copyright (C) 2001,2002  Federico Montesino <fedemp@altern.org>
//  
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//  
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//  
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


// This is an introductory example file that illustrates basic usage
// of ccRTP. You will also see a bit on how to use other classes from
// CommonC++.

// I am a typical hello world program. I consist of a sender thread,
// that sends the salutation message on RTP packets; and a receiver
// thread, that prints the messages. This is a program with an unsual
// structure, the receiver just tries to process the first available
// packet periodically, and both are in the same program. Thus, it
// should not be seen as an example for typical applications but as a
// test of some functions of ccRTP.

#include <cstdio>
#include <ctime>
// In order to use ccRTP, the RTP stack of CommonC++, just include...
#include <ccrtp/rtp.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

// base ports
const int RECEIVER_BASE = 33634;
const int TRANSMITTER_BASE = 32522;

// For this example, this is irrelevant. 
//const int TIMESTAMP_RATE = 90000;

/**
 * @class ccRTP_Hello_Rx
 * Receiver of salutes.
 */
class ccRTP_Hello_Rx: public Thread
{

private:
	// socket to receive packets
	RTPSession *socket;
	// loopback network address
	InetHostAddress local_ip;
	// identifier of this sender
	uint32 ssrc;
	
public:
	ccRTP_Hello_Rx(){
		// Before using ccRTP you should learn something about other
		// CommonC++ classes. We need InetHostAddress...

		// Construct loopback address
		local_ip = "127.0.0.1";

		// Is that correct?
		if( ! local_ip ){  
			// this is equivalent to `! local_ip.isInetAddress()'
			cerr << "Rx: IP address is not correct!" << endl;
			exit();
		}

		// create socket for RTP connection and get a random
		// SSRC identifier
		socket = new RTPSession(local_ip,RECEIVER_BASE);
		ssrc = socket->getLocalSSRC();
	}
	
	~ccRTP_Hello_Rx(){
		cout << endl << "Destroying receiver -ID: " << hex
		     << (int)ssrc;
		terminate();
		delete socket;
		cout << "... " << "destroyed.";
	}

	// This method does almost everything.
	void run(void){    

		cout << "Hello, " << defaultApplication().
			getSDESItem(SDESItemTypeCNAME)
		     << " ..." << endl;
		// redefined from Thread.
		// Set up connection
		socket->setSchedulingTimeout(20000);
		socket->setExpireTimeout(3000000);
		//socket->UDPTransmit::setTypeOfService(SOCKET_IPTOS_LOWDELAY);
		if( !socket->addDestination(local_ip,TRANSMITTER_BASE) )
			cerr << "Rx (" << hex << (int)ssrc 
			     << "): could not connect to port." 
			     << TRANSMITTER_BASE;
		
		cout << "Rx (" << hex << (int)ssrc
		     << "): " << local_ip.getHostname() 
		     <<	" is waiting for salutes in port "
		     << RECEIVER_BASE << "..." << endl;
		
		socket->setPayloadFormat(StaticPayloadFormat(sptMP2T));
		socket->startRunning();
		// Let's check the queues  (you should read the documentation
		// so that you know what the queues are for).
		cout << "Rx (" << hex << (int)ssrc 
		     << "): The queue is " 
		     << ( socket->isActive() ? "" : "in") 
		     << "active." << endl;		

		// This is the main loop, where packets are received.
		for( int i = 0 ; true ; i++ ){
			
			// Wait for an RTP packet.
			const AppDataUnit *adu = NULL;
			while ( NULL == adu ) {
				Thread::sleep(10);
				adu = socket->getData(socket->getFirstTimestamp());
			}
			
			// Print content (likely a salute :))
			// Note we are sure the data is an asciiz string.
			time_t receiving_time = time(NULL);
			char tmstring[30];
			strftime(tmstring,30,"%X",localtime(&receiving_time));
			cout << "Rx (" << hex << (int)ssrc 
			     << "): [receiving at " << tmstring << "]: " 
			     <<	adu->getData() << endl;
			delete adu;
		}
	}
};

/**
 * @class ccRTP_Hello_Tx
 * Transmitter of salutes.
 */
class ccRTP_Hello_Tx: public Thread, public TimerPort
{

private:
	// socket to transmit
	RTPSession *socket;
	// loopback network address
	InetHostAddress local_ip;
	// identifier of this sender
	uint32 ssrc;

public:
	ccRTP_Hello_Tx(){
		// Before using ccRTP you should learn something about other
		// CommonC++ classes. We need InetHostAddress...

		// Construct loopback address
		local_ip = "127.0.0.1";
		
		// Is that correct?
		if( ! local_ip ){  
		// this is equivalent to `! local_ip.isInetAddress()'
			cerr << "Tx: IP address is not correct!" << endl;
			exit();
		}
		
		socket = new RTPSession(local_ip,TRANSMITTER_BASE);
		ssrc = socket->getLocalSSRC();
	}

	~ccRTP_Hello_Tx(){
		cout << endl << "Destroying transmitter -ID: " << hex 
		     << (int)ssrc;
		terminate();
		delete socket;
		cout << "... " << "destroyed.";
	}

	// This method does almost everything.
	void run(void){    
		// redefined from Thread.
		cout << "Tx (" << hex << (int)ssrc << "): " << 
			local_ip.getHostname() 
		     <<	" is going to salute perself through " 
		     << local_ip << "..." << endl;
		
		// Set up connection
		socket->setSchedulingTimeout(20000);
		socket->setExpireTimeout(3000000);
		if( !socket->addDestination(local_ip,RECEIVER_BASE) )
			cerr << "Tx (" << hex << (int)ssrc 
			     << "): could not connect to port." 
			     << RECEIVER_BASE;
		
		cout << "Tx (" << hex << (int)ssrc << 
			"): Transmitting salutes to port "
		     << RECEIVER_BASE << "..." << endl;

		uint32 timestamp = 0;
		// This will be useful for periodic execution
		TimerPort::setTimer(1000);

		// This is the main loop, where packets are sent.
		socket->setPayloadFormat(StaticPayloadFormat(sptMP2T));
		socket->startRunning();
		// Let's check the queues  (you should read the documentation
		// so that you know what the queues are for).
		cout << "Tx (" << hex << (int)ssrc << "): The queue is "
		     << ( socket->isActive()? "" : "in")
		     << "active." << endl;

		for( int i = 0 ; true ;i++ ){

			// send RTP packets, providing timestamp,
			// payload type and payload.  
			// construct salute.
			unsigned char salute[50];
			snprintf((char *)salute,50,
				 "Hello, brave gnu world (#%u)!",i);
			time_t sending_time = time(NULL);
			// get timestamp to send salute
			if ( 0 == i ){
				timestamp = socket->getCurrentTimestamp();
				
			} else {
				// increment for 1 second
				timestamp += socket->getCurrentRTPClockRate();
			}	

			socket->putData(timestamp,salute,
					strlen((char *)salute)+1);
			// print info
			char tmstring[30];
			strftime(tmstring,30,"%X",
				 localtime(&sending_time));
			cout << "Tx (" << hex << (int)ssrc 
			     << "): sending salute " << "no " << dec << i 
			     << ", at " << tmstring 
			     << "..." << endl;

			// Let's wait for the next cycle
			Thread::sleep(TimerPort::getTimer());
			TimerPort::incTimer(1000);
		}
	}
};

int main(int argc, char *argv[])
{

	// Construct the two main threads. they will not run yet.
	ccRTP_Hello_Rx *receiver = new ccRTP_Hello_Rx;
	ccRTP_Hello_Tx *transmitter = new ccRTP_Hello_Tx;
	
	cout << "This is rtphello, a very simple test program for ccRTP." << 
		endl << "Strike [Enter] when you are fed up with it." << endl;

	// Start execution of hello now.
	receiver->start();
	transmitter->start();

	cin.get();

	delete transmitter;
	delete receiver;

	cout << endl << "That's all." << endl;
	
	return 0;
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 8
 * End:
 */

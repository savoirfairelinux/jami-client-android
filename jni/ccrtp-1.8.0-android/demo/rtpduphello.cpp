// rtpduphello.
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
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


// This is an introductory example file that illustrates basic usage
// of ccRTP. You will also see a bit on how to use CommonC++ threads.

// It is a typical hello world program. It consists of tow duplex
// connections that talk each other through RTP packets. They do not
// say more than a typical salutation message. They both send and
// receive messages, and print the messages they receive.


#include <cstdio>
#include <cstdlib>
// In order to use ccRTP, the RTP stack of CommonC++, you only need to
// include ...
#include <ccrtp/ext.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

/**
 * @class ccRTP_dupHello
 * This is the class that will do almost everything.
 */
class ccRTP_dupHello: public Thread
{
private:
    // There will be two duplex connections. They both will send
    // and receive packets.
    RTPDuplex *duplexA, *duplexB;

public:
    // Destructor.
    ~ccRTP_dupHello()
    {
        terminate();
        delete duplexA;
        delete duplexB;
    }

    // Constructor.
    ccRTP_dupHello() : duplexA(NULL), duplexB(NULL)
        {}

    // This method does almost everything.
    void run(void)
    {
        // redefined from Thread.

        // Before using ccRTP you should learn something about other
        // CommonC++ classes. We need InetHostAddress...

        // Construct loopback address
        InetHostAddress local_ip;
        local_ip = "127.0.0.1";

        // Is that correct?
        if( ! local_ip ) {
        // this is equivalent to `! local_ip.isInetAddress()'
            cerr << ": IP address is not correct!" << endl;
            exit();
        }

        cout << local_ip.getHostname() <<
            " is going to talk to perself through " <<
            local_ip << "..." << endl;

        // ____Here comes the real RTP stuff____

        // Construct two RTPSocket. 22222 will be the base
        // port of A.  33334 will be the base port of B.
        const int A_BASE = 22222;
        const int B_BASE = 33334;

        duplexA = new RTPDuplex(local_ip,A_BASE,B_BASE);

        duplexB = new RTPDuplex(local_ip,B_BASE,A_BASE);

        // Set up A's connection
        duplexA->setSchedulingTimeout(90000);
        duplexA->setExpireTimeout(2500000);
        if( duplexA->connect(local_ip,B_BASE) < 0 )
            cerr << "Duplex A could not connect.";

        // Set up B's connection
        duplexB->setSchedulingTimeout(160000);
        duplexB->setExpireTimeout(3500000);
        if( duplexB->connect(local_ip,A_BASE) < 0 )
            cerr << "Duplex B could not connect.";

        // Let's check the queues  (you should read the documentation
        // so that you know what the queues are for).

        if( duplexA->RTPDataQueue::isActive() )
            cout << "The queue A is active." << endl;
        else
            cerr << "The queue  A is not active." << endl;

        if( duplexB->RTPDataQueue::isActive() )
            cout << "The queue B is active." << endl;
        else
            cerr << "The queue B is not active." << endl;


        cout << "Transmitting..." << endl;

        // This message will be sent on RTP packets, from A to
        // B and from B to A.
        unsigned char helloA[] = "Hello, brave gnu world from A!";
        unsigned char helloB[] = "Hello, brave gnu world from B!";

        // This is not important
        time_t sending_time;
        time_t receiving_time;
        char tmstring[30];

        StaticPayloadFormat pf = sptMP2T;
        duplexA->setPayloadFormat(pf);
        duplexB->setPayloadFormat(pf);

        // This is the main loop, where packets are sent and receipt.
        // A and B both will send and receive packets.
        for( int i = 0 ; true ; i++ ) {

            // A and B do almost exactly the same things,
            // I have kept this here -out of a send/receive
            // method- in the interest of clarity.

            // A: Send an RTP packet
            sending_time = time(NULL);
            duplexA->putData(2*(i)*90000,helloA,
                      strlen((char *)helloA));
            // Tell it
            strftime(tmstring,30,"%X",localtime(&sending_time));
            cout << "A: sending message at " << tmstring << "..."
                 << endl;

            // A: Receive an RTP packet
            receiving_time = time(NULL);
            const AppDataUnit* aduA =
                duplexA->getData(duplexA->getFirstTimestamp());
            if ( aduA ) {
                // Tell it
                strftime(tmstring,30,"%X",localtime(&receiving_time));
                cout << "A:[receiving at " << tmstring << "]: " <<
                    aduA->getData() << endl;
            }
            // Wait for 0.1 seconds
            Thread::sleep(100);

            // B: Send an RTP packet
            sending_time = time(NULL);
            duplexB->putData(2*(i)*90000,helloB,
                     strlen((char *)helloB));
            // Tell it
            strftime(tmstring,30,"%X",localtime(&sending_time));
            cout << "B: sending message at " << tmstring << "..."
                 << endl;

            // B: Receive an RTP packet
            receiving_time = time(NULL);
            const AppDataUnit* aduB =
                duplexB->getData(duplexB->getFirstTimestamp());
            if ( aduB ) {
                // Tell it
                strftime(tmstring,30,"%X",localtime(&receiving_time));
                cout << "B:[receiving at " << tmstring << "]: " <<
                    aduB->getData() << endl;
            }

            Thread::sleep(1900);
        }

    }
};

int main(int argc, char *argv[])
{
    // Construct the main thread. It will not run yet.
    ccRTP_dupHello *hello = new ccRTP_dupHello;

    cout << "This is rtpduphello, a very simple test program for ccRTP."
         << endl << "Strike [Enter] when you are fed up." << endl;

    // Start execution of hello.
    hello->start();

    cin.get();

    cout << endl << "That's all" << endl;

    delete hello;

    exit(0);
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */




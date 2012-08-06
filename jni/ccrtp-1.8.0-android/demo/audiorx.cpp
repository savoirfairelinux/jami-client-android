// audiorx.
// A simple and amusing program for testing basic features of ccRTP.
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

// A very simple mu-law encoded audio player.

// This is an introductory example file that illustrates basic usage
// of ccRTP. You will also see a bit on how to use CommonC++ threads and
// TimerPort.

// I am a player of \mu-law encoded RTP audio packets. I
// do not accept any arguments.

#include <cstdio>
#include <cstdlib>
// Some consts common to audiorx and audiotx
#include <audio.h>
// In order to use ccRTP, the RTP stack of CommonC++, you only need to
// include ...
#include <ccrtp/rtp.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

/**
 * @class ccRTP_AudioReceiver
 * This is the class that will do almost everything.
 */
class ccRTP_AudioReceiver: public Thread, public TimerPort
{
private:
    // This is the file we will write to (/dev/audio)
    int audiooutput;
    // The aforementioned file will be transmitted through this socket
    RTPSession *socket;

public:
    // Constructor
    ccRTP_AudioReceiver() {
        audiooutput=open("/dev/audio",O_WRONLY/*|O_NDELAY*/);

        if( audiooutput > 0 ) {
            cout << "Audio device is ready to play." << endl;
        }else{
            cout << "I could not open /dev/audio " << endl;
            exit();
        }

        socket=NULL;
    }

    // Destructor.
    ~ccRTP_AudioReceiver() {
        terminate();
        delete socket;
        ::close(audiooutput);
    }

    // This method does almost everything.
    void run(void) {
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
            " is going to listen to perself through " <<
            local_ip << "..." << endl;

        // ____Here comes the real RTP stuff____

        // Construct the RTP socket
        socket = new RTPSession(local_ip,RECEIVER_BASE,0);

        // Set up receiver's connection
        socket->setSchedulingTimeout(10000);
        if( !socket->addDestination(local_ip,TRANSMITTER_BASE) )
            cerr << "The receiver could not connect.";

        // Let's check the queue (you should read the documentation
        // so that you know what the queue is for).
        socket->startRunning();
        cout << "The RTP queue is ";
        if( socket->isActive() )
            cout << "active." << endl;
        else
            cerr << "not active." << endl;

        cout << "Waiting for audio packets..." << endl;

        // This will be useful for periodic execution.
        TimerPort::setTimer(PERIOD);

        setCancel(cancelImmediate);
        // This is the main loop, where packets are sent and receipt.
        socket->setPayloadFormat(StaticPayloadFormat(sptPCMU));
        for( int i=0 ; true ; i++ ) {
            const AppDataUnit* adu;
            do {
                adu = socket->getData(socket->getFirstTimestamp());
                if ( NULL == adu )
                    Thread::sleep(5);
                else cout << ".";
            }while ( (NULL == adu) || (adu->getSize() <= 0) );


            // This is for buffering some packets at the
            // receiver side, since playing smoothly
            // without any reception buffer is almost
            // impossible.  Try commenting the two lines
            // below, or stop transmission and continue
            // later: you will probably hear noise or
            // cracks.
            if (i==0)
                Thread::sleep(20);

            if(::write(audiooutput,adu->getData(),adu->getSize()) < (ssize_t)adu->getSize())
                break;

            cout << "." << flush;

            // Let's wait for the next cycle
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(PERIOD);
        }

    } // end of run
};


int main(int argc, char *argv[])
{
    cout << "This is audiorx, a simple test program for ccRTP." << endl;
    cout << "I am waiting for audio packets on port " << RECEIVER_BASE
         << "." << endl;
    cout << "Do you want to hear something? Run audiotx." << endl;
    cout << "Strike [Enter] when you are fed up. Enjoy!." << endl;

    // Construct the main thread.
    ccRTP_AudioReceiver *receiver = new ccRTP_AudioReceiver();

    // Run it.
    receiver->start();

    cin.get();

    cout << endl << "That's all." << endl;

    delete receiver;

    exit(0);
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */




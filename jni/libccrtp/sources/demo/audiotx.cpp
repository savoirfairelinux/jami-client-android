// audiotx.
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


// This is an introductory example file that illustrates basic usage
// of ccRTP. You will also see a bit on how to use CommonC++ threads and
// TimerPort.

// I am a transmitter of \mu-law encoded RTP audio packets. In order
// to hear what I transmit, you should be running my colleague
// `audiorx'. You can give me the name of a .au file as argument.

#include <cstdio>
#include <cstdlib>
// Some consts common to audiotx and audiorx
#include <audio.h>
// In order to use ccRTP, the RTP stack of CommonC++, you only need to
// include ...
#include <ccrtp/rtp.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

/**
 * @class ccRTP_AudioTransmitter
 * This is the class that will do almost everything.
 */
class ccRTP_AudioTransmitter: public Thread, public TimerPort
{
private:
    // This is the descriptor of the file we will read from
    // (commonly, /dev/audio or a .au file)
    int audioinput;

    // If we are sending a .au file
    bool sendingfile;

    // The aforementioned file will be transmitted through this socket
    RTPSession *socket;

public:
    // Constructor. If it is given a file name, this thread will
    // transmit that file. If it is not, /dev/audio input is
    // transmitted
    ccRTP_AudioTransmitter(char *filename=(char *)"") {

        if( !strcmp(filename,"") ) {
            filename=(char *)"/dev/audio";
            sendingfile = false;
        }else{
            sendingfile = true;
        }

        audioinput=open(filename,O_RDONLY|O_NDELAY);

        if( audioinput >= 0 ) {
            cout << "Ready to transmit " << filename << "." <<endl;
        }else{
            cout << "I could not open " << filename << "." << endl;
            exit();
        }

        socket=NULL;
    }

    // Destructor.
    ~ccRTP_AudioTransmitter() {
        terminate();
        delete socket;
        ::close(audioinput);
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
        if( ! local_ip ){
            // this is equivalent to `! local_ip.isInetAddress()'
            cerr << ": IP address is not correct!" << endl;
            exit();
        }

        cout << local_ip.getHostname() <<
            " is going to transmit audio to perself through " <<
            local_ip << "..." << endl;

        // ____Here comes the real RTP stuff____

        // Construct the RTP socket.
        socket = new RTPSession(local_ip,TRANSMITTER_BASE);

        // Set up connection
        socket->setSchedulingTimeout(10000);
        if( !socket->addDestination(local_ip,RECEIVER_BASE) )
            cerr << "I could not connect.";

        socket->setPayloadFormat(StaticPayloadFormat(sptPCMU));

        socket->startRunning();
        cout << "The RTP queue service thread is ";
        if( socket->isActive() )
            cout << "active." << endl;
        else
            cerr << "not active." << endl;

        cout << "Transmitting " << PACKET_SIZE
             << " octects long packets "
             << "every " << PERIOD << " milliseconds..." << endl;

        unsigned char buffer[PACKET_SIZE];
        int count=PACKET_SIZE;

        // This will be useful for periodic execution
        TimerPort::setTimer(PERIOD);

        setCancel(cancelImmediate);
        // This is the main loop, where packets are transmitted.
        for( int i = 0 ; (!sendingfile || count > 0) ; i++ ) {

            count = ::read(audioinput,buffer,PACKET_SIZE);
            if( count > 0 ) {
                // send an RTP packet, providing timestamp,
                // payload type and payload.
                socket->putData(PACKET_SIZE*i,buffer,
                        PACKET_SIZE);
            }
            cout << "." << flush;

            // Let's wait for the next cycle
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(PERIOD);
        }
        cout << endl << "I have got no more data to send. " <<endl;
    }
};

int main(int argc, char *argv[])
{
    cout << "This is audiotx, a simple test program for ccRTP." << endl;
    cout << "You should have run audiorx (the server/receiver) before." << endl;
    cout << "Strike [Enter] when you are fed up. Enjoy!." << endl;

    ccRTP_AudioTransmitter *transmitter;

    // Construct the main thread. It will not run yet.
    if ( argc == 2 )
        transmitter = new ccRTP_AudioTransmitter(argv[1]);
    else
        transmitter = new ccRTP_AudioTransmitter();

    // Start transmitter thread.
    transmitter->start();

    cin.get();

    cout << endl << "That's all." << endl;

    delete transmitter;

    exit(0);
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */





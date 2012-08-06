// rtplisten
// Listen for RTP packets.
// Copyright (C) 2001,2002,2003,2004 Federico Montesino <fedemp@altern.org>
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

#include <cstdlib>
#include <ccrtp/rtp.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

class Listener: RTPSession
{
public:
    Listener(InetMcastAddress& ima, tpport_t port) :
    RTPSession(ima,port) {}

    Listener(InetHostAddress& ia, tpport_t port) :
    RTPSession(ia,port) {}

    void listen()
    {
        cout << "My SSRC identifier is: "
             << hex << (int)getLocalSSRC() << endl;

        defaultApplication().setSDESItem(SDESItemTypeTOOL,
                         "rtplisten demo app.");
        setExpireTimeout(1000000);

        setPayloadFormat(StaticPayloadFormat(sptPCMU));
        startRunning();
        for (;;) {
            const AppDataUnit* adu;
            while ( (adu = getData(getFirstTimestamp())) ) {
                cerr << "I got an app. data unit - "
                     << adu->getSize()
                     << " payload octets ("
                     << "pt " << (int)adu->getType()
                     << ") from "
                     << hex << (int)adu->getSource().getID()
                     << "@" << dec <<
                    adu->getSource().getNetworkAddress()
                     << ":"
                     << adu->getSource().getDataTransportPort()
                     << endl;
                delete adu;
            }
            Thread::sleep(7);
        }
    }

    // redefined from IncomingDataQueue
    void onNewSyncSource(const SyncSource& src)
    {
        cout << "* New synchronization source: " <<
             hex << (int)src.getID() << endl;
    }

    // redefined from QueueRTCPManager
    void onGotSR(SyncSource& source, SendReport& SR, uint8 blocks)
    {
        RTPSession::onGotSR(source,SR,blocks);
        cout << "I got an SR RTCP report from "
             << hex << (int)source.getID() << "@"
             << dec
             << source.getNetworkAddress() << ":"
             << source.getControlTransportPort() << endl;
    }

    // redefined from QueueRTCPManager
    void onGotRR(SyncSource& source, RecvReport& RR, uint8 blocks)
    {
        RTPSession::onGotRR(source,RR,blocks);
        cout << "I got an RR RTCP report from "
             << hex << (int)source.getID() << "@"
             << dec
             << source.getNetworkAddress() << ":"
             << source.getControlTransportPort() << endl;
    }

    // redefined from QueueRTCPManager
    bool onGotSDESChunk(SyncSource& source, SDESChunk& chunk, size_t len)
    {
        bool result = RTPSession::onGotSDESChunk(source,chunk,len);
        cout << "I got a SDES chunk from "
             << hex << (int)source.getID() << "@"
             << dec
             << source.getNetworkAddress() << ":"
             << source.getControlTransportPort()
             << " ("
             << source.getParticipant()->getSDESItem(SDESItemTypeCNAME)
             << ") " << endl;
        return result;
    }

    void onGotGoodbye(const SyncSource& source, const std::string& reason)
    {
        cout << "I got a Goodbye packet from "
             << hex << (int)source.getID() << "@"
             << dec
             << source.getNetworkAddress() << ":"
             << source.getControlTransportPort() << endl;
        cout << "   Goodbye reason: \"" << reason << "\"" << endl;
    }
};

int main(int argc, char *argv[])
{
    cout << "rtplisten" << endl;

    if (argc != 3) {
        cerr << "Syntax: " << " ip port" << endl;
        exit(1);
    }

    InetMcastAddress ima;
    try {
        ima = InetMcastAddress(argv[1]);
    } catch (...) { }

    Listener *foo;
    tpport_t port = atoi(argv[2]);
    if ( ima.isInetAddress() ) {
        foo = new Listener(ima,port);
        cout << "Listening on multicast address " << ima << ":" <<
            port << endl;
    } else {
        InetHostAddress ia(argv[1]);
        foo = new Listener(ia,atoi(argv[2]));
        cout << "Listening on unicast address " << ia << ":" <<
            port << endl;
    }
    cout << "Press Ctrl-C to finish." << endl;
    foo->listen();
    delete foo;
    return 0;
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */

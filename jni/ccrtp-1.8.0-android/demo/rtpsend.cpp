// rtpsend
// Send RTP packets using ccRTP.
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

#include <cstdlib>
#include <ccrtp/rtp.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

/**
 * @brief This class sends an RTP Packet
 **/
class Sender: public RTPSession, public TimerPort
{
public:
    Sender(const unsigned char* data, const InetHostAddress& ia,
    tpport_t port, uint32 tstamp, uint16 count) :
    RTPSession(InetHostAddress("0.0.0.0")), packetsPerSecond(10)
    {
        uint32 timestamp = tstamp? tstamp : 0;

        cout << "My SSRC identifier is: "
             << hex << (int)getLocalSSRC() << endl;

        defaultApplication().setSDESItem(SDESItemTypeTOOL, "rtpsend demo app.");
        setSchedulingTimeout(10000);
        setExpireTimeout(1000000);

        if ( !addDestination(ia,port) ) {
            cerr << "Could not connect" << endl;
            exit();
        }

        setPayloadFormat(StaticPayloadFormat(sptPCMU));
        startRunning();

        uint16 tstampInc = getCurrentRTPClockRate()/packetsPerSecond;
        uint32 period = 1000/packetsPerSecond;
        TimerPort::setTimer(period);
        for ( int i = 0; i < count ; i++ ) {
            putData(timestamp + i*tstampInc,
                data,strlen((char *)data) + 1);
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(period);
        }
    }

private:
    const uint16 packetsPerSecond;
};

int main(int argc, char *argv[])
{
    cout << "rtpsend..." << endl;

    if (argc != 6) {
        cerr << "Syntax: " << "data host port timestamp count" << endl;
        exit(1);
    }

    Sender sender((unsigned char *)argv[1], InetHostAddress(argv[2]),
        atoi(argv[3]), atoi(argv[4]), atoi(argv[5]));

    cout << "I have sent " << argv[5]
         << " RTP packets containing \"" << argv[1]
         << "\", with timestamp " << argv[4]
         << " to " << argv[2] << ":" << argv[3]
         << endl;
    return 0;
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */





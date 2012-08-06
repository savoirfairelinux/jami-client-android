// test ccRTP functionality
// Copyright (C) 2004 Federico Montesino Pouzols <fedemp@altern.org>
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

class PacketsPattern
{
public:
    inline const InetHostAddress& getDestinationAddress() const
        { return destinationAddress; }

    inline const tpport_t getDestinationPort() const
        { return destinationPort; }

    uint32 getPacketsNumber() const
        { return packetsNumber; }

    const unsigned char* getPacketData(uint32 i)
        { return data; }

    const size_t getPacketSize(uint32 i)
        { return packetsSize; }

private:
    static const InetHostAddress destinationAddress;
    static const uint16 destinationPort = 34566;
    static const uint32 packetsNumber = 100;
    static const uint32 packetsSize = 100;
    static unsigned char data[65535];
};

const InetHostAddress PacketsPattern::destinationAddress =
    InetHostAddress("localhost");

unsigned char PacketsPattern::data[65535];

PacketsPattern pattern;

class Test
{
public:
    virtual int doTest() = 0;
};

class SendPacketTransmissionTest : public Test, public Thread, public TimerPort
{
public:
    void run()
    {
        doTest();
    }

    int doTest()
    {
        // should be valid?
        //RTPSession tx();
        RTPSession tx(InetHostAddress("localhost"));
        tx.setSchedulingTimeout(10000);
        tx.setExpireTimeout(1000000);

        tx.startRunning();

        tx.setPayloadFormat(StaticPayloadFormat(sptPCMU));
        if ( !tx.addDestination(pattern.getDestinationAddress(),
                    pattern.getDestinationPort()) ) {
            return 1;
        }

        // 50 packets per second (packet duration of 20ms)
        uint32 period = 20;
        uint16 inc = tx.getCurrentRTPClockRate()/50;
        TimerPort::setTimer(period);
        for ( uint32 i = 0; i < pattern.getPacketsNumber(); i++ ) {
            tx.putData(i*inc, pattern.getPacketData(i), pattern.getPacketSize(i));
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(period);
        }
        return 0;
    }
};

class RecvPacketTransmissionTest : public Test, public Thread
{
public:
    void run()
    {
        doTest();
    }

    int doTest()
    {
        RTPSession rx(pattern.getDestinationAddress(), pattern.getDestinationPort());

        rx.setSchedulingTimeout(10000);
        rx.setExpireTimeout(1000000);

        rx.startRunning();
        rx.setPayloadFormat(StaticPayloadFormat(sptPCMU));
        // arbitrary number of loops
        for ( int i = 0; i < 500 ; i++ ) {
            const AppDataUnit* adu;
            while ( (adu = rx.getData(rx.getFirstTimestamp())) ) {

                delete adu;
            }
            Thread::sleep(7);
        }
        return 0;
    }
};

class MiscTest : public Test, public Thread, public TimerPort
{
    void run()
    {
        doTest();
    }

    int doTest()
    {
        const uint32 NSESSIONS = 10;
        RTPSession rx(pattern.getDestinationAddress(),pattern.getDestinationPort());
        RTPSession **tx = new RTPSession* [NSESSIONS];
        for ( uint32 i = 0; i < NSESSIONS; i++ ) {
            tx[i] = new RTPSession(InetHostAddress("localhost"));
        }
        for ( uint32 i = 0; i  < NSESSIONS; i++) {
            tx[i]->setSchedulingTimeout(10000);
            tx[i]->setExpireTimeout(1000000);
            tx[i]->setPayloadFormat(StaticPayloadFormat(sptPCMU));
            if ( !tx[i]->addDestination(pattern.getDestinationAddress(), pattern.getDestinationPort()) ) {
                return 1;
            }
        }

        rx.setPayloadFormat(StaticPayloadFormat(sptPCMU));
        rx.setSchedulingTimeout(5000);
        rx.setExpireTimeout(10000000); // 10 seconds!
        rx.startRunning();

        for ( uint32 i = 0; i  < NSESSIONS; i++) {
            tx[i]->startRunning();
        }
        uint32 period = 20;
        TimerPort::setTimer(period);
        for ( uint32 i = 0; i < pattern.getPacketsNumber(); i++ ) {
            if ( i == 70 ) {
                RTPApplication &app = defaultApplication();
                app.setSDESItem(SDESItemTypeCNAME,"foo@bar");
            }
            for ( uint32 s = 0; s  < NSESSIONS; s++) {
            // 50 packets per second (packet duration of 20ms)
                uint16 inc =
                    tx[s]->getCurrentRTPClockRate()/50;
                tx[s]->putData(i*inc, pattern.getPacketData(i), pattern.getPacketSize(i));
            }
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(period);
        }

        Thread::sleep(5000);
        for ( uint32 i = 0; i < NSESSIONS; i++ ) {
            delete tx[i];
        }
        RTPSession::SyncSourcesIterator it;
        cout << "Sources of synchronization:" << endl;
        for (it = rx.begin() ; it != rx.end(); it++) {
            const SyncSource &s = *it;
            cout << s.getID();
            if ( s.isSender() )
                cout << " (sender) ";
            cout << s.getNetworkAddress() << ":" <<
                s.getControlTransportPort() << "/" <<
                s.getDataTransportPort();
            Participant *p = s.getParticipant();
            cout << " (" <<
                p->getSDESItem(SDESItemTypeCNAME)
                 << ") " << endl;
        }
        RTPApplication &app = defaultApplication();
        RTPApplication::ParticipantsIterator ai;
        cout << "Participants:" << endl;
        for ( ai = app.begin(); ai != app.end(); ai++ ) {
            const Participant &p = *ai;
            cout << p.getSDESItem(SDESItemTypeCNAME) << endl;
            //cout << p.getPRIVPrefix();
        }
        delete tx;
        return 0;
    }
};

// class TestPacketHeaders { }
// header extension

// class TestRTCPTransmission { }

// class TestMiscellaneous { }

// Things that should be tested:
// extreme values (0 - big) for putData
// segmentation (setMaxSendSegmentSize())
// performance: packets/second (depending on packet size and # of participants)
int main(int argc, char *argv[])
{
    int result = 0;
    bool send = false;
    bool recv = false;

    RecvPacketTransmissionTest *rx;
    SendPacketTransmissionTest *tx;

    // accept as parameter if must run as --send or --recv

    // run several tests in parallel threads
    if ( send ) {
        tx = new SendPacketTransmissionTest();
        tx->start();
        tx->join();
    } else  if ( recv ) {
        rx = new RecvPacketTransmissionTest();
        rx->start();
        rx->join();
    } else {
        MiscTest m;
        m.start();
        m.join();
    }
    exit(result);
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */

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
#include <cstring>
#include <ccrtp/rtp.h>
#include <ccrtp/rtppkt.h>
#include <ccrtp/crypto/SrtpSymCrypto.h>
#include <ccrtp/CryptoContext.h>
#include <ccrtp/CryptoContextCtrl.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

// Select one of SrtpEncryptionAESF8, SrtpEncryptionAESCM, SrtpEncryptionTWOCM, SrtpEncryptionTWOF8
// per RFC 3711 standard is: SrtpEncryptionAESCM
static int cryptoAlgo = SrtpEncryptionAESCM;

inline int hex_char_to_nibble(uint8_t c)
{
    switch(c) {
    case ('0'): return 0x0;
    case ('1'): return 0x1;
    case ('2'): return 0x2;
    case ('3'): return 0x3;
    case ('4'): return 0x4;
    case ('5'): return 0x5;
    case ('6'): return 0x6;
    case ('7'): return 0x7;
    case ('8'): return 0x8;
    case ('9'): return 0x9;
    case ('a'): return 0xa;
    case ('A'): return 0xa;
    case ('b'): return 0xb;
    case ('B'): return 0xb;
    case ('c'): return 0xc;
    case ('C'): return 0xc;
    case ('d'): return 0xd;
    case ('D'): return 0xd;
    case ('e'): return 0xe;
    case ('E'): return 0xe;
    case ('f'): return 0xf;
    case ('F'): return 0xf;
    default: return -1;   /* this flags an error */
    }
    /* NOTREACHED */
    return -1;  /* this keeps compilers from complaining */
}

/*
 * hex_string_to_octet_string converts a hexadecimal string
 * of length 2 * len to a raw octet string of length len
 */

int hex_string_to_octet_string(char *raw, char *hex, int len)
{
    uint8 x;
    int tmp;
    int hex_len;

    hex_len = 0;
    while (hex_len < len) {
        tmp = hex_char_to_nibble(hex[0]);
        if (tmp == -1)
            return hex_len;
        x = (tmp << 4);
        hex_len++;
        tmp = hex_char_to_nibble(hex[1]);
        if (tmp == -1)
            return hex_len;
        x |= (tmp & 0xff);
        hex_len++;
        *raw++ = x;
        hex += 2;
    }
    return hex_len;
}

class PacketsPattern
{
public:
    inline const InetHostAddress& getDestinationAddress() const
        { return destinationAddress; }

    inline const tpport_t getDestinationPort() const
        { return destinationPort; }

    uint32 getPacketsNumber() const
        { return packetsNumber; }

    uint32 getSsrc() const
        { return 0xdeadbeef; }

    const unsigned char* getPacketData(uint32 i)
        { return data; }

    const size_t getPacketSize(uint32 i)
        { return packetsSize; }

private:
    static const InetHostAddress destinationAddress;
    static const uint16 destinationPort = 5002;
    static const uint32 packetsNumber = 10;
    static const uint32 packetsSize = 12;
    static unsigned char data[];
};

const InetHostAddress PacketsPattern::destinationAddress =
    InetHostAddress("localhost");

unsigned char PacketsPattern::data[] = {
    "0123456789\n"
};

PacketsPattern pattern;

static char* fixKey = (char *)"c2479f224b21c2008deea6ef0e5dbd4a761aef98e7ebf8eed405986c4687";

// static uint8* masterKey =  (uint8*)"masterKeymasterKeymasterKeymaster";
// static uint8* masterSalt = (uint8*)"NaClNaClNaClNa";

uint8 masterKey[] = {   0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                        0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

uint8 masterSalt[] = {  0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d };

static uint8 binKeys[60];

class SendPacketTransmissionTest: public Thread, public TimerPort
{
public:
    void run() {
        doTest();
    }

    int doTest()
    {
        // should be valid?
        //RTPSession tx();
        RTPSession tx(pattern.getSsrc(), InetHostAddress("localhost"));
        tx.setSchedulingTimeout(10000);
        tx.setExpireTimeout(1000000);

        CryptoContext* txCryptoCtx =
            new CryptoContext(pattern.getSsrc(),
                  0,                           // roc,
                  0L,                          // keydr << 48,
                  cryptoAlgo,         // encryption algo
                  SrtpAuthenticationSha1Hmac,  // authtication algo
                  masterKey,                   // Master Key
                  128 / 8,                     // Master Key length
                  masterSalt,                  // Master Salt
                  112 / 8,                     // Master Salt length
                  128 / 8,                     // encryption keyl
                  160 / 8,                     // authentication key len (SHA1))
                  112 / 8,                     // session salt len
                  80 / 8);                     // authentication tag len
        txCryptoCtx->deriveSrtpKeys(0);

        tx.setOutQueueCryptoContext(txCryptoCtx);

        CryptoContextCtrl* txCryptoCtxCtrl = new CryptoContextCtrl(0,
                  cryptoAlgo,         // encryption algo
                  SrtpAuthenticationSha1Hmac,  // authtication algo
                  masterKey,                   // Master Key
                  128 / 8,                     // Master Key length
                  masterSalt,                  // Master Salt
                  112 / 8,                     // Master Salt length
                  128 / 8,                     // encryption keyl
                  160 / 8,                     // authentication key len (SHA1))
                  112 / 8,                     // session salt len
                  80 / 8);                     // authentication tag len
        tx.setOutQueueCryptoContextCtrl(txCryptoCtxCtrl);

        tx.startRunning();

        tx.setPayloadFormat(StaticPayloadFormat(sptPCMU));
        if (!tx.addDestination(pattern.getDestinationAddress(), pattern.getDestinationPort()) ) {
            return 1;
        }

        // 50 packets per second (packet duration of 20ms)
        uint32 period = 20;
        uint16 inc = tx.getCurrentRTPClockRate()/50;
        TimerPort::setTimer(period);
        for ( uint32 i = 0; i < pattern.getPacketsNumber(); i++ ) {
            tx.putData(i*inc, pattern.getPacketData(i), pattern.getPacketSize(i));
            cout << "Sent some data: " << i << endl;
            Thread::sleep(TimerPort::getTimer());
            TimerPort::incTimer(period);
        }
        return 0;
    }
};


class RecvPacketTransmissionTest: public Thread
{
public:
    void run() {
        doTest();
    }

    int doTest()
    {
        RTPSession rx(pattern.getSsrc(), pattern.getDestinationAddress(),
            pattern.getDestinationPort());

        rx.setSchedulingTimeout(10000);
        rx.setExpireTimeout(1000000);

        CryptoContext* rxCryptoCtx =
            new CryptoContext(0,                // SSRC == 0 -> Context template
                    0,                          // roc,
                    0L,                         // keydr << 48,
                    cryptoAlgo,        // encryption algo
                    SrtpAuthenticationSha1Hmac, // authtication algo
                    masterKey,                  // Master Key
                    128 / 8,                    // Master Key length
                    masterSalt,                 // Master Salt
                    112 / 8,                    // Master Salt length
                    128 / 8,                    // encryption keyl
                    160 / 8,                    // authentication keylen (SHA1))
                    112 / 8,                    // session salt len
                    80 / 8);                    // authentication tag len
        rx.setInQueueCryptoContext(rxCryptoCtx);

        CryptoContextCtrl* rxCryptoCtxCtrl = new CryptoContextCtrl(0,
                  cryptoAlgo,         // encryption algo
                  SrtpAuthenticationSha1Hmac,  // authtication algo
                  masterKey,                   // Master Key
                  128 / 8,                     // Master Key length
                  masterSalt,                  // Master Salt
                  112 / 8,                     // Master Salt length
                  128 / 8,                     // encryption keyl
                  160 / 8,                     // authentication key len (SHA1))
                  112 / 8,                     // session salt len
                  80 / 8);                     // authentication tag len

        rx.setInQueueCryptoContextCtrl(rxCryptoCtxCtrl);

        rx.startRunning();
        rx.setPayloadFormat(StaticPayloadFormat(sptPCMU));
        // arbitrary number of loops
        for ( int i = 0; i < 500 ; i++ ) {
            const AppDataUnit* adu;
            while ( (adu = rx.getData(rx.getFirstTimestamp())) ) {
                cerr << "got some data: " << adu->getData() << endl;
                delete adu;
            }
            Thread::sleep(70);
        }
        return 0;
    }
};

int main(int argc, char *argv[])
{
    int result = 0;
    bool send = false;
    bool recv = false;
    bool f8Test = false;

    char* inputKey = NULL;
    char *args = *argv++;

    while(NULL != (args = *argv++)) {
        if(*args == '-')
            ++args;
        if(!strcmp(args, "r") || !strcmp(args, "recv"))
            recv = true;
        else if(!strcmp(args, "s") || !strcmp(args, "send"))
            send = true;
        else if(!strcmp(args, "8") || !strcmp(args, "8test"))
            f8Test = true;
        else if(!strcmp(args, "k") || !strcmp(args, "key"))
            inputKey = *argv++;
        else
            fprintf(stderr, "*** ccsrtptest: %s: unknown option\n", args);
    }

    if (inputKey == NULL) {
        inputKey = fixKey;
    }
    hex_string_to_octet_string((char*)binKeys, inputKey, 60);

    if (send || recv) {
        if (send) {
            cout << "Running as sender" << endl;
        }
        else {
            cout << "Running as receiver" << endl;
        }
    }
    else if (f8Test) {
        cout << "Running F8 test: ";
        int ret = testF8();
        cout << ret << endl;
        exit(ret);
    }
    RecvPacketTransmissionTest *rx;
    SendPacketTransmissionTest *tx;

    // accept as parameter if must run as -s, -r, -8

    // run several tests in parallel threads
    if (send) {
        tx = new SendPacketTransmissionTest();
        tx->start();
        tx->join();
    } else if (recv) {
        rx = new RecvPacketTransmissionTest();
        rx->start();
        rx->join();
    }
    exit(result);
}

/** EMACS **
 * Local variables:
 * mode: c++
 * c-basic-offset: 4
 * End:
 */


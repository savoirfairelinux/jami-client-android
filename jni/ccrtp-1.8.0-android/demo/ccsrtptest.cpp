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
#include <ccrtp/rtppkt.h>
#include <ccrtp/crypto/AesSrtp.h>
#include <ccrtp/CryptoContext.h>

#ifdef  CCXX_NAMESPACES
using namespace ost;
using namespace std;
#endif

static void hexdump(const char* title, const unsigned char *s, int l)
{
    int n=0;

    if (s == NULL) return;

    fprintf(stderr, "%s",title);
    for( ; n < l ; ++n) {
        if((n%16) == 0)
            fprintf(stderr, "\n%04x",n);
        fprintf(stderr, " %02x",s[n]);
    }
    fprintf(stderr, "\n");
}

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
                  SrtpEncryptionAESCM,         // encryption algo
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
                    SrtpEncryptionAESCM,        // encryption algo
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

/*
 * The F8 test vectors according to RFC3711
 */
unsigned char salt[] = {0x32, 0xf2, 0x87, 0x0d};

unsigned char iv[] = {  0x00, 0x6e, 0x5c, 0xba, 0x50, 0x68, 0x1d, 0xe5,
                        0x5c, 0x62, 0x15, 0x99, 0xd4, 0x62, 0x56, 0x4a};

unsigned char key[]= {  0x23, 0x48, 0x29, 0x00, 0x84, 0x67, 0xbe, 0x18,
                        0x6c, 0x3d, 0xe1, 0x4a, 0xae, 0x72, 0xd6, 0x2c};

unsigned char payload[] = {
                        0x70, 0x73, 0x65, 0x75, 0x64, 0x6f, 0x72, 0x61,
                        0x6e, 0x64, 0x6f, 0x6d, 0x6e, 0x65, 0x73, 0x73,
                        0x20, 0x69, 0x73, 0x20, 0x74, 0x68, 0x65, 0x20,
                        0x6e, 0x65, 0x78, 0x74, 0x20, 0x62, 0x65, 0x73,
                        0x74, 0x20, 0x74, 0x68, 0x69, 0x6e, 0x67};  // 39 bytes

unsigned char cipherText[] = {
                        0x01, 0x9c, 0xe7, 0xa2, 0x6e, 0x78, 0x54, 0x01,
                        0x4a, 0x63, 0x66, 0xaa, 0x95, 0xd4, 0xee, 0xfd,
                        0x1a, 0xd4, 0x17, 0x2a, 0x14, 0xf9, 0xfa, 0xf4,
                        0x55, 0xb7, 0xf1, 0xd4, 0xb6, 0x2b, 0xd0, 0x8f,
                        0x56, 0x2c, 0x0e, 0xef, 0x7c, 0x48, 0x02}; // 39 bytes

unsigned char rtpPacketHeader[] = {
                        0x80, 0x6e, 0x5c, 0xba, 0x50, 0x68, 0x1d, 0xe5,
                        0x5c, 0x62, 0x15, 0x99};

unsigned char rtpPacket[] = {
                    0x80, 0x6e, 0x5c, 0xba, 0x50, 0x68, 0x1d, 0xe5,
                    0x5c, 0x62, 0x15, 0x99,                        // header
                    0x70, 0x73, 0x65, 0x75, 0x64, 0x6f, 0x72, 0x61, // payload
                    0x6e, 0x64, 0x6f, 0x6d, 0x6e, 0x65, 0x73, 0x73,
                    0x20, 0x69, 0x73, 0x20, 0x74, 0x68, 0x65, 0x20,
                    0x6e, 0x65, 0x78, 0x74, 0x20, 0x62, 0x65, 0x73,
                    0x74, 0x20, 0x74, 0x68, 0x69, 0x6e, 0x67};
uint32 ROC = 0xd462564a;

static int testF8()
{
    IncomingRTPPkt* rtp = new IncomingRTPPkt(rtpPacket, sizeof(rtpPacket));
    AesSrtp* aesCipher = new AesSrtp();
    AesSrtp* f8AesCipher = new AesSrtp();

    aesCipher->setNewKey(key, sizeof(key));

    /* Create the F8 IV (refer to chapter 4.1.2.2 in RFC 3711):
     *
     * IV = 0x00 || M || PT || SEQ  ||      TS    ||    SSRC   ||    ROC
     *      8Bit  1bit  7bit  16bit       32bit        32bit        32bit
     * ------------\     /--------------------------------------------------
     *       XX       XX      XX XX   XX XX XX XX   XX XX XX XX  XX XX XX XX
     */

    unsigned char derivedIv[16];
    uint32 *ui32p = (uint32 *)derivedIv;

    derivedIv[0] = 0;
    memcpy(&derivedIv[1], rtp->getRawPacket()+1, 11);

    // set ROC in network order into IV
    ui32p[3] = htonl(ROC);

    int32 pad = rtp->isPadded() ? rtp->getPaddingSize() : 0;

    if (memcmp(iv, derivedIv, 16) != 0) {
        cerr << "Wrong IV constructed" << endl;
        hexdump("derivedIv", derivedIv, 16);
        hexdump("test vector Iv", iv, 16);
        return -1;
    }

    // now encrypt the RTP payload data
    aesCipher->f8_encrypt(rtp->getPayload(), rtp->getPayloadSize()+pad,
        derivedIv, key, sizeof(key), salt, sizeof(salt), f8AesCipher);

    // compare with test vector cipher data
    if (memcmp(rtp->getPayload(), cipherText, rtp->getPayloadSize()+pad) != 0) {
        cerr << "cipher data mismatch" << endl;
        hexdump("computed cipher data", rtp->getPayload(), rtp->getPayloadSize()+pad);
        hexdump("Test vcetor cipher data", cipherText, sizeof(cipherText));
        return -1;
    }

    // Now decrypt the data to get the payload data again
    aesCipher->f8_encrypt(rtp->getPayload(), rtp->getPayloadSize()+pad,
        derivedIv, key, sizeof(key), salt, sizeof(salt), f8AesCipher);

    // compare decrypted data with test vector payload data
    if (memcmp(rtp->getPayload(), payload, rtp->getPayloadSize()+pad) != 0) {
        cerr << "payload data mismatch" << endl;
        hexdump("computed payload data", rtp->getPayload(), rtp->getPayloadSize()+pad);
        hexdump("Test vector payload data", payload, sizeof(payload));
        return -1;
    }
    return 0;
}

int main(int argc, char *argv[])
{
    int result = 0;
    bool send = false;
    bool recv = false;
    bool f8Test = false;

    char c;
    char* inputKey = NULL;

    /* check args */
    while (1) {
        c = getopt(argc, argv, "k:rs8");
        if (c == -1) {
            break;
        }
        switch (c) {
        case 'k':
            inputKey = optarg;
            break;
        case 'r':
            recv = true;
            break;
        case 's':
            send = true;
            break;
        case '8':
            f8Test = true;
            break;
        default:
            cerr << "Wrong Arguments" << endl;
            exit(1);
        }
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
        int ret = testF8();
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


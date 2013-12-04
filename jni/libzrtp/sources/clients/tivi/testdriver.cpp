/*
 * Test program for tivi interface
 */
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <CtZrtpSession.h>
#include <CtZrtpCallback.h>

struct sockaddr_in adr_inet;
struct sockaddr_in adr_clnt;
socklen_t lenClnt;          // length
int s;                       // Socket

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

static void displayError(const char *what) {
    fprintf(stderr, "Error: %s: %s\n", strerror(errno), what);
    exit(1);
}

static void sendData(uint8_t *buffer, unsigned int length)
{
    int z = sendto(s, buffer, length, 0,  (struct sockaddr *)&adr_clnt, lenClnt);
    if ( z < 0 ) {
        displayError("sendto(2)");
    }
}

// This is the callback that we use for audio stream
class TestCallbackAudio: public CtZrtpCb {
    void onNewZrtpStatus(CtZrtpSession *session, char *p, CtZrtpSession::streamName streamNm) {
        uint8_t buffer[20];
        fprintf(stderr, "new status: %s\n", p == NULL ? "NULL" : p);
        session->getInfo("sec_state", buffer, 19);
        printf("secState: %s\n", buffer);
    }

    void onNeedEnroll(CtZrtpSession *session, CtZrtpSession::streamName streamNm, int32_t info) {
        fprintf(stderr, "Need enroll\n");
    }

    void onPeer(CtZrtpSession *session, char *name, int iIsVerified, CtZrtpSession::streamName streamNm) {
        fprintf(stderr, "onPeer: %s", name == NULL || strlen(name) == 0 ? "NULL" : name);
        fprintf(stderr, ", verified: %s\n", iIsVerified ? "YES" : "NO");
        uint8_t buffer[20];

        session->getInfo("rs1", buffer, 19);
        printf("RS1: %s ", buffer);

        session->getInfo("rs2", buffer, 19);
        printf("RS2: %s ", buffer);

        session->getInfo("pbx", buffer, 19);
        printf("PBX: %s ", buffer);

        session->getInfo("aux", buffer, 9);
        printf("AUX: %s\n", buffer);

        session->getInfo("lbClient", buffer, 19);
        printf("Client: %s ", buffer);

        session->getInfo("lbVersion", buffer, 19);
        printf("Version: %s ", buffer);

        session->getInfo("lbChiper", buffer, 19);
        printf("cipher: %s ", buffer);

        session->getInfo("lbHash", buffer, 19);
        printf("hash: %s ", buffer);

        session->getInfo("lbAuthTag", buffer, 19);
        printf("auth: %s ", buffer);

        session->getInfo("lbKeyExchange", buffer, 19);
        printf("KeyEx: %s ", buffer);

        session->getInfo("sc_secure", buffer, 19);
        printf("SC secure: %s ", buffer);

        session->getInfo("sdp_hash", buffer, 19);
        printf("zrtp-hash: %s\n", buffer);

        session->setLastPeerNameVerify("TestName", 0);
    }

    void onZrtpWarning(CtZrtpSession *session, char *p, CtZrtpSession::streamName streamNm) {
        fprintf(stderr, "Warning: %s\n", p == NULL ? "NULL" : p);
    }

};

class TestSendCallbackAudio: public CtZrtpSendCb {
    void sendRtp(CtZrtpSession const *session, uint8_t* packet, size_t length, CtZrtpSession::streamName streamNm) {
//        hexdump("ZRTP packet", packet, length);
        fprintf(stderr, "ZRTP send packet, length: %lu, %.8s\n", length, packet+16);
        sendData(packet, length);
    }
};

extern char zrtpBuildInfo[];
static unsigned char recvAuxSecret[] = {1,2,3,4,5,6,7,8,9,0};


int main(int argc,char **argv) {
    int z;
    ssize_t length;
    socklen_t len_inet;
    const char *srvr_addr = "127.0.0.1";
    uint8_t buffer[1300];            // Recv buffer
    uint32_t uiSSRC = 0xfeedbacc;

    fprintf(stderr, "Config info: %s\n", getZrtpBuildInfo());
    
    CtZrtpSession::initCache("testzid.dat");        // initialize cache file

    CtZrtpSession *session = new CtZrtpSession();
    TestCallbackAudio *callback = new TestCallbackAudio();
    TestSendCallbackAudio *sendCallback = new TestSendCallbackAudio();

    session->init(true, true);                      // audio and video

    session->setUserCallback(callback, CtZrtpSession::AudioStream);
    session->setSendCallback(sendCallback, CtZrtpSession::AudioStream);
    session->getSignalingHelloHash((char*)buffer, CtZrtpSession::AudioStream, 0);
    session->setAuxSecret(recvAuxSecret, sizeof(recvAuxSecret));

    fprintf(stderr, "Our Hello hash: %s\n", buffer);

    // Set a bogous peer hello hash to force a warning
    // session->setSignalingHelloHash("950ff29288587ca0115948f386a89aa98d41b56089f267e62d5cbd42c997aa7c", CtZrtpSession::AudioStream);

    s = socket(AF_INET,SOCK_DGRAM,0);
    if ( s == -1 ) {
        displayError("socket()");
    }
    memset(&adr_inet,0,sizeof adr_inet);
    adr_inet.sin_family = AF_INET;
    adr_inet.sin_port = htons(5002);
    adr_inet.sin_addr.s_addr = inet_addr(srvr_addr);

    if (adr_inet.sin_addr.s_addr == INADDR_NONE ) {
        displayError("bad address listener.");
    }
    len_inet = sizeof(adr_inet);

    z = bind(s, (struct sockaddr *)&adr_inet, len_inet);
    if ( z == -1 ) {
        displayError("bind()");
    }

    memset(&adr_inet,0,sizeof adr_inet);
    adr_clnt.sin_family = AF_INET;
    adr_clnt.sin_port = htons(5004);
    adr_clnt.sin_addr.s_addr = inet_addr(srvr_addr);

    if (adr_clnt.sin_addr.s_addr == INADDR_NONE ) {
        displayError("bad address listener.");
    }
    lenClnt = sizeof(adr_clnt);

    if (!session->isStarted(CtZrtpSession::AudioStream))
        session->start(uiSSRC, CtZrtpSession::AudioStream);

    // Now wait for requests:
    for (;;) {

        len_inet = sizeof(adr_clnt);
        length = recvfrom(s, buffer, sizeof(buffer),  0, NULL, NULL);
        if (length < 0) {
            displayError("recvfrom(2)");
        }
//         hexdump("Data before processing", buffer, length);

//         if (!session->isStarted(CtZrtpSession::AudioStream))
//             session->start(uiSSRC, CtZrtpSession::AudioStream);

        /*
         * process incoming data
         */
        size_t newLength;
        int rc = session->processIncomingRtp(buffer, length, &newLength, CtZrtpSession::AudioStream);
        fprintf(stderr, "processing returns: %d\n", rc);
//         hexdump("Data after processing", buffer, newLength);
        if (rc == 0)
            continue;                           // drop packet

        fprintf(stderr, "Received data: %s\n", &buffer[12]); // assume normal RTP packet for debug printout
    }
    /*
     * Close the socket and exit:
     */
    close(s);
    return 0;
}

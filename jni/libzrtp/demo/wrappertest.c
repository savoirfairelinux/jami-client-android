/*
    This class maps the ZRTP C calls to ZRTP C++ methods.
    Copyright (C) 2010  Werner Dittmann

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

#include <libzrtpcpp/ZrtpCWrapper.h>

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

/* Forward declaration of thethe ZRTP specific callback functions that this
  adapter must implement */
static int32_t zrtp_sendDataZRTP (ZrtpContext* ctx, const uint8_t* data, int32_t length ) ;
static int32_t zrtp_activateTimer (ZrtpContext* ctx, int32_t time ) ;
static int32_t zrtp_cancelTimer(ZrtpContext* ctx) ;
static void zrtp_sendInfo (ZrtpContext* ctx, int32_t severity, int32_t subCode ) ;
static int32_t zrtp_srtpSecretsReady (ZrtpContext* ctx, C_SrtpSecret_t* secrets, int32_t part ) ;
static void zrtp_srtpSecretsOff (ZrtpContext* ctx, int32_t part ) ;
static void zrtp_rtpSecretsOn (ZrtpContext* ctx, char* c, char* s, int32_t verified ) ;
static void zrtp_handleGoClear(ZrtpContext* ctx) ;
static void zrtp_zrtpNegotiationFailed(ZrtpContext* ctx, int32_t severity, int32_t subCode ) ;
static void zrtp_zrtpNotSuppOther(ZrtpContext* ctx) ;
static void zrtp_synchEnter(ZrtpContext* ctx) ;
static void zrtp_synchLeave(ZrtpContext* ctx) ;
static void zrtp_zrtpAskEnrollment (ZrtpContext* ctx, char* info ) ;
static void zrtp_zrtpInformEnrollment(ZrtpContext* ctx, char* info ) ;
static void zrtp_signSAS(ZrtpContext* ctx, char* sas) ;
static int32_t zrtp_checkSASSignature (ZrtpContext* ctx, char* sas ) ;

/* The callback function structure for ZRTP */
static zrtp_Callbacks c_callbacks = {
    &zrtp_sendDataZRTP,
    &zrtp_activateTimer,
    &zrtp_cancelTimer,
    &zrtp_sendInfo,
    &zrtp_srtpSecretsReady,
    &zrtp_srtpSecretsOff,
    &zrtp_rtpSecretsOn,
    &zrtp_handleGoClear,
    &zrtp_zrtpNegotiationFailed,
    &zrtp_zrtpNotSuppOther,
    &zrtp_synchEnter,
    &zrtp_synchLeave,
    &zrtp_zrtpAskEnrollment,
    &zrtp_zrtpInformEnrollment,
    &zrtp_signSAS,
    &zrtp_checkSASSignature
};

/*
 * Here start with callback functions that support the ZRTP core
 */
static int32_t zrtp_sendDataZRTP (ZrtpContext* ctx, const uint8_t* data, int32_t length )
{
    return 0;
}

static int32_t zrtp_activateTimer (ZrtpContext* ctx, int32_t time)
{
    return 0;
}

static int32_t zrtp_cancelTimer(ZrtpContext* ctx)
{
    return 0;
}

static void zrtp_sendInfo (ZrtpContext* ctx, int32_t severity, int32_t subCode )
{
}

static int32_t zrtp_srtpSecretsReady (ZrtpContext* ctx, C_SrtpSecret_t* secrets, int32_t part )
{
    return 0;
}

static void zrtp_srtpSecretsOff (ZrtpContext* ctx, int32_t part )
{
}

static void zrtp_rtpSecretsOn (ZrtpContext* ctx, char* c, char* s, int32_t verified )
{
}

static void zrtp_handleGoClear(ZrtpContext* ctx)
{
}

static void zrtp_zrtpNegotiationFailed (ZrtpContext* ctx, int32_t severity, int32_t subCode )
{
}

static void zrtp_zrtpNotSuppOther(ZrtpContext* ctx)
{
}

static void zrtp_synchEnter(ZrtpContext* ctx)
{
}

static void zrtp_synchLeave(ZrtpContext* ctx)
{
}

static void zrtp_zrtpAskEnrollment(ZrtpContext* ctx, char* info )
{

}
static void zrtp_zrtpInformEnrollment(ZrtpContext* ctx, char* info )
{
}

static void zrtp_signSAS(ZrtpContext* ctx, char* sas)
{
}

static int32_t zrtp_checkSASSignature(ZrtpContext* ctx, char* sas )
{
    return 0;
}

int main(int argc, char *argv[])
{
    ZrtpContext* zrtpCtx;
    char* hh;
    char** names;
    
    zrtpCtx = zrtp_CreateWrapper ();
    zrtp_initializeZrtpEngine(zrtpCtx, &c_callbacks, "test", "test.zid", NULL);
    
    hh = zrtp_getHelloHash(zrtpCtx);
    if (hh != 0) 
    {
        printf("hh: %s\n", hh);
    }
    else
        printf("no hh");

    zrtp_InitializeConfig(zrtpCtx);
    names = zrtp_getAlgorithmNames(zrtpCtx, zrtp_HashAlgorithm);
    
    for (; *names; names++) {
        printf("name: %s\n", *names);
    }
    
    return 0;
}
#ifdef __cplusplus
}
#endif

/*
  Copyright (C) 2006-2008 Werner Dittmann

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

/*
 * Authors: Werner Dittmann <Werner.Dittmann@t-online.de>
 */
#include <stdint.h>
#include <libzrtpcpp/ZrtpConfigure.h>
//                             1
//                    1234567890123456
char clientId[] =    "GNU ZRTP 2.1.0  "; // 16 chars max.
char zrtpVersion[] = "1.10";             // must be 4 chars
/**
 *
 */
char HelloMsg[]    = "Hello   ";
char HelloAckMsg[] = "HelloACK";
char CommitMsg[]   = "Commit  ";
char DHPart1Msg[]  = "DHPart1 ";
char DHPart2Msg[]  = "DHPart2 ";
char Confirm1Msg[] = "Confirm1";
char Confirm2Msg[] = "Confirm2";
char Conf2AckMsg[] = "Conf2ACK";
char ErrorMsg[]    = "Error   ";
char ErrorAckMsg[] = "ErrorACK";
char GoClearMsg[]  = "GoClear ";
char ClearAckMsg[] = "ClearACK";
char PingMsg[]     = "Ping    ";
char PingAckMsg[]  = "PingACK ";
char SasRelayMsg[] = "SASrelay";
char RelayAckMsg[] = "RelayACK";

char responder[]      = "Responder";
char initiator[]      = "Initiator";
char iniMasterKey[]   = "Initiator SRTP master key";
char iniMasterSalt[]  = "Initiator SRTP master salt";
char respMasterKey[]  = "Responder SRTP master key";
char respMasterSalt[] = "Responder SRTP master salt";

char iniHmacKey[]  = "Initiator HMAC key";
char respHmacKey[] = "Responder HMAC key";
char retainedSec[] = "retained secret";

char iniZrtpKey[]  = "Initiator ZRTP key";
char respZrtpKey[] = "Responder ZRTP key";

char sasString[] = "SAS";

char KDFString[] = "ZRTP-HMAC-KDF";

char zrtpSessionKey[] = "ZRTP Session Key";

char zrtpMsk[] = "ZRTP MSK";
char zrtpTrustedMitm[] = "Trusted MiTM key";

char s256[] = "S256";
char s384[] = "S384";
const char* mandatoryHash = s256;

char aes3[] = "AES3";
char aes2[] = "AES2";
char aes1[] = "AES1";
char two3[] = "2FS3";
char two2[] = "2FS2";
char two1[] = "2FS1";
const char* mandatoryCipher = aes1;

char dh2k[] = "DH2k";
char ec25[] = "EC25";
char dh3k[] = "DH3k";
char ec38[] = "EC38";
char mult[] = "Mult";
const char* mandatoryPubKey = dh3k;

char b32[] = "B32 ";
const char* mandatorySasType = b32;

char hs32[] = "HS32";
char hs80[] = "HS80";
char sk32[] = "SK32";
char sk64[] = "SK64";
const char* mandatoryAuthLen_1 = hs32;
const char* mandatoryAuthLen_2 = hs80;

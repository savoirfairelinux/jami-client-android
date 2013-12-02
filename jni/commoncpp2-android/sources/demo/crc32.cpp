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
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// As a special exception to the GNU General Public License, permission is
// granted for additional uses of the text contained in its release
// of Common C++.
//
// The exception is that, if you link the Common C++ library with other
// files to produce an executable, this does not by itself cause the
// resulting executable to be covered by the GNU General Public License.
// Your use of that executable is in no way restricted on account of
// linking the Common C++ library code into it.
//
// This exception does not however invalidate any other reasons why
// the executable file might be covered by the GNU General Public License.
//
// This exception applies only to the code released under the
// name Common C++.  If you copy code from other releases into a copy of
// Common C++, as the General Public License permits, the exception does
// not apply to the code that you add in this way.  To avoid misleading
// anyone as to the status of such modified files, you must delete
// this exception notice from them.
//
// If you write modifications of your own for Common C++, it is your choice
// whether to permit this exception to apply to your modifications.
// If you do not wish that, delete this exception notice.

#include <cc++/common.h>

#ifdef	CCXX_NAMESPACES
using namespace std;
using namespace ost;
#endif


int main(int argc, char *argv[])
 {
   unsigned char test[44];
   union {
	   unsigned char buf[4];
	   uint32 value;
   } data;
   uint32 crc;
   int i;

   cout << "CRC32 Algorithm Test\n\n";

   cout << "AAL-5 Test #1 - 40 Octets filled with \"0\" - ";
   cout << "CRC32 = 0x864d7f99\n";

   for (i = 0; i < 40; i++)
	  test[i] = 0x0;
   test[40] = test[41] = test[42] = 0x0;
   test[43] = 0x28;

   CRC32Digest crc1;
   crc1.putDigest(test, 44);
   crc1.getDigest(data.buf);
   crc = data.value;
   cout << "Test #1 CRC32 = " << hex << crc << "\n\n";
   if (crc == 0x864d7f99)
	  cout << "Test #1 PASSED\n\n\n";
   else
	  cout << "Test #1 FAILED\n\n\n";


   cout << "AAL-5 Test #2 - 40 Octets filled with \"1\" - ";
   cout << "CRC32 = 0xc55e457a\n";

   for (i = 0; i < 40; i++)
	  test[i] = 0xFF;
   test[40] = test[41] = test[42] = 0x0;
   test[43] = 0x28;

   CRC32Digest crc2;
   crc2.putDigest(test, 44);
   crc2.getDigest(data.buf);
   crc = data.value;
   cout << "Test #2 CRC32 = " << hex << crc << "\n\n";
   if (crc == 0xc55e457a)
	  cout << "Test #2 PASSED\n\n\n";
   else
	  cout << "Test #2 FAILED\n\n\n";

   cout << "AAL-5 Test #3 - 40 Octets counting 1 to 40 - ";
   cout << "CRC32 = 0xbf671ed0\n";

   for (i = 0; i < 40; i++)
	  test[i] = i+1;
   test[40] = test[41] = test[42] = 0x0;
   test[43] = 0x28;

   CRC32Digest crc3;
   crc3.putDigest(test, 44);
   crc3.getDigest(data.buf);
   crc = data.value;
   cout << "Test #3 CRC32 = " << hex << crc << "\n\n";
   if (crc == 0xbf671ed0)
	  cout << "Test #3 PASSED\n\n\n";
   else
	  cout << "Test #3 FAILED\n\n\n";

}

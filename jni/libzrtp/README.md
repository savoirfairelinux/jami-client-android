## GNU ZRTP C++

This package provides a library that adds ZRTP support to the GNU
ccRTP stack. Phil Zimmermann developed ZRTP to allow ad-hoc, easy to
use key negotiation to setup Secure RTP (SRTP) sessions. GNU ZRTP works
together with GNU ccRTP (1.5.0 or later) and provides a ZRTP
implementation that can be directly embedded into client and server
applications.

The GNU ZRTP implementation is compliant to [RFC 6189][]. Currently GNU ZRTP
C++ supports the following features:

* multi-stream mode
* Finite field Diffie-Helman with 2048 and 3072 bit primes
* Elliptic curve Diffie-Helman with 256 and 384 bit curves
* AES-128 and AES-256 symmetric cipher
* Twofish-128 and Twofish-256 bit symmetric ciphers
* The SRTP authentication methods HMAC-SHA1 with 32 bit and 80 bit length and
  the Skein MAC with 32 bit and 64 bit length
* The Short Authentication String (SAS) type with base 32 encoding (4
  characters)

Enhanced features like PBX SAS relay aka *trusted Man-in-the-Middle* or
preshared mode are not supported but the GNU ZRTP C++ implementation defines
the necessary external interfaces and functions for these enhanced features
(stubs only).

### Interoperability
During the development of ZRTP and its sister implementation ZRTP4J (the Java
version of the ZRTP) Phil Zimmermann, his developers, and I worked together to
make sure Phil's [Zfone][] implementation and the GNU ZRTP implementations can
work together.

[zfone]: http://zfoneproject.com/index.html


### Other implementations based on GNU ZRTP C++ 

The ZRTP4J implementation is a copycat of the original C++ code. I used the
same overall class structure and copied a lot of C++ functionality to Java. Of
course some Java adaptation were done, for example to overcome the problem of
non-existing pointers :-), thus I use some non-obvious array handling. If you
are interessted in the Java implementation of ZRTP then you may have a look
[here][javazrtp]. The Jitsi project uses the Java implementation. Jitsi is a
powerfull communication client and is definitely worth a [look][jitsi].

To enable C based code to use ZRTP C++ I did a C wrapper that offers the same
functionality to C based RTP implementations. The first use of the ZRTP C
wrapper was for the [PJSIP][] library, actually the RTP part of this
library. The ZRTP handler for PJSIP is [here][pjzrtp]. This port enables PJSIP
based clients to use ZRTP. One of the first clients that use this feature is
*[CSipSimple][]*, an very good open source Android SIP client.

[pjsip]: http://www.pjsip.org
[pjzrtp]: https://github.com/wernerd/ZRTP4PJ
[javazrtp]: https://github.com/wernerd/ZRTP4J
[jitsi]: http://www.jitsi.org
[csipsimple]: http://code.google.com/p/csipsimple


### Some notes on GNU ZRTP C++ history
The first application that demonstrated the embedded ZRTP was Minisp (now
defunct). Minisip has it's own RTP stack and the very first version of this
embedded ZRTP implementation worked together with this specific RTP stack. 

A few weeks later I implemented the GNU ccRTP glue code and ZRTP became part
of the official GNU ccRTP project and was named GNU ZRTP C++. The Twinkle
softphone uses GNU ccRTP and GNU ZRTP C++ since it's 0.8.2 release and Michel
de Boer, the implementor of Twinkle, created a nice user interface. All
following versions of Twinkle include GNU ZRTP C++ as well.


### License and further information
Please note, this library is licensed under the GNU GPL, version 3 or 
later, and has been copyright assigned to the Free Software Foundation.

For further information refer to the [ZRTP FAQ][zrtpfaq] and the
[GNU ZRTP howto][zrtphow]. Both are part of the GNU Telephony wiki and are
located in its documentation category.

[zrtphow]:  http://www.gnutelephony.org/index.php/GNU_ZRTP_How_To
[zrtpfaq]:  http://www.gnutelephony.org/index.php/ZRTP_FAQ
[rfc 6189]: http://tools.ietf.org/html/rfc6189

## Building GNU ZRTP C++ 
Since version 1.6 GNU ZRTP C++ supports the *cmake* based build process
only. The cmake build process is simpler than the GNU automake/autoconf
process. To build GNU ZRTP C++ perform the following steps after you unpacked
the source archive or pulled the source from [Github][]:

    cd <zrtpsrc_dir>
	mkdir build
	cd build
	cmake ..
	make
	
Running cmake in a separate `build` directory is the preferred way. Cmake and
the following `make` generate all files in or below the build directory. Thus
the base directory and the source directories are not polluted with `*.o`,
`*.la`, or other files that result from the build process. You may delete the
build directory and create a new one to start from fresh (this is the ultimate
`make clean` :-) ) or you may create a second directory to build with
different settings without mixing the two builds.

[github]: http://github.com/wernerd/ZRTPCPP

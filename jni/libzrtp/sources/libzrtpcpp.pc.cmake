prefix=@prefix@
exec_prefix=@exec_prefix@
libdir=@libdir@
pkglibdir=${libdir}/@PACKAGE@
includedir=@includedir@
pkgincludedir=${includedir}/@PACKAGE@

Name: libzrtpcpp
Description: GNU ZRTP core library
Version: @VERSION@
Requires: @CRYPTOBACKEND@
Libs:  -L${libdir} -l@zrtplibName@
Cflags: -I${includedir}



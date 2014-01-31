prefix=${CMAKE_INSTALL_PREFIX}
libdir=${CMAKE_INSTALL_PREFIX}/lib${LIB_SUFFIX}
includedir=${CMAKE_INSTALL_PREFIX}/include

Name: ucommon
Description: GNU Common C++ class framework
Version: ${PACKAGE_FILE_VERSION}
Libs:  -lcommoncpp -lucommon ${PACKAGE_LIBS}
Cflags: ${PACKAGE_FLAGS}

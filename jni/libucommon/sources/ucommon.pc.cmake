prefix=${CMAKE_INSTALL_PREFIX}
libdir=${CMAKE_INSTALL_PREFIX}/lib${LIB_SUFFIX}
includedir=${CMAKE_INSTALL_PREFIX}/include
minimal="-lucommon ${PACKAGE_LIBS}"

Name: ${PROJECT_NAME}
Description: ${PROJECT_NAME} library
Version: ${PACKAGE_FILE_VERSION}
Libs: -lusecure -lucommon ${ADDITIONAL_LIBS} ${PACKAGE_LIBS}
Cflags: ${PACKAGE_FLAGS}


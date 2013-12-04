
MACRO(GENERATE_PACKAGING PACKAGE VERSION)

  # The following components are regex's to match anywhere (unless anchored)
  # in absolute path + filename to find files or directories to be excluded
  # from source tarball.
  SET (CPACK_SOURCE_IGNORE_FILES
  #svn files
  "\\\\.svn/"
  "\\\\.cvsignore$"
  # temporary files
  "\\\\.swp$"
  # backup files
  "~$"
  # eclipse files
  "\\\\.cdtproject$"
  "\\\\.cproject$"
  "\\\\.project$"
  "\\\\.settings/"
  "\\\\.kdev4/"
  "\\\\.kdev4$"
  "\\\\.kdev4_include_paths$"
  # others
  "\\\\.#"
  "/#"
  "/build*"
  "/autom4te\\\\.cache/"
  "/_build/"
  "/doc/html/"
  "/\\\\.git/"
  # used before
  "/CVS/"
  "/\\\\.libs/"
  "/\\\\.deps/"
  "\\\\.o$"
  "\\\\.lo$"
  "\\\\.la$"
  "\\\\.sh$"
  "Makefile\\\\.in$"
  "\\\\.directory$"
  "\\\\._.DS_Store$"
  "\\\\._buildmac$"
  )

  SET(CPACK_PACKAGE_VENDOR "Werner Dittmann")
  #SET(CPACK_PACKAGE_DESCRIPTION_FILE "${CMAKE_CURRENT_SOURCE_DIR}/ReadMe.txt")
  #SET(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/Copyright.txt")
  #SET(CPACK_PACKAGE_VERSION_MAJOR ${version_major})
  #SET(CPACK_PACKAGE_VERSION_MINOR ${version_minor})
  #SET(CPACK_PACKAGE_VERSION_PATCH ${version_patch})
  SET( CPACK_GENERATOR "TBZ2")
  SET( CPACK_SOURCE_GENERATOR "TBZ2")
  SET( CPACK_SOURCE_PACKAGE_FILE_NAME "${PACKAGE}-${VERSION}" )
  INCLUDE(CPack)

#  SPECFILE()

  ADD_CUSTOM_TARGET(svncheck
    COMMAND cd $(CMAKE_SOURCE_DIR) && LC_ALL=C git status | grep -q "nothing to commit .working directory clean.")

  SET(AUTOBUILD_COMMAND
    COMMAND ${CMAKE_COMMAND} -E remove ${CMAKE_BINARY_DIR}/*.tar.bz2
    COMMAND ${CMAKE_MAKE_PROGRAM} package_source
#    COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_SOURCE_DIR}/package/${PACKAGE}.changes" "${CMAKE_BINARY_DIR}/package/${PACKAGE}.changes"
  )

  ADD_CUSTOM_TARGET(srcpackage_local
      ${AUTOBUILD_COMMAND})

  ADD_CUSTOM_TARGET(srcpackage
     COMMAND ${CMAKE_MAKE_PROGRAM} svncheck
     ${AUTOBUILD_COMMAND})

  ENDMACRO(GENERATE_PACKAGING)

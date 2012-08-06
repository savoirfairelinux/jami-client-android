find_package(Doxygen)

if(DOXYGEN_EXECUTABLE)
  OPTION(DBUS_ENABLE_DOXYGEN_DOCS "build DOXYGEN documentation (requires Doxygen)" ON)
endif(DOXYGEN_EXECUTABLE)

if (DBUS_ENABLE_DOXYGEN_DOCS)
    set (top_srcdir ${CMAKE_SOURCE_DIR}/..)
    configure_file(${CMAKE_SOURCE_DIR}/Doxyfile.cmake ${CMAKE_BINARY_DIR}/Doxyfile )
    add_custom_target(doc 
        COMMAND ${DOXYGEN_EXECUTABLE} ${CMAKE_BINARY_DIR}/Doxyfile
    )
endif (DBUS_ENABLE_DOXYGEN_DOCS)

#
# find docbook generator
# 
find_program(MEINPROC4_EXECUTABLE meinproc4)

find_program(XMLTO_EXECUTABLE xmlto)

if (MEINPROC4_EXECUTABLE OR XMLTO_EXECUTABLE)
  OPTION(DBUS_ENABLE_XML_DOCS "build XML  documentation (requires xmlto or meinproc4)" ON)
endif (MEINPROC4_EXECUTABLE OR XMLTO_EXECUTABLE)

if (MEINPROC4_EXECUTABLE)
	set(DOCBOOK_GENERATOR_NAME "meinproc4" PARENT_SCOPE)
	set(DBUS_XML_DOCS_ENABLED 1)
	if(WIN32)
		get_filename_component(_a ${MEINPROC4_EXECUTABLE} PATH)
		get_filename_component(_meinproc_install_path ${_a} PATH)
	else(WIN32)
		set(_meinproc_install_path ${CMAKE_INSTALL_PREFIX})
	endif(WIN32)
	set(STYLESHEET "${_meinproc_install_path}/share/apps/ksgmltools2/docbook/xsl/html/docbook.xsl")
endif (MEINPROC4_EXECUTABLE)
  
 
if (XMLTO_EXECUTABLE)
	set (DOCBOOK_GENERATOR_NAME "xmlto" PARENT_SCOPE)
	set(DBUS_XML_DOCS_ENABLED 1)
	MESSAGE(STATUS "xmlto docbook generator found")
endif (XMLTO_EXECUTABLE)

if (DBUS_ENABLE_XML_DOCS)

macro (DOCBOOK _sources _options)
  get_filename_component(_infile ${_sources} ABSOLUTE)
  get_filename_component(_basename ${_infile} NAME_WE)
  set(_outfile ${CMAKE_CURRENT_BINARY_DIR}/${_basename}.html)

  if (EXISTS ${_sources})
	  if (MEINPROC4_EXECUTABLE)
		  ADD_CUSTOM_TARGET(${_basename}.html ALL
			${MEINPROC4_EXECUTABLE} --stylesheet ${STYLESHEET} -o ${_outfile} ${_infile} 
			DEPENDS ${_infile} 
			WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
		)
	  endif ()
	  if (XMLTO_EXECUTABLE)
		  ADD_CUSTOM_TARGET(${_basename}.html ALL
			${XMLTO_EXECUTABLE} -vv ${_options} ${_infile} 
			DEPENDS ${_infile} 
			WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
		)
	  endif ()
	  install(FILES ${_outfile} DESTINATION share/doc/dbus)
  else ()
	  MESSAGE(STATUS "skipping xml doc generating for ${_infile}, file not found")
  endif ()
	  
endmacro (DOCBOOK)

### copy tests to builddir so that generated tests and static tests 
### are all in one place.
### todo how to add more filetypes 
MACRO (COPYDIR _src _type)
    FOREACH(FILE_TYPE ${_type})
    	FOREACH(DIR ${_src})
    		FILE(GLOB FILES "${CMAKE_SOURCE_DIR}/../${DIR}/${FILE_TYPE}" )
    		FILE(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/${DIR})
    		FOREACH(FILE ${FILES})
    			GET_FILENAME_COMPONENT(FILENAME ${FILE} NAME)
    			SET (TARGET ${CMAKE_BINARY_DIR}/${DIR}/${FILENAME})
    			configure_file(${FILE} ${TARGET} COPYONLY)
    			IF (CONFIG_VERBOSE)
    				MESSAGE("FROM: ${FILE}\nTO: ${TARGET}\n")
    			ENDIF (CONFIG_VERBOSE)
    		ENDFOREACH(FILE)
    	ENDFOREACH(DIR)
    ENDFOREACH(FILE_TYPE)
ENDMACRO (COPYDIR)

COPYDIR(doc *.png)
COPYDIR(doc *.svg)

DOCBOOK(${CMAKE_SOURCE_DIR}/../doc/dbus-test-plan.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/../doc/dbus-tutorial.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/../doc/dbus-specification.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/../doc/dbus-faq.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/bus/dbus-daemon.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/tools/dbus-monitor.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/tools/dbus-send.xml html-nochunks)
DOCBOOK(${CMAKE_SOURCE_DIR}/tools/dbus-launch.xml html-nochunks)

#
# handle html index file
#
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/index.html.cmake ${CMAKE_CURRENT_BINARY_DIR}/index.html )
install(FILES ${CMAKE_CURRENT_BINARY_DIR}/index.html DESTINATION share/doc/dbus)

set (EXTRA_DIST 	
	${CMAKE_SOURCE_DIR}/../doc/busconfig.dtd			
	${CMAKE_SOURCE_DIR}/../doc/introspect.dtd			
	${CMAKE_SOURCE_DIR}/../doc/introspect.xsl
)

install(FILES ${EXTRA_DIST} DESTINATION share/doc/dbus)

endif(DBUS_ENABLE_XML_DOCS)

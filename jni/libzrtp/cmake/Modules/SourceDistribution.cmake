# - a CMake module that helps to create a source distribution
#
# This module provide some macros that setup a source distribution.
# In contrast to standard CPack processing this is a very lightweight
# module that works very fast. The source distribution module enables
# the Cmake user to add indivdiual files and directories and thus
# provides a more fine grained control than CPack.
#
# The module works similar to the standard CMake INSTALL command: the
# macros of this module prepare CMake files (cmake_src_dist.cmake) that
# contain all necessary commands to create the distribution directoy.
# The make target 'src_dist' executes the commands and builds the
# compressed tar file of the source distribution.  
#
# Usage:
#  src_distribution_init([NOT_INCLUDE_DEFAULT] [<distribtuion name>])
#    Initializes the source distribution functions. Each CMakeList.txt
#    that distributes sources must call this macro before it can use
#    other source distrbution macros. 
#    Only the first call from the top level CMakeLists.txt uses the
#    distribution name argument. All subsequent call silently ignore it.
#    The macro sets the distribution name to ${PROJECT_NAME}-{VERSION}
#    if no distribution name is provided.
#    The macro automatically includes some default files and directories 
#    into the distribution: CMakeLists.txt and the cmake directory. 
#    Set NOT_INCLUDE_DEFAULT to disable this function.
#    The macro creates a make target 'src_dist'. This target executes
#    all operations to create the distribution directory structure and
#    to create the compressed tar file <distrbution name>.tar.gz. The
#    source distribution directory can be deleted afterwards.
#
#  add_src_dist_dirs(<DIRECTORY> [<DIRECTORY>]*)
#    Works imilar to the normal add_subdirectory command of CMake.
#    This call adds a subdirectory that contains sources or other
#    files that go into a source distribution. The subdirecty must
#    contain a CMakeLists.txt file that also uses the source distrbution
#    macros.
#
#  add_src_dist_files(<FILENAME> [<FILENAME>]*)
#    Adds one or more files to the source distrbution.
#
# Eaxample:
#
# include(SourceDistrbution)
#
# The following call initializes the module and sets the distrbution's
# name to 'mySourceDist'. The macro creates a directory with this name
# in the current build directory and include the standard CMakeLists.txt
# file and the 'cmake' directory (it it exists) into the distribution.
#
# src_distribution_init(mySourceDist)
#
#  Now add some files (assumes ${src_files} was set previously):
# add_src_dist_files(README LICENSE ${src_files})
#
#  Now add a subdirectoy, in this case an include directory:
# add_src_dist_dirs(include) 
#
#
# ---- internal macros ----
#
# This macro gets the current directory relative to CMAKE_SOURCE_DIR 
# and sets an internal variable to the current distribution directory.
# Another variable holds the current path to the CMake command file.
# Other macros use these variable to construct commands
# to build the distribution structure.
#
MACRO (_set_src_dist_scope_vars)
STRING(REPLACE "${CMAKE_SOURCE_DIR}" "" _src_dist_subdir "${CMAKE_CURRENT_SOURCE_DIR}")
if (NOT _src_dist_subdir)
    set(_src_dist_fulldir ${SRC_DIST_DIR})
else()
    set(_src_dist_fulldir ${SRC_DIST_DIR}${_src_dist_subdir})
endif()
set(_src_dist_cmd_file_path ${CMAKE_CURRENT_BINARY_DIR}/${_SRC_DIST_CMD_FILE_NAME})
ENDMACRO()

#
# Check for the NOT_INCLUDE_DEFAULT option.
#
MACRO(_src_dist_parse_options _result _default _length)
  set(${_default} TRUE)

  foreach(_arg ${ARGN})
    if (_arg STREQUAL "NOT_INCLUDE_DEFAULT")
         set(${_default} FALSE)
    endif()
  endforeach()

  set(${_result} ${ARGN})
  list(LENGTH ${_result} ${_length})
  if (${_length} GREATER 0)
    list(REMOVE_ITEM ${_result} "NOT_INCLUDE_DEFAULT")
  endif()
  # recompute length of list
  list(LENGTH ${_result} ${_length})

ENDMACRO()


#
# This macro initializes the source distribution package.
# Only the top-level initialization macro init_src_distribution()
# calls this internal macro.
#
MACRO (_src_dist_internal_init)
# internal variable for distribution cmake file
set(_SRC_DIST_CMD_FILE_NAME "cmake_src_dist.cmake")

if (${_src_dist_dirlist_length} EQUAL 0)
   set(_src_dist_tardir ${PROJECT_NAME}-${VERSION})
else()
   list(GET _src_dist_dirlist 0 _src_dist_tardir)
endif()
set(SRC_DIST_DIR ${CMAKE_BINARY_DIR}/${_src_dist_tardir})

message(STATUS "Source distribution direcrory set to: ${SRC_DIST_DIR}")

_set_src_dist_scope_vars()
file(REMOVE ${_src_dist_cmd_file_path})

# fill in first commands into the distribution cmake file. Calling
# 'make src_dist' executes the stored commands and prepares the source
# distrubtion.
#
file(APPEND ${_src_dist_cmd_file_path} "
# clear contents of an existing distribution directory
file(REMOVE_RECURSE ${SRC_DIST_DIR})
")

add_custom_target(src_dist 
  COMMAND ${CMAKE_COMMAND} -P ${_src_dist_cmd_file_path}
  COMMAND ${CMAKE_COMMAND} -E tar cfj ${SRC_DIST_DIR}.tar.bz2 ${_src_dist_tardir}
  COMMAND ${CMAKE_COMMAND} -E remove_directory ${SRC_DIST_DIR}
  )

ENDMACRO()

################# User visible macros ###################
#
MACRO(src_distribution_init)

# clear old src distribution cmake command file
_src_dist_parse_options(_src_dist_dirlist _src_dist_default _src_dist_dirlist_length ${ARGN})

if (NOT DEFINED _SRC_DIST_INIT)
    _src_dist_internal_init()
    set(_SRC_DIST_INIT TRUE)
else()
  _set_src_dist_scope_vars()
  file(REMOVE ${_src_dist_cmd_file_path})
endif()

if(_src_dist_default)
  if(IS_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
    set(_src_dist_list_tmp)
    # Get all files names in cmake subdir
    # Unfortunately CMake also globs all directories and files that start
    # with . - that is not the same as shell behaviour
    file(GLOB_RECURSE _src_dist_names_tmp RELATIVE 
         ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/cmake/*)
    #
    # Remove all file names that contain a name that start with .
    foreach(_nm ${_src_dist_names_tmp})
      string(REGEX REPLACE .*/\\..* "" _nm ${_nm})
      set(_src_dist_list_tmp ${_src_dist_list_tmp} ${_nm})
    endforeach()
    add_src_dist_files(${_src_dist_list_tmp})
  endif()
  if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeLists.txt")
    file(APPEND ${_src_dist_cmd_file_path} "
FILE(INSTALL DESTINATION \"${_src_dist_fulldir}\" TYPE FILE FILES 
\"${CMAKE_CURRENT_SOURCE_DIR}/CMakeLists.txt\") 
") 
  endif()
endif()

ENDMACRO()

# Add a subdirectory to the src distribution
#
MACRO(add_src_dist_dirs)

foreach(_dir ${ARGN})
    if (NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/${_dir}/CMakeLists.txt)
        message(FATAL_ERROR 
	    "Soure distribution subdirectory \"${CMAKE_CURRENT_SOURCE_DIR}/${_dir}\" does not contain a CMakeLists.txt")
    endif()

    # include subdirectory's distribution cmake command file
    file(APPEND ${_src_dist_cmd_file_path} "
include(\"${CMAKE_CURRENT_BINARY_DIR}/${_dir}/${_SRC_DIST_CMD_FILE_NAME}\")
")
endforeach()
ENDMACRO()

#
# Add files to the src distribution. The handles and install files
# that are in the same directory as the current source as well as files
# in sub directories of the current source (with relative path).
#
MACRO(add_src_dist_files)

foreach(_file ${ARGN})
  get_filename_component(_src_dist_tmp_path ${_file} PATH)
#  string(REPLACE "${CMAKE_SOURCE_DIR}" "" _src_dist_tmp_path "${_src_dist_tmp_path}")
  if(_src_dist_tmp_path)
    set(_src_dist_tmp_path ${_src_dist_fulldir}/${_src_dist_tmp_path})
  else ()
    set(_src_dist_tmp_path ${_src_dist_fulldir})
  endif()
  file(APPEND ${_src_dist_cmd_file_path} "
FILE(INSTALL DESTINATION \"${_src_dist_tmp_path}\" TYPE FILE FILES
    \"${CMAKE_CURRENT_SOURCE_DIR}/${_file}\") ")

endforeach()

ENDMACRO()

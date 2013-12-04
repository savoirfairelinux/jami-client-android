cmake_minimum_required (VERSION 2.6)

# setup the Thread include and lib
find_package(Threads)
if(CMAKE_HAVE_PTHREAD_H)
  set(HAVE_PTHREAD_H TRUE)
endif()
set(LIBS ${LIBS} ${CMAKE_THREAD_LIBS_INIT})

#to make sure includes are first taken - it contains config.h
include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_SOURCE_DIR}/srtp
    ${CMAKE_SOURCE_DIR}/common ${CMAKE_SOURCE_DIR}/bnlib)

set(cryptcommon_srcs
    ${CMAKE_SOURCE_DIR}/cryptcommon/twofish.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/twofish_cfb.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/aescrypt.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/aeskey.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/aestab.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/aes_modes.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/macSkein.cpp
    ${CMAKE_SOURCE_DIR}/cryptcommon/skein.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/skein_block.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/skeinApi.c
    ${CMAKE_SOURCE_DIR}/cryptcommon/ZrtpRandom.cpp)

set(zrtp_tivi_src
    ${CMAKE_CURRENT_SOURCE_DIR}/CtZrtpSession.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/CtZrtpStream.cpp
    ${CMAKE_SOURCE_DIR}/common/Thread.cpp
    ${CMAKE_SOURCE_DIR}/common/MutexClass.cpp
    ${CMAKE_SOURCE_DIR}/common/EventClass.cpp)

set(srtp_src
    ${CMAKE_SOURCE_DIR}/srtp/CryptoContext.cpp
    ${CMAKE_SOURCE_DIR}/srtp/CryptoContextCtrl.cpp
    ${CMAKE_SOURCE_DIR}/srtp/SrtpHandler.cpp)

set(crypto_src_srtp
   ${CMAKE_SOURCE_DIR}/srtp/crypto/hmac.cpp
   ${CMAKE_SOURCE_DIR}/srtp/crypto/SrtpSymCrypto.cpp
   ${CMAKE_SOURCE_DIR}/srtp/crypto/sha1.c)

set(zrtpcpp_src ${zrtp_src} ${zrtp_tivi_src} ${zrtp_crypto_src} ${zrtp_skein_src} ${bnlib_src} ${srtp_src} ${crypto_src_srtp} ${cryptcommon_srcs})

# for the Thread classes etc. - remove D_WITHOUT_TIVI_ENV if you compile for/with Tivi modules, maybe build static
# and iclude this into Tivi shared lib in the second step. Need to cross-check with Java build in case of static build.
# Beware of undefined symbols - set correct library build parameters in case of shared lib
add_definitions(-DLINUX -DNANO_SECOND_SLEEP -D_WITHOUT_TIVI_ENV)

if(BUILD_STATIC AND NOT BUILD_SHARED)
    set(LIBRARY_BUILD_TYPE STATIC)
else()
    set(LIBRARY_BUILD_TYPE SHARED)
endif()


if(JAVA)
    # check if JAVA development environment is available. If javac is available
    # we assume the full JDK is available also.
    find_program(JAVAC NAMES javac -version DOC "Java compiler")
    if(NOT JAVAC)
        MESSAGE(FATAL_ERROR "Java support -- Java compiler not found")
    endif()
    if (NOT IS_DIRECTORY $ENV{JAVA_HOME})
        MESSAGE(FATAL_ERROR "Java support -- JAVA_HOME environment variable not set or wrong")
    endif()

    # set the required include paths to compile JNI files
    set(jniInclude $ENV{JAVA_HOME}/include)
    string(TOLOWER ${CMAKE_SYSTEM_NAME} osName)
    set(jniIncludeOs ${jniInclude}/${osName})
    if(NOT IS_DIRECTORY ${jniInclude} AND NOT IS_DIRECTORY ${jniIncludeOs})
        MESSAGE(FATAL_ERROR "Java support -- expected include paths '${jniInclude}' and '${jniIncludeOs}' not found")
    endif()

    # Now we need the swig process to generate the C wrapper and Java files
    find_program(SWIG NAMES swig -version DOC "SWIG process")
    if(NOT SWIG)
        MESSAGE(FATAL_ERROR "Java support -- swig executable not found")
    endif()

    # Generate the files and store them in correct dir, Java package name is: 'cp.bnlib'
    file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/swigJava/wd/tivi)
    execute_process(COMMAND swig -java -c++ -outdir wd/tivi -package wd.tivi -o sessionTest_wrap.cpp -outcurrentdir ${CMAKE_CURRENT_SOURCE_DIR}/swigJava/sessionTest.i
        WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/swigJava")

    # The wrapper file requires some special include paths, set as source file properties
    set(sessionTest_wrap ${CMAKE_CURRENT_BINARY_DIR}/swigJava/sessionTest_wrap.cpp)
    set_source_files_properties(${sessionTest_wrap} PROPERTIES COMPILE_FLAGS "-I${jniInclude} -I${jniIncludeOs}")

    # Add both source files to the bnlib_src
    set(zrtpcpp_src ${zrtpcpp_src} ${sessionTest_wrap})

endif()

add_custom_command(OUTPUT ${CMAKE_BINARY_DIR}/buildinfo.c 
                   COMMAND ${CMAKE_COMMAND} -E echo \"char zrtpBuildInfo[] = \\\"${VERSION}:${GIT_COMMIT}:${CMAKE_SYSTEM_PROCESSOR}\\\"\;\" > ${CMAKE_BINARY_DIR}/buildinfo.c)

add_library(${zrtplibName} ${LIBRARY_BUILD_TYPE} ${zrtpcpp_src} ${CMAKE_BINARY_DIR}/buildinfo.c)
set_target_properties(${zrtplibName} PROPERTIES VERSION ${VERSION} SOVERSION ${SOVERSION})
target_link_libraries(${zrtplibName} ${LIBS})

add_custom_command(TARGET ${zrtplibName} POST_BUILD COMMAND ${CMAKE_COMMAND} -E remove ${CMAKE_BINARY_DIR}/buildinfo.c)

# **** Test programs ****
#
add_executable(testdriver testdriver.cpp)
target_link_libraries(testdriver ${zrtplibName})
add_dependencies(testdriver ${zrtplibName})

add_executable(sdesTestdriver sdesTestdriver.cpp)
target_link_libraries(sdesTestdriver ${zrtplibName})
add_dependencies(sdesTestdriver ${zrtplibName})


# If Java support is enabled then compile the generate Java classes, build jar
# and compile java test program after the shared lib is ready
if(JAVA)
    add_custom_command(TARGET ${zrtplibName} POST_BUILD
                     COMMAND javac -d . wd/tivi/*.java
                     COMMAND jar -cf sessionTest.jar wd
                     COMMAND javac -cp sessionTest.jar -d . "${CMAKE_CURRENT_SOURCE_DIR}/swigJava/SessionTest.java"
                     WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/swigJava"
                     COMMENT "Compile the generated Java source, build JAR file, and test class")
    # To run test class in build directory: java -cp swigJava/sessionTest.jar:swigJava -Djava.library.path=`pwd` SessionTest
endif()

# **** Setup packing environment ****
#
if(${PROJECT_NAME} STREQUAL ${CMAKE_PROJECT_NAME})
    include(${CMAKE_SOURCE_DIR}/cmake/Modules/GeneratePackage.cmake)

    GENERATE_PACKAGING(${PACKAGE} ${VERSION})
endif()

#
# Create the Android make files but do not execute them
if(ANDROID)
    set(TIVI_ENV "-D_WITHOUT_TIVI_ENV")

    foreach(loop ${zrtpcpp_src})
        string(REPLACE "${CMAKE_SOURCE_DIR}/" "" o ${loop})
        set(zrtpcpp_src_spc ${zrtpcpp_src_spc} ${o})
    endforeach()
    set(local_cpp_features "exceptions")

    # The java wrappers may require RTTI in case the SWIG director feature is active.
    if(JAVA)
        set(local_cpp_features ${local_cpp_features} rtti)
    endif()

    string(REPLACE ";" " " zrtpcpp_src_spc "${zrtpcpp_src_spc}")
#    configure_file(${CMAKE_SOURCE_DIR}/clients/tivi/jni/zrtplib/Android.mk.cmake
#                   ${CMAKE_BINARY_DIR}/clients/tivi/jni/zrtplib/Android.mk @ONLY)

#    configure_file(${CMAKE_SOURCE_DIR}/clients/tivi/jni/sqlite3/Android.mk.cmake
#                   ${CMAKE_BINARY_DIR}/clients/tivi/jni/sqlite3/Android.mk @ONLY)

    configure_file(${CMAKE_SOURCE_DIR}/clients/tivi/android/jni/Android.mk
                   ${CMAKE_BINARY_DIR}/clients/tivi/android/jni/Android.mk @ONLY)

    configure_file(${CMAKE_SOURCE_DIR}/clients/tivi/android/jni/Application.mk
                   ${CMAKE_BINARY_DIR}/clients/tivi/android/jni/Application.mk COPYONLY)
endif()


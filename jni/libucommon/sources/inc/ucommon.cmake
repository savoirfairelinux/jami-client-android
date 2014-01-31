# Copyright (C) 2009 David Sugar, Tycho Softworks
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

if (NOT UCOMMON_LIBS AND NOT UCOMMON_FLAGS)
    include(CheckCCompilerFlag)

    if(CMAKE_COMPILER_IS_GNUCXX)
        set(UCOMMON_VISIBILITY_FLAG "-fvisibility=hidden")
        if(MINGW OR MSYS OR CMAKE_SYSTEM MATCHES "Windows")
            set(CHECK_FLAGS -Wno-long-long -mthreads -fvisibility-inlines-hidden)
        else()
            if(CMAKE_SYSTEM MATCHES "SunOS.*")
                set(CHECK_FLAGS -Wno-long-long -mt -fvisibility-inlines-hidden)
            else()
                set(CHECK_FLAGS -Wno-long-long -pthread -mt -fvisibility-inlines-hidden)
            endif()
        endif()
    endif()

    if(BUILD_RUNTIME AND WIN32)
        set(UCOMMON_FLAGS ${UCOMMON_FLAGS} -DUCOMMON_RUNTIME)
    else()
        if(BUILD_STATIC)
            set(UCOMMON_FLAGS ${UCOMMON_FLAGS} -DUCOMMON_STATIC)
        endif()
    endif()

    # see if we are building with or without std c++ libraries...
    if (BUILD_STDLIB)
        # for now we assume only newer libstdc++ library
        set(UCOMMON_FLAGS ${UCOMMON_FLAGS} -DNEW_STDCPP)
        MESSAGE( STATUS "Configuring full ANSI C++ runtime")
    elseif (BUILD_OLDLIB)
        # for really old libstdc++ libraries...
        set(UCOMMON_FLAGS ${UCOMMON_FLAGS} -DOLD_STDCPP)
        MESSAGE( STATUS "Configuring compatible C++ runtime")
    else()
        MESSAGE( STATUS "Configuring minimal C++ runtime")
        if(CMAKE_COMPILER_IS_GNUCXX)
            set(CHECK_FLAGS ${CHECK_FLAGS} -fno-exceptions -fno-rtti -fno-enforce-eh-specs)
            if(MINGW OR MSYS OR CMAKE_SYSTEM MATCHES "Windows")
                set(UCOMMON_LINKING -nodefaultlibs -nostdinc++)
            else()
                set(UCOMMON_LINKING -nodefaultlibs -nostdinc++)
            endif()
        endif()
    endif()

    # check final for compiler flags
    foreach(flag ${CHECK_FLAGS})
        check_c_compiler_flag(${flag} CHECK_${flag})
        if(CHECK_${flag})
            set(UCOMMON_FLAGS ${UCOMMON_FLAGS} ${flag})
        endif()
    endforeach()

    # visibility support for linking reduction (gcc >4.1 only so far...)

    if(UCOMMON_VISIBILITY_FLAG)
        check_c_compiler_flag(${UCOMMON_VISIBILITY_FLAG} CHECK_VISIBILITY)
    endif()

    if(CHECK_VISIBILITY)
        set(UCOMMON_FLAGS ${UCOMMON_FLAGS} ${UCOMMON_VISIBILITY_FLAG} -DUCOMMON_VISIBILITY=1)
    else()
        set(UCOMMON_FLAGS ${UCOMMON_FLAGS} -DUCOMMON_VISIBILITY=0)
    endif()

endif()

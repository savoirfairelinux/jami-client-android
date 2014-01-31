
include_directories(${CMAKE_CURRENT_BINARY_DIR}/src ${CMAKE_SOURCE_DIR}/src ${CMAKE_CURRENT_SOURCE_DIR})

########### next target ###############

set(rtpsend_SRCS rtpsend.cpp)
add_executable(demo-rtpsend ${rtpsend_SRCS})
target_link_libraries(demo-rtpsend ccrtp)
add_dependencies(demo-rtpsend ccrtp)


########### next target ###############

set(rtplisten_SRCS rtplisten.cpp)
add_executable(demo-rtplisten ${rtplisten_SRCS})
target_link_libraries(demo-rtplisten ccrtp)
add_dependencies(demo-rtplisten ccrtp)


########### next target ###############

set(rtphello_SRCS rtphello.cpp)
add_executable(demo-rtphello ${rtphello_SRCS})
target_link_libraries(demo-rtphello ${KDE4_KDECORE_LIBS} ccrtp)
add_dependencies(demo-rtphello ccrtp)

########### next target ###############

set(rtpduphello_SRCS rtpduphello.cpp)
add_executable(demo-rtpduphello ${rtpduphello_SRCS})
target_link_libraries(demo-rtpduphello ccrtp)
add_dependencies(demo-rtpduphello ccrtp)

########### next target ###############

set(audiorx_SRCS audiorx.cpp audio.h)
add_executable(demo-audiorx ${audiorx_SRCS})
target_link_libraries(demo-audiorx ccrtp)
add_dependencies(demo-audiorx ccrtp)

########### next target ###############

set(audiotx_SRCS audiotx.cpp)
add_executable(demo-audiotx ${audiotx_SRCS})
target_link_libraries(demo-audiotx ccrtp)
add_dependencies(demo-audiotx ccrtp)

########### next target ###############

set(ccrtptest_SRCS ccrtptest.cpp)
add_executable(demo-ccrtptest ${ccrtptest_SRCS})
target_link_libraries(demo-ccrtptest ccrtp)
add_dependencies(demo-ccrtptest ccrtp)

########### next target ###############
# SOME build issue remains...
if (SRTP_SUPPORT AND NOT WIN32)
  set(ccsrtptest_SRCS ccsrtptest.cpp)
  add_executable(demo-ccsrtptest ${ccsrtptest_SRCS})
  target_link_libraries(demo-ccsrtptest ccrtp)
  add_dependencies(demo-ccsrtptest ccrtp)
endif()


#to make sure includes are first taken - it contains config.h
include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_CURRENT_SOURCE_DIR}/.. ${CMAKE_CURRENT_SOURCE_DIR}
                     ${CMAKE_CURRENT_SOURCE_DIR}/../zrtp
                     ${CMAKE_CURRENT_SOURCE_DIR}/../clients/ccrtp)

########### next target ###############

add_executable(zrtptest zrtptest.cpp)
target_link_libraries(zrtptest zrtpcpp ccrtp commoncpp)
add_dependencies(zrtptest zrtpcpp)

########### next target ###############

add_executable(zrtptestMulti zrtptestMulti.cpp)
target_link_libraries(zrtptestMulti zrtpcpp ccrtp commoncpp)
add_dependencies(zrtptestMulti zrtpcpp)

########### next target ###############

#add_executable(wrappertest wrappertest.c)
#target_link_libraries(wrappertest zrtpcpp)

########### install files ###############
# None

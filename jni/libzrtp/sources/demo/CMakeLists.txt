
#to make sure includes are first taken - it contains config.h
include_directories(BEFORE ${CMAKE_BINARY_DIR})
include_directories (${CMAKE_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}
                     ${CMAKE_SOURCE_DIR}/zrtp
                     ${CMAKE_SOURCE_DIR}/clients/ccrtp)

if (CCRTP)
    ########### next target ###############

    add_executable(zrtptest zrtptest.cpp)
    target_link_libraries(zrtptest ${zrtplibName})
    add_dependencies(zrtptest ${zrtplibName})

    ########### next target ###############

    add_executable(zrtptestMulti zrtptestMulti.cpp)
    target_link_libraries(zrtptestMulti ${zrtplibName})
    add_dependencies(zrtptestMulti ${zrtplibName})
else()
    add_executable(sdestest sdestest.cpp)
    target_link_libraries(sdestest ${zrtplibName})
    add_dependencies(sdestest ${zrtplibName})
endif()
########### next target ###############

#add_executable(wrappertest wrappertest.c)
#target_link_libraries(wrappertest zrtpcpp)

########### install files ###############
# None

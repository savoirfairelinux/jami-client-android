########### next target ###############

add_executable(zrtptest zrtptest.cpp)
target_link_libraries(zrtptest zrtpcpp)
add_dependencies(zrtptest zrtpcpp)

########### next target ###############

add_executable(zrtptestMulti zrtptestMulti.cpp)
target_link_libraries(zrtptestMulti zrtpcpp)
add_dependencies(zrtptestMulti zrtpcpp)

########### next target ###############

#add_executable(wrappertest wrappertest.c)
#target_link_libraries(wrappertest zrtpcpp)

########### install files ###############
# None


if(enable_ccrtp)
  set(ccrtp_inst ZrtpQueue.h zrtpccrtp.h ZrtpUserCallback.h TimeoutProvider.h)
endif()

install(FILES 
    ZrtpCodes.h ZrtpConfigure.h ZrtpCallback.h ZrtpCWrapper.h 
    ${ccrtp_inst} DESTINATION include/libzrtpcpp)


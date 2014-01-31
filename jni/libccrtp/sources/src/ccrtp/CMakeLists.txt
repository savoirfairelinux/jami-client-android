set(ccrtp1_headers base.h 
		 formats.h 
		 rtppkt.h 
		 rtcppkt.h 
		 sources.h 
		 channel.h  
		 queuebase.h 
		 iqueue.h 
		 oqueue.h
		 ioqueue.h
		 cqueue.h
		 ext.h
		 rtp.h 
		 pool.h
		 CryptoContext.h
         CryptoContextCtrl.h)

########### install files ###############

install(FILES ${ccrtp1_headers} DESTINATION include/ccrtp)

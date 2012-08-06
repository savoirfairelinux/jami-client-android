project(tools)

set (dbus_send_SOURCES
	../../tools/dbus-print-message.c			
	../../tools/dbus-print-message.h			
	../../tools/dbus-send.c
)

set (dbus_monitor_SOURCES
	../../tools/dbus-monitor.c				
	../../tools/dbus-print-message.c			
	../../tools/dbus-print-message.h
)

if (WIN32)
set (dbus_launch_SOURCES
	../../tools/dbus-launch-win.c
)
else (WIN32)
set (dbus_launch_SOURCES
	../../tools/dbus-launch.c
)
endif (WIN32)

if (DBUS_BUILD_X11)
set (dbus_launch_SOURCES
     ${dbus_launch_SOURCES}
     ../../tools/dbus-launch-x11.c
)
endif(DBUS_BUILD_X11)

set (dbus_cleanup_sockets_SOURCES
	../../tools/dbus-cleanup-sockets.c
)

set (dbus_viewer_SOURCES
	../../tools/dbus-names-model.c			
	../../tools/dbus-names-model.h			
	../../tools/dbus-tree-view.c			
	../../tools/dbus-tree-view.h			
	../../tools/dbus-viewer.c
)

add_executable(dbus-send ${dbus_send_SOURCES})
target_link_libraries(dbus-send ${DBUS_LIBRARIES})
install_targets(/bin dbus-send )

add_executable(dbus-launch ${dbus_launch_SOURCES})
target_link_libraries(dbus-launch )
if (DBUS_BUILD_X11)
  target_link_libraries(dbus-launch ${X11_LIBRARIES} )
endif (DBUS_BUILD_X11)
install_targets(/bin dbus-launch )

add_executable(dbus-monitor ${dbus_monitor_SOURCES})
target_link_libraries(dbus-monitor ${DBUS_LIBRARIES})
install_targets(/bin dbus-monitor )

# Microsoft Developer Studio Project File - Name="ccgnu2" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=ccgnu2 - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "ccgnu2.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "ccgnu2.mak" CFG="ccgnu2 - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "ccgnu2 - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "ccgnu2 - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "ccgnu2 - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCGNU2_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MD /W3 /GX /O2 /I "..\common" /I "..\include" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCGNU2_EXPORTS" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0xc0a /d "NDEBUG"
# ADD RSC /l 0xc0a /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /machine:I386
# ADD LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ws2_32.lib /nologo /dll /machine:I386
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "ccgnu2 - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCGNU2_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MDd /W3 /Gm /GX /ZI /Od /I "..\common" /I "..\include" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCGNU2_EXPORTS" /YX /FD /GZ /out:"debug/ccgnu2d.dll" /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0xc0a /d "_DEBUG"
# ADD RSC /l 0xc0a /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ws2_32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /debug /machine:I386 /out:"Debug/ccgnu2d.dll" /pdbtype:sept
# SUBTRACT LINK32 /pdb:none

!ENDIF 

# Begin Target

# Name "ccgnu2 - Win32 Release"
# Name "ccgnu2 - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=..\src\dir.cpp
# End Source File
# Begin Source File

SOURCE=..\src\dso.cpp
# End Source File
# Begin Source File

SOURCE=..\src\event.cpp
# End Source File
# Begin Source File

SOURCE=..\src\exception.cpp
# End Source File
# Begin Source File

SOURCE=..\src\file.cpp
# End Source File
# Begin Source File

SOURCE=..\src\friends.cpp
# End Source File
# Begin Source File

SOURCE=..\src\inaddr.cpp
# End Source File
# Begin Source File

SOURCE=..\src\keydata.cpp
# End Source File
# Begin Source File

SOURCE=..\src\mempager.cpp
# End Source File
# Begin Source File

SOURCE=..\src\missing.cpp
# End Source File
# Begin Source File

SOURCE=..\src\mutex.cpp
# End Source File
# Begin Source File

SOURCE=..\src\peer.cpp
# End Source File
# Begin Source File

SOURCE=..\src\port.cpp
# End Source File
# Begin Source File

SOURCE=..\src\process.cpp
# End Source File
# Begin Source File

SOURCE=..\src\semaphore.cpp
# End Source File
# Begin Source File

SOURCE=..\src\simplesocket.cpp
# End Source File
# Begin Source File

SOURCE=..\src\slog.cpp
# End Source File
# Begin Source File

SOURCE=..\src\socket.cpp
# End Source File
# Begin Source File

SOURCE=..\src\strchar.cpp
# End Source File
# Begin Source File

SOURCE=..\src\string.cpp
# End Source File
# Begin Source File

SOURCE=..\src\thread.cpp
# End Source File
# Begin Source File

SOURCE=..\src\threadkey.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE="..\include\cc++\common.h"
# End Source File
# Begin Source File

SOURCE=".\cc++\config.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\exception.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\export.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\file.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\misc.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\missing.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\network.h"
# End Source File
# Begin Source File

SOURCE=..\src\private.h
# End Source File
# Begin Source File

SOURCE="..\include\cc++\process.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\serial.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\slog.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\socket.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\strchar.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\string.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\thread.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\unix.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\urlstring.h"
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project

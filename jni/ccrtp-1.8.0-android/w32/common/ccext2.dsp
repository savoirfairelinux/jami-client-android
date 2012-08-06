# Microsoft Developer Studio Project File - Name="ccext2" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=ccext2 - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "ccext2.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "ccext2.mak" CFG="ccext2 - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "ccext2 - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "ccext2 - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "ccext2 - Win32 Release"

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
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCEXT2_EXPORTS" /YX /FD /c
# ADD CPP /nologo /MD /W3 /GX /O2 /I "..\common" /I "..\include" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCEXT2_EXPORTS" /YX /FD /c
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0xc0a /d "NDEBUG"
# ADD RSC /l 0xc0a /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /machine:I386
# ADD LINK32 ccgnu2.lib ws2_32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /machine:I386 /libpath:"Release"
# SUBTRACT LINK32 /pdb:none

!ELSEIF  "$(CFG)" == "ccext2 - Win32 Debug"

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
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCEXT2_EXPORTS" /YX /FD /GZ /c
# ADD CPP /nologo /MDd /W3 /Gm /GX /ZI /Od /I "..\common" /I "..\include" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "CCEXT2_EXPORTS" /YX /FD /GZ /out:"debug/ccext2d.dll" /c
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /win32
# ADD BASE RSC /l 0xc0a /d "_DEBUG"
# ADD RSC /l 0xc0a /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 ws2_32.lib ccgnu2d.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib /nologo /dll /debug /machine:I386 /out:"Debug/ccext2d.dll" /pdbtype:sept /libpath:"Debug"
# SUBTRACT LINK32 /pdb:none

!ENDIF 

# Begin Target

# Name "ccext2 - Win32 Release"
# Name "ccext2 - Win32 Debug"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=..\src\buffer.cpp
# End Source File
# Begin Source File

SOURCE=..\src\cmdoptns.cpp
# End Source File
# Begin Source File

SOURCE=..\src\date.cpp
# End Source File
# Begin Source File

SOURCE=..\src\digest.cpp
# End Source File
# Begin Source File

SOURCE=..\src\engine.cpp
# End Source File
# Begin Source File

SOURCE=..\src\fifo.cpp
# End Source File
# Begin Source File

SOURCE=..\src\getopt.c
# End Source File
# Begin Source File

SOURCE=..\src\getopt1.c
# End Source File
# Begin Source File

SOURCE=..\src\md5.cpp
# End Source File
# Begin Source File

SOURCE=..\src\network.cpp
# End Source File
# Begin Source File

SOURCE=..\src\numbers.cpp
# End Source File
# Begin Source File

SOURCE=..\src\persist.cpp
# End Source File
# Begin Source File

SOURCE=..\src\serial.cpp
# End Source File
# Begin Source File

SOURCE=..\src\unix.cpp
# End Source File
# Begin Source File

SOURCE=..\src\url.cpp
# End Source File
# Begin Source File

SOURCE=..\src\urlstring.cpp
# End Source File
# Begin Source File

SOURCE=..\src\xml.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE="..\include\cc++\buffer.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\cmdoptns.h"
# End Source File
# Begin Source File

SOURCE=".\cc++\config.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\digest.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\export.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\groups.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\numbers.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\persist.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\url.h"
# End Source File
# Begin Source File

SOURCE="..\include\cc++\xml.h"
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# End Group
# End Target
# End Project

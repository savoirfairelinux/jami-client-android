@echo off
rem bootstrap framework when no pre-packaged setup.exe is available
rem we use a default configuration and copy the "debug" version of libs

echo Bootstrapping Framework from Debugging Libraries

if exist "C:\Program Files\GNU Telephony" goto common

echo Creating C:\Program Files\GNU Telephony...

mkdir "C:\Program Files\GNU Telephony"
mkdir "C:\Program Files\GNU Telephony\CAPE Framework"
mkdir "C:\Program Files\GNU Telephony\CAPE Framework\Include"
mkdir "C:\Program Files\GNU Telephony\CAPE Framework\Include\cc++"
mkdir "C:\Program Files\GNU Telephony\CAPE Framework\Compat"
mkdir "C:\Program Files\GNU Telephony\CAPE Framework\Import"

:common
if exist "C:\Program Files\Common Files\CAPE Framework" goto include

echo Creating C:\Program Files\Common Files\CAPE Framework...

mkdir "C:\Program Files\Common Files\CAPE Framework"
mkdir "C:\Program Files\Common Files\CAPE Framework\Common"
mkdir "C:\Program Files\Common Files\CAPE Framework\Compat"

:include

echo Copying include files...

copy /y ..\include\cc++\*.h "C:\Program Files\GNU Telephony\CAPE Framework\Include\cc++" >nul
copy /y cc++\config.h "C:\Program Files\GNU Telephony\CAPE Framework\Include\cc++" >nul
copy /y ..\src\getopt.h "C:\Program Files\GNU Telephony\CAPE Framework\Include" >nul

if not exist debug6\ccext2.lib goto  import

echo Copying compat import libs...
copy /y debug6\cc*.lib "C:\Program Files\GNU Telephony\CAPE Framework\Compat" >nul

:import

if not exist debug\ccext2.lib goto compat

echo Copying common import libs...

copy /y debug\cc*.lib "C:\Program Files\GNU Telephony\CAPE Framework\Import" >nul

:compat

if not exist debug6\ccext2.dll goto runtime

echo Copying compat runtime files...

copy /y debug6\cc*.dll "C:\Program Files\Common Files\CAPE Framework\Compat" >nul

:runtime

if not exist debug\ccext2.dll goto finish

echo Copying common runtime files...

copy /y debug\cc*.dll "C:\Program Files\Common Files\CAPE Framework\Common" >nul

:finish

echo Updating registry...

regedit /s common.reg

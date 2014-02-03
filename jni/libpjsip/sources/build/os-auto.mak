# build/os-auto.mak.  Generated from os-auto.mak.in by configure.

export OS_CFLAGS   := $(CC_DEF)PJ_AUTOCONF=1  -fpic -ffunction-sections -funwind-tables -no-canonical-prefixes -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb -Os -g -DNDEBUG -fomit-frame-pointer -finline-limit=64 -O0 -UNDEBUG -marm -fno-omit-frame-pointer -Ijni -DANDROID -Wa,--noexecstack -Wformat -Werror=format-security -I/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/include  -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/include -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/include -DPJ_IS_BIG_ENDIAN=0 -DPJ_IS_LITTLE_ENDIAN=1

export OS_CXXFLAGS := $(CC_DEF)PJ_AUTOCONF=1  -fpic -ffunction-sections -funwind-tables -no-canonical-prefixes -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16 -mthumb -Os -g -DNDEBUG -fomit-frame-pointer -finline-limit=64 -O0 -UNDEBUG -marm -fno-omit-frame-pointer -Ijni -DANDROID -Wa,--noexecstack -Wformat -Werror=format-security -I/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/include  -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/include -I/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a/include  -shared --sysroot=/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm -lgcc -no-canonical-prefixes -march=armv7-a -Wl,--fix-cortex-a8 -Wl,--no-undefined -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now -lc -lm -fexceptions

export OS_LDFLAGS  :=  -nostdlib -L/home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/lib/ -L/home/lisional/Dev/ADT/ndk/sources/cxx-stl/gnu-libstdc++/4.8/libs/armeabi-v7a -lm /home/lisional/Dev/ADT/ndk/platforms/android-19/arch-arm/usr/lib/crtbegin_so.o -lgnustl_shared  -lc -lgcc -llog -lgcc

export OS_SOURCES  := 



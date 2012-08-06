CXXFLAGS=-Wno-psabi ./configure --host=arm-linux-androideabi --with-sysroot=/home/evigier/android-14b-toolchain/sysroot
CXXFLAGS=-Wno-psabi ./configure --host=arm-linux-androideabi --with-sysroot=/home/evigier/android-14b-toolchain/sysroot --prefix=/home/evigier/android-ndk-r8b/platforms/android-14/arch-arm/usr
cp config.h.android config.h
cp inc/cc++/config.h.android inc/cc++/config.h

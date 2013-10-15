echo "NDK_STANDALONE_TC = $NDK_STANDALONE_TC"
echo "NDK = $NDK"
./autogen.sh
./configure --host=arm-linux-androideabi --with-sysroot=$NDK_STANDALONE_TC/sysroot --prefix=$NDK/platforms/android-14/arch-arm/usr --disable-srtp
ln -s src jni
ndk-build -j4

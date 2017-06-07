FROM ubuntu:16.04

ENV LANG en_US.utf8
ENV LC_ALL en_US.utf8
ENV HOME /home/builder
ENV SSH_AUTH_SOCK /home/builder/.sockets/ssh

RUN apt-get update && apt-get install -y --no-install-recommends \
	asciidoc \
	autogen \
	automake \
	autoconf \
	autopoint \
	ca-certificates \
	cmake \
	bc \
	bison \
	build-essential \
	bzip2 \
	doxygen \
	git \
	gradle \
	lib32stdc++6 \
	lib32z1 \
	libtool \
	m4 \
	openjdk-8-jdk \
	pkg-config \
	python-software-properties \
	ssh \
	swig \
	software-properties-common \
	unzip \
	wget \
	curl \
	yasm \
	zip \
	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL

# Android SDK tools
RUN wget -O /tmp/android-tools.zip https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip && \
	mkdir -p /opt/android-sdk && \
	unzip -d /opt/android-sdk /tmp/android-tools.zip && \
	rm -f /tmp/android-tools.zip && \
	chown -R root:root /opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/tools/bin
RUN (while sleep 1; do echo "y"; done) | sdkmanager --update

# Android SDK libraries, NDK
ENV ANDROID_DEPS=\
'build-tools;25.0.3 \
 platforms;android-25 \
 extras;android;m2repository \
 extras;google;m2repository \
 ndk-bundle'
RUN (while sleep 1; do echo "y"; done) | sdkmanager $ANDROID_DEPS
ENV ANDROID_SDK=/opt/android-sdk
ENV ANDROID_NDK=/opt/android-sdk/ndk-bundle

# Gas-preprocessor (for ffmpeg ASM)
RUN wget https://raw.githubusercontent.com/FFmpeg/gas-preprocessor/master/gas-preprocessor.pl && \
	chmod a+x gas-preprocessor.pl && \
	mv gas-preprocessor.pl /usr/bin

WORKDIR /home/builder/src
VOLUME /home/builder/src

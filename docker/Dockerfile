FROM gradle:jdk8

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
	gettext \
	ca-certificates \
	cmake \
	bc \
	bison \
	build-essential \
	bzip2 \
	doxygen \
	git \
	lib32stdc++6 \
	lib32z1 \
	libtool \
	locales \
	m4 \
	pkg-config \
	software-properties-common \
	ssh \
	unzip \
	wget \
	curl \
	yasm \
	nasm \
	zip \
	libpcre3 \
	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL

# Android SDK tools
RUN wget -O /tmp/android-tools.zip https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip && \
	mkdir -p /opt/android-sdk && \
	unzip -q -d /opt/android-sdk /tmp/android-tools.zip && \
	rm -f /tmp/android-tools.zip && \
	chown -R root:root /opt/android-sdk

# Swig 4.0.1
RUN wget -O /tmp/swig.tar.gz https://github.com/swig/swig/archive/rel-4.0.1.tar.gz
RUN tar xzf  /tmp/swig.tar.gz -C /opt
RUN cd /opt/swig-rel-4.0.1/ && ./autogen.sh && ./configure && make
RUN cd /opt/swig-rel-4.0.1/ && make install

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/tools/bin
RUN (while sleep 1; do echo "y"; done) | sdkmanager --update

# Android SDK libraries, NDK
ENV ANDROID_DEPS=\
'build-tools;29.0.2 \
 platforms;android-29 \
 extras;android;m2repository \
 extras;google;m2repository \
 ndk;21.0.6113669'
RUN (while sleep 1; do echo "y"; done) | sdkmanager $ANDROID_DEPS
ENV ANDROID_SDK=/opt/android-sdk
ENV ANDROID_NDK=/opt/android-sdk/ndk/21.0.6113669

WORKDIR /home/builder/src
VOLUME /home/builder/src

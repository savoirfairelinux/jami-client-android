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
	yasm \
	zip \
	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL


ENV ANDROID_DEPS='platform-tools build-tools;25.0.2 platforms;android-25 extras;android;m2repository extras;google;m2repository'

# Android Linux SDK tools
# See http://developer.android.com/sdk/index.html
RUN wget -O /tmp/android-tools.zip https://dl.google.com/android/repository/tools_r25.2.3-linux.zip && \
	mkdir -p /opt/android-sdk && \
	unzip -d /opt/android-sdk /tmp/android-tools.zip && \
	rm -f /tmp/android-tools.zip && \
	chown -R root:root /opt/android-sdk

# Android Linux NDK
# See http://developer.android.com/tools/sdk/ndk/index.html
RUN wget -O /tmp/android-ndk.zip http://dl.google.com/android/repository/android-ndk-r13b-linux-x86_64.zip && \
	unzip /tmp/android-ndk.zip -d /opt/ && \
	rm -f /tmp/android-ndk.zip && \
	chown -R root:root /opt/android-ndk-r13b

# Other tools and resources of Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK=/opt/android-sdk
ENV ANDROID_NDK=/opt/android-ndk-r13b
ENV PATH=${PATH}:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools
RUN yes | sdkmanager $ANDROID_DEPS

WORKDIR /home/builder/src
VOLUME /home/builder/src

FROM gradle:jdk17

ENV LANG en_US.utf8
ENV LC_ALL en_US.utf8

RUN apt-get update && apt-get install -y  --no-install-recommends \
	apt-transport-https \
	ca-certificates \
	gnupg \
	software-properties-common \
	wget

#RUN wget -O - https://apt.kitware.com/keys/kitware-archive-latest.asc 2>/dev/null | gpg --dearmor - | tee /etc/apt/trusted.gpg.d/kitware.gpg >/dev/null && \
#	apt-add-repository 'deb https://apt.kitware.com/ubuntu/ jammy main'

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
	ninja-build \
	bzip2 \
	doxygen \
	git \
	lib32stdc++6 \
	lib32z1 \
	libtool \
	locales \
	m4 \
	pkg-config \
	python-is-python3 \
	ssh \
	unzip \
	wget \
	curl \
	yasm \
	nasm \
	zip \
	libpcre2-dev \
	libpcre3 \
	libpcre3-dev \
	ruby ruby-dev \
	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL

# Android SDK tools
RUN echo "prefer-family = IPv6" >> /etc/wgetrc
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
RUN wget -O /tmp/android-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
	mkdir -p ${ANDROID_HOME} && \
	unzip -q -d ${ANDROID_HOME} /tmp/android-tools.zip && \
	rm -f /tmp/android-tools.zip && \
	chown -R root:root ${ANDROID_HOME}
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/bin

# Swig 4.1.1
RUN wget -O /tmp/swig.tar.gz https://github.com/swig/swig/archive/v4.1.1.tar.gz && \
	tar xzf  /tmp/swig.tar.gz -C /opt && \
	cd /opt/swig-4.1.1/ && ./autogen.sh && ./configure && make && make install && \
	cd .. && rm -rf /opt/swig-4.1.1 /tmp/swig.tar.gz

# Android SDK libraries, NDK
RUN sdkmanager --sdk_root=${ANDROID_HOME} --update
RUN (while sleep 1; do echo "y"; done) | sdkmanager --channel=1 --sdk_root=${ANDROID_HOME} 'build-tools;34.0.0' \
	'platforms;android-34'\
	'extras;android;m2repository'\
	'extras;google;m2repository'\
	'ndk;26.1.10909125'
ENV ANDROID_SDK=${ANDROID_HOME}
ENV ANDROID_NDK=${ANDROID_HOME}/ndk/26.1.10909125

# Fastlane
RUN gem install fastlane -NV
ENV HOME=/tmp

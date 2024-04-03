FROM gradle:jdk17-jammy

ENV LANG en_US.utf8
ENV LC_ALL en_US.utf8
ENV DEBIAN_FRONTEND noninteractive
ENV DOCKER="true"

# Download packages
RUN apt-get update && apt-get install -y --no-install-recommends \
	apt-transport-https \
	asciidoc \
	autoconf \
	autogen \
	automake \
	autopoint \
	bc \
	bison \
	build-essential \
	bzip2 \
	cmake \
	ca-certificates \
	curl \
	doxygen \
	git \
	gnupg \
	gettext \
	lib32stdc++6 \
	lib32z1 \
	libdbus-glib-1-2 \
	libnss3 \
	libpcre2-dev \
	libpcre3 \
	libpcre3-dev \
	libtool \
	libx11-6 \
	locales \
	m4 \
	nasm \
	ninja-build \
	pkg-config \
	python-is-python3 \
	ruby \
	ruby-dev \
	software-properties-common \
	ssh \
	sudo \
	unzip \
	wget \
	yasm \
	zip \
	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL
	
# Fastlane
RUN gem install fastlane -NV

# Configure wget 
RUN echo "prefer-family = IPv6" >> /etc/wgetrc

# Swig 4.1.1
RUN wget -O /tmp/swig.tar.gz https://github.com/swig/swig/archive/v4.1.1.tar.gz && \
	tar xzf  /tmp/swig.tar.gz -C /opt && \
	cd /opt/swig-4.1.1/ && ./autogen.sh && ./configure && make && make install && \
	cd .. && rm -rf /opt/swig-4.1.1 /tmp/swig.tar.gz



# Commandlinetools 11076708
ENV ANDROID_SDK_ROOT=/opt/android
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
RUN wget https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
  unzip -d $ANDROID_SDK_ROOT /tmp/$ANDROID_CMD && \
  mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools && cd $ANDROID_SDK_ROOT/cmdline-tools &&  mv NOTICE.txt source.properties bin lib tools/  && \
  cd $ANDROID_SDK_ROOT/cmdline-tools/tools && ls
ENV PATH "$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"

# Spoon (test-manager) 
RUN wget -O /spoon-runner.jar "https://search.maven.org/remote_content?g=com.squareup.spoon&a=spoon-runner&v=LATEST&c=jar-with-dependencies"

# Android SDK libraries, NDK
ARG ARCH="x86_64"
ARG TARGET="google_apis_playstore"
ARG API_LEVEL="34"
ARG BUILD_TOOLS="34.0.0"
ARG EMULATOR_PACKAGE="system-images;android-${API_LEVEL};${TARGET};${ARCH}"
RUN sdkmanager --update
RUN yes Y | sdkmanager --licenses
RUN sdkmanager --channel=1 --no_https "platforms;android-${API_LEVEL}" \
	'extras;android;m2repository' \
	'extras;google;m2repository' \
	'ndk;26.2.11394342' \
	"system-images;android-${API_LEVEL};${TARGET};${ARCH}" \
	"$EMULATOR_PACKAGE" \
	"build-tools;${BUILD_TOOLS}" \
	'platform-tools'
ENV ANDROID_SDK=${ANDROID_SDK_ROOT}
ENV ANDROID_NDK=${ANDROID_SDK_ROOT}/ndk/26.2.11394342

# Create emulator
ARG EMULATOR_NAME="nexus"
ARG EMULATOR_DEVICE="Nexus 6"
ENV EMULATOR_NAME=$EMULATOR_NAME
ENV DEVICE_NAME=$EMULATOR_DEVICE
RUN echo "no" | avdmanager --verbose create avd --force --name "${EMULATOR_NAME}" --device "${EMULATOR_DEVICE}" --package "${EMULATOR_PACKAGE}"

WORKDIR /tmp

COPY . /tmp
ENV EXTRA_PATH "$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"

CMD [ "/bin/bash" ]

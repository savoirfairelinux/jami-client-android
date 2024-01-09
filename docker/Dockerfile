FROM gradle:jdk17-jammy

#ENV LANG en_US.utf8
#ENV LC_ALL en_US.utf8
#ENV DEBIAN_FRONTEND noninteractive
#ENV DOCKER="true"

# Download packages
#RUN apt-get update && apt-get install -y --no-install-recommends apt-transport-https \
#	asciidoc \
#	autoconf \
#	autogen \
#	automake \
#	autopoint \
#	bc \
#	bison \
#	build-essential \
#	bzip2 \
#	cmake \
#	ca-certificates \
#	curl \
#	doxygen \
#	git \
#	gnupg \
#	gettext \
#	lib32stdc++6 \
#	lib32z1 \
#	libdbus-glib-1-2 \
#	libnss3 \
#	libpcre2-dev \
#	libpcre3 \
#	libpcre3-dev \
#	libtool \
#	libx11-6 \
#	locales \
#	m4 \
#	nasm \
#	ninja-build \
#	pkg-config \
#	python-is-python3 \
#	ruby \
#	ruby-dev \
#	software-properties-common \
#	ssh \
#	sudo \
#	unzip \
#	wget \
#	yasm \
#	zip \
#	&& locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL

# Fastlane
#RUN gem install fastlane -NV


# Configure wget 
#RUN echo "prefer-family = IPv6" >> /etc/wgetrc





# Swig 4.1.1
#RUN wget -O /tmp/swig.tar.gz https://github.com/swig/swig/archive/v4.1.1.tar.gz && \
#	tar xzf  /tmp/swig.tar.gz -C /opt && \
#	cd /opt/swig-4.1.1/ && ./autogen.sh && ./configure && make && make install && \
#	cd .. && rm -rf /opt/swig-4.1.1 /tmp/swig.tar.gz


# Commandlinetools 11076708
#ENV ANDROID_SDK_ROOT=/opt/android
#ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
#RUN wget https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
#  unzip -d $ANDROID_SDK_ROOT /tmp/$ANDROID_CMD && \
#  mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools && cd $ANDROID_SDK_ROOT/cmdline-tools &&  mv NOTICE.txt source.properties bin lib tools/  && \
#  cd $ANDROID_SDK_ROOT/cmdline-tools/tools && ls
#ENV PATH "$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"

# Spoon (test-manager) 
#RUN wget -O /spoon-runner.jar "https://search.maven.org/remote_content?g=com.squareup.spoon&a=spoon-runner&v=LATEST&c=jar-with-dependencies"

# Android SDK libraries, NDK
#ARG ARCH="x86_64"
#ARG TARGET="google_apis_playstore"
#ARG API_LEVEL="34"
#ARG BUILD_TOOLS="34.0.0"
#ARG EMULATOR_PACKAGE="system-images;android-34;google_apis_playstore;x86_64"
#RUN sdkmanager --update
#RUN yes Y | sdkmanager --licenses
#RUN sdkmanager --channel=1 --no_https "platforms;android-${API_LEVEL}" \
#	'extras;android;m2repository' \
#	'extras;google;m2repository' \
#	'ndk;26.3.11579264' \
#	"system-images;android-${API_LEVEL};${TARGET};${ARCH}" \
#	"$EMULATOR_PACKAGE" \
#	"emulator" \
#	"build-tools;${BUILD_TOOLS}" \
#	'platform-tools'
#ENV ANDROID_SDK=${ANDROID_SDK_ROOT}
#ENV ANDROID_NDK=${ANDROID_SDK_ROOT}/ndk/26.3.11579264


# Create non-root user with identical host uid/gid.
# It will helps with permissions on shared volumes.
ARG HOST_UID
ARG HOST_GID
RUN groupadd --gid $HOST_GID jenkins && \
    useradd --no-log-init --system --uid $HOST_UID --gid jenkins jenkins --create-home

# Install depedencies.
#    - Emulation dependencies
#    - Virtualization dependencies
RUN apt-get update && \ 
    apt-get install --yes \
    	libx11-6 \
    	libpulse0 \
    	libgl1 \
    	libnss3 \
    	libxcomposite1 \
    	libxcursor1 \
    	libxdamage1 \
    	libxi6 libxtst6 \
    	libasound2 && \
   apt-get install --yes \
    	qemu-kvm \
    	libvirt-daemon-system \
    	libvirt-clients bridge-utils
    	
# Enable new non-root user to do virtualization.
ARG abc=0
RUN adduser jenkins kvm

USER jenkins
RUN id
#ARG WORKDIR="/home/root"
ARG WORKDIR="/home/jenkins"
WORKDIR $WORKDIR

# Links.
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"

# Environment variables.
ENV JAVA_HOME=/opt/java/openjdk
ENV ANDROID_SDK_ROOT="$WORKDIR/.android_sdk"
ENV PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools"

# Download and install android SDK Command-Line Tools.
#RUN wget https://dl.google.com/android/repository/${ANDROID_CMD} --directory-prefix /tmp && \
#    unzip -d $ANDROID_SDK_ROOT /tmp/$ANDROID_CMD

# Accept sdkmanager licences.
#RUN yes Y | $ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager --licenses --sdk_root=$ANDROID_SDK_ROOT

# Get latest android SDK Command-Line Tools.
# Then, the sdkmanager should be able to recognize the SDK location, and you won't need to provide the sdk_root flag.
#RUN $ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager --no_https --sdk_root=$ANDROID_SDK_ROOT "cmdline-tools;latest"

# Install everything needed for android emulator.
# Disable https to try to be faster.
#RUN sdkmanager --no_https "platforms;android-33" "build-tools;33.0.2"
#RUN sdkmanager --no_https "extras;google;m2repository" "extras;android;m2repository"
#RUN sdkmanager --no_https "platform-tools" "tools"
#RUN sdkmanager --no_https "system-images;android-33;google_apis_playstore;x86_64"

COPY .android_sdk $WORKDIR/.android_sdk

ARG bust=0
RUN id
# Create Android Virtual Device (AVD).
ARG EMULATOR_NAME="device"
ENV EMULATOR_NAME=$EMULATOR_NAME
RUN avdmanager create avd --name "${EMULATOR_NAME}" --device pixel --package "system-images;android-33;google_apis_playstore;x86_64"
RUN echo avdmanager list avd


CMD [ "/bin/bash" ]

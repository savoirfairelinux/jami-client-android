FROM gradle:jdk17-jammy as build

ENV LANG en_US.utf8
ENV LC_ALL en_US.utf8
ENV DEBIAN_FRONTEND noninteractive

# Script dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    apt-transport-https \
    ca-certificates \
    gnupg \
    software-properties-common \
    wget

# Jami build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
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
    curl \
    doxygen \
    gettext \
    git \
    lib32stdc++6 \
    lib32z1 \
    libpcre2-dev \
    libpcre3 \
    libpcre3-dev \
    libtool \
    locales \
    m4 \
    nasm \
    ninja-build \
    pkg-config \
    python-is-python3 \
    ruby \
    ruby-dev \
    ssh \
    unzip \
    yasm \
    zip \
    && locale-gen $LANG $LC_ALL && update-locale $LANG $LC_ALL

# Swig
RUN wget -O /tmp/swig.tar.gz https://github.com/swig/swig/archive/v4.2.1.tar.gz && \
	tar xzf  /tmp/swig.tar.gz -C /opt && \
	cd /opt/swig-4.2.1/ && ./autogen.sh && ./configure && make && make install && \
	cd .. && rm -rf /opt/swig-4.2.1 /tmp/swig.tar.gz

# Install Fastlane
RUN gem install fastlane -NV

# Install Commandlinetools.
ENV ANDROID_SDK_ROOT=/opt/android
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
RUN wget https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
    unzip -d $ANDROID_SDK_ROOT /tmp/$ANDROID_CMD && \
    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools && cd $ANDROID_SDK_ROOT/cmdline-tools && mv NOTICE.txt source.properties bin lib tools/ && \
    cd $ANDROID_SDK_ROOT/cmdline-tools/tools && ls
ENV PATH "$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"

# Install android SDK libraries, NDK.
ARG API_LEVEL="35"
ARG BUILD_TOOLS="35.0.1"
RUN sdkmanager --update
RUN yes Y | sdkmanager --licenses
RUN sdkmanager --channel=1 --no_https "platforms;android-${API_LEVEL}" \
    'extras;android;m2repository' \
    'extras;google;m2repository' \
    'ndk;28.0.13004108' \
    'cmake;3.30.5' \
    "build-tools;${BUILD_TOOLS}"
ENV ANDROID_SDK=${ANDROID_SDK_ROOT}
ENV ANDROID_NDK=${ANDROID_SDK_ROOT}/ndk/28.0.13004108


FROM build as test

# Jami build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libx11-6

# Install Spoon (test manager).
ENV SPOON_RUNNER_PATH=/spoon-runner.jar
RUN wget -O /spoon-runner.jar "https://search.maven.org/remote_content?g=com.squareup.spoon&a=spoon-runner&v=LATEST&c=jar-with-dependencies"

# Install android SDK libraries for emulation.
ARG API_LEVEL="35"
ARG TARGET="google_apis_playstore"
ARG ARCH="x86_64"
ARG EMULATOR_PACKAGE="system-images;android-${API_LEVEL};${TARGET};${ARCH}"
ARG BUILD_TOOLS="35.0.1"
RUN sdkmanager --update
RUN yes Y | sdkmanager --licenses
RUN sdkmanager --channel=1 --no_https "${EMULATOR_PACKAGE}" "emulator" "platform-tools"

# Create emulator.
ARG EMULATOR_NAME="nexus"
ARG EMULATOR_DEVICE="Nexus 6"
ENV EMULATOR_NAME=$EMULATOR_NAME
ENV DEVICE_NAME=$EMULATOR_DEVICE
RUN echo "no" | avdmanager --verbose create avd --force --name "${EMULATOR_NAME}" --device "${EMULATOR_DEVICE}" --package "${EMULATOR_PACKAGE}"

# Create non-root user with identical host uid/gid.
# It will helps with permissions on shared volumes.
ARG HOST_UID
ARG HOST_GID
RUN groupadd -g $HOST_GID jenkins && \
    useradd --no-log-init --system --uid $HOST_UID --gid jenkins jenkins --create-home

# Define environment variables.
ENV JAVA_HOME=/opt/java/openjdk/
ENV ANDROID_ABI=x86_64
ENV ANDROID_HOME=/opt/android
ENV PATH="$PATH:/opt/java/openjdk/bin:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"

CMD [ "/bin/bash" ]
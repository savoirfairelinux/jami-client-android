#!/usr/bin/env bash

# This script get the short ndk version

source_properties="${ANDROID_NDK}/source.properties"

if [ ! -s "${source_properties}" ]; then
    echo "No NDK found. Abort!"
    exit 1
fi

major_version=$(sed -En -e 's/^Pkg.Revision[ \t]*=[ \t]*([0-9a-f]+).*/\1/p' ${source_properties})
numerical_minor_version=$(sed -En -e 's/^Pkg.Revision[ \t]*=[ \t]*[0-9a-f]+\.([0-9]+).*/\1/p' ${source_properties})
minor_version=$(echo ${numerical_minor_version} | tr 0123456789 abcdefghij)
ndk_version=r${major_version}${minor_version}

echo ${ndk_version}
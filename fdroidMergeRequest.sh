#/bin/bash

if [ $# -ne 3 ] ; then
    echo "Usage:"
    echo "1: tag"
    echo "2: versionName"
    echo "3: versionCode"
    echo "4: private gitlab.com access token"
    echo "e.g. ./fdroidMergeRequest android/20190123_beta 20190123 146 djlhsdKH345456DDGfo7"
    
    exit
fi
    
tag=$1
versionName="$2"
versionCode=$3
gitlabToken=$4

git clone https://gitlab.com/savoirfairelinux/fdroiddata.git
cd fdroiddata/
git fetch upstream || exit 
git checkout upstream/master -b $tag

cp metadata/cx.ring.txt metadata/cx.ring.txt_

head -n -12 metadata/cx.ring.txt_ > metadata/cx.ring.txt

echo "Build:$versionName,$versionCode
    commit=$tag
    timeout=10800
    subdir=client-android/ring-android/app
    submodules=yes
    sudo=apt-get update && \
        apt-get install --yes swig
    gradle=noPush
    rm=client-electron,client-gnome,client-ios,client-macosx,client-uwp,client-windows
    build=cd ../.. && \
        export ANDROID_NDK_ROOT="$ANDROID_NDK" && \
        export ANDROID_ABI="armeabi-v7a arm64-v8a x86" && \
        ./compile.sh --release --no-gradle
    ndk=r17b" >> metadata/cx.ring.txt
    
tail -n 13 metadata/cx.ring.txt_ | head -n -2 >> metadata/cx.ring.txt

echo "Current Version:$versionName" >> metadata/cx.ring.txt
echo "Current Version Code:$versionCode" >> metadata/cx.ring.txt

rm metadata/cx.ring.txt_

git add metadata/cx.ring.txt
git commit -s -m "Updates Jami to $versionName"
git push origin $tag

if [ $# -ne 4 ] ; then
    curl --request POST \
    --url https://gitlab.com/api/v4/projects/10540147/merge_requests \
    --header 'content-type: application/json' \
    --header "private-token: $gitlabToken" \
    --data '{
    "id": 1,
    "title": "New Jami revision",
    "target_branch": "master",
    "source_branch": "master",
    "target_project_id": 36528
    }'
fi
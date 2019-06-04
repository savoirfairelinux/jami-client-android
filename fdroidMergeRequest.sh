#!/usr/bin/env bash

# This script pull/clone a fdroiddata repository

set -x # Get a more verbose output for Jenkins

if [ $# -lt 4 ] ; then
    echo "Usage:"
    echo "1: commit"
    echo "2: versionName"
    echo "3: versionCode"
    echo "4: ndkVersion"
    echo "4: private gitlab.com access token (optional)"
    echo "e.g. ./fdroidMergeRequest.sh e5d0d7d2e625e2455fce3d041dd563c45ab9d4a9 20190123 146 r19c djlhsdKH345456DDGfo7"
    exit
fi

commit=$1
versionName="$2"
versionCode=$3
ndkVersion=$4
gitlabToken=$5

if [ -d "fdroiddata" ]
then
    echo "fdroiddata repository exists"
    git -C fdroiddata checkout master
    git -C fdroiddata pull --rebase
else
    echo "fdroiddata repository does not exists"
    git clone git@gitlab.com:savoirfairelinux/fdroiddata.git
fi

git -C fdroiddata remote add upstream git@gitlab.com:fdroid/fdroiddata.git
git -C fdroiddata fetch upstream || exit
git -C fdroiddata status
git -C fdroiddata checkout upstream/master
git -C fdroiddata config user.name "savoirfairelinux"
git -C fdroiddata config user.email mobile@savoirfairelinux.com

METADATA_FOLDER=fdroiddata/metadata

cp ${METADATA_FOLDER}/cx.ring.yml ${METADATA_FOLDER}/cx.ring.yml_

head -n -11 ${METADATA_FOLDER}/cx.ring.yml_ > ${METADATA_FOLDER}/cx.ring.yml

echo "  - versionName: '${versionName}'
    versionCode: ${versionCode}
    commit: ${commit}
    timeout: 10800
    subdir: client-android/ring-android/app
    submodules: true
    gradle:
        - noPush
    rm:
        - client-electron
        - client-gnome
        - client-ios
        - client-macosx
        - client-uwp
        - client-windows
        - docker
        - docs
        - lrc
        - packaging
        - scripts
    build:
        - cd ../..
        - export ANDROID_NDK_ROOT=\"\$ANDROID_NDK\"
        - export ANDROID_ABI=\"armeabi-v7a arm64-v8a x86\"
        - ./compile.sh --release --no-gradle
    ndk: ${ndkVersion}" >> ${METADATA_FOLDER}/cx.ring.yml


tail -n 12 ${METADATA_FOLDER}/cx.ring.yml_ | head -n -2 >> ${METADATA_FOLDER}/cx.ring.yml

echo "CurrentVersion: '${versionName}'" >> ${METADATA_FOLDER}/cx.ring.yml
echo "CurrentVersionCode: ${versionCode}" >> ${METADATA_FOLDER}/cx.ring.yml

rm ${METADATA_FOLDER}/cx.ring.yml_

releaseDate=`date +%Y%m`
releaseBranch="release_${releaseDate}"

git -C fdroiddata add metadata/cx.ring.yml
git -C fdroiddata commit -s -m "Updates Jami to $versionName"
git -C fdroiddata push origin HEAD:refs/heads/${releaseBranch} -f
git -C fdroiddata status

FDROID_METADATA_PROJECT_ID=36528
SFL_METADATA_PROJECT_ID=10540147

if [ $# -ge 4 ] ; then
    curl --request POST \
    --url https://gitlab.com/api/v4/projects/${SFL_METADATA_PROJECT_ID}/merge_requests \
    --header 'content-type: application/json' \
    --header "private-token: ${gitlabToken}" \
    --data "{
    \"id\": 1,
    \"title\": \"New Jami revision\",
    \"target_branch\": \"master\",
    \"source_branch\": \"${releaseBranch}\",
    \"target_project_id\": ${FDROID_METADATA_PROJECT_ID}
    }"
fi

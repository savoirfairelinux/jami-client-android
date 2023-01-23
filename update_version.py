#!python3
import re
import datetime

PATTERN_CODE = re.compile(r'^(\s*versionCode\s*=\s*)(\d+)$', re.MULTILINE)
PATTERN_NAME = re.compile(r'^(\s*versionName\s*=\s*)"(\w+)(?:-(\d+))?"\s*$', re.MULTILINE)


def get_new_code(old_code):
    return int(old_code) + 1


def get_new_name(name_base, name_serial_str):
    new_name_base = datetime.date.today().strftime("%Y%m%d")
    name_serial = int(name_serial_str) if name_serial_str else 0
    new_name_serial = name_serial + 1 if name_base == new_name_base else 1
    return f"{new_name_base}-{new_name_serial:02}"


def update_version(path, new_code=None, new_name=None):
    with open(path, 'r') as f:
        content = f.read()

    if not new_code:
        new_code = get_new_code(PATTERN_CODE.search(content).group(2))

    if not new_name:
        name_match = PATTERN_NAME.search(content)
        new_name = get_new_name(name_match.group(2), name_match.group(3))

    content = PATTERN_CODE.sub(rf'\g<1>{new_code}', content)
    content = PATTERN_NAME.sub(rf'\g<1>"{new_name}"', content)

    with open(path, 'w') as f:
        f.write(content)

    return (new_code, new_name)


def commit(path, new_code, new_name):
    import subprocess
    subprocess.Popen(['git', 'add', path],
                     stdout=subprocess.PIPE).communicate()
    subprocess.Popen(['git', 'commit', '-m', f'update version to {new_name} ({new_code})'],
                     stdout=subprocess.PIPE).communicate()


if __name__ == '__main__':
    import argparse, json, sys
    parser = argparse.ArgumentParser()
    parser.add_argument('--version-name')
    parser.add_argument('--version-code', type=int)
    parser.add_argument('-p', '--path', default='jami-android/app/build.gradle.kts')
    parser.add_argument('-c', '--commit', action='store_true')
    args = parser.parse_args()

    new_code, new_name = update_version(args.path, args.version_code, args.version_name)
    json.dump({'name': new_name, 'code': new_code}, sys.stdout)
    print()

    if args.commit:
        commit(args.path, new_code, new_name)

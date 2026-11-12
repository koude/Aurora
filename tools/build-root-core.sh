#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output_dir=$1
go_bin=${AURORA_GO:-go}

mkdir -p "$output_dir" "$project_dir/.task-tools/gocache" "$project_dir/.task-tools/gomodcache" "$project_dir/.task-tools/gopath" "$project_dir/.task-tools/tmp"
export GOCACHE="$project_dir/.task-tools/gocache"
export GOMODCACHE="$project_dir/.task-tools/gomodcache"
export GOPATH="$project_dir/.task-tools/gopath"
export TMPDIR="$project_dir/.task-tools/tmp"
export GOTOOLCHAIN=local
export GOOS=android

# Keep the Mihomo submodule untouched. Only Aurora's privileged executable
# uses a dedicated firewall-chain prefix; ordinary VPN/proxy builds do not.
overlay_source="$project_dir/.task-tools/tmp/aurora-root-sing-tun-server.go"
overlay_json="$project_dir/.task-tools/tmp/aurora-root-overlay.json"
python3 - "$project_dir/core/src/foss/golang/clash/listener/sing_tun/server.go" "$overlay_source" "$overlay_json" <<'PY'
import json, pathlib, sys
original, replacement, overlay = map(pathlib.Path, sys.argv[1:])
source = original.read_text()
before = 'TableName:              "mihomo",'
assert source.count(before) == 1, 'Mihomo auto-redirect table name changed upstream'
replacement.write_text(source.replace(before, 'TableName:              "aurora",'))
overlay.write_text(json.dumps({'Replace': {str(original): str(replacement)}}))
PY

cd "$project_dir/core/src/foss/golang/clash"
for target in arm64-v8a:arm64 x86_64:amd64 armeabi-v7a:arm x86:386; do
    abi=${target%%:*}
    arch=${target#*:}
    mkdir -p "$output_dir/$abi"
    if [ "$arch" != arm64 ]; then
        ndk=${AURORA_NDK_HOME:?AURORA_NDK_HOME is required for 32-bit Android cores}
        case "$(uname -s)" in
            Darwin) host=darwin-x86_64 ;;
            Linux) host=linux-x86_64 ;;
            *) echo "Unsupported build host" >&2; exit 1 ;;
        esac
        toolchain="$ndk/toolchains/llvm/prebuilt/$host/bin"
        case "$arch" in
            arm) cc="$toolchain/armv7a-linux-androideabi21-clang" ;;
            386) cc="$toolchain/i686-linux-android21-clang" ;;
            amd64) cc="$toolchain/x86_64-linux-android21-clang" ;;
        esac
        [ -x "$cc" ] || { echo "Missing Android compiler: $cc" >&2; exit 1; }
        CGO_ENABLED=1 CC="$cc" GOARCH=$arch "$go_bin" build -overlay "$overlay_json" -tags with_gvisor -trimpath -ldflags '-s -w' \
            -o "$output_dir/$abi/libaurora_root.so" .
        continue
    fi
    CGO_ENABLED=0 GOARCH=$arch "$go_bin" build -overlay "$overlay_json" -tags with_gvisor -trimpath -ldflags '-s -w' \
        -o "$output_dir/$abi/libaurora_root.so" .
done

#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output_dir=$1
variant=$2
root_dir=$3
sdk_dir=$4
go_bin=${AURORA_GO:-go}
ndk="$sdk_dir/ndk/29.0.14206865"
case "$(uname -s)" in
    Darwin) host=darwin-x86_64 ;;
    Linux) host=linux-x86_64 ;;
    *) echo 'Unsupported build host' >&2; exit 1 ;;
esac
toolchain="$ndk/toolchains/llvm/prebuilt/$host/bin"
cap_variant=$(printf '%s' "$variant" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
core_dir="$project_dir/core/build/intermediates/library_jni/$variant/copy${cap_variant}JniLibsProjectOnly/jni"
tmp_dir="$project_dir/app/build/tmp/rootPatches/$variant"
mkdir -p "$output_dir" "$tmp_dir" "$project_dir/.task-tools/gocache" "$project_dir/.task-tools/gomodcache" "$project_dir/.task-tools/gopath" "$project_dir/.task-tools/tmp"
export GOCACHE="$project_dir/.task-tools/gocache"
export GOMODCACHE="$project_dir/.task-tools/gomodcache"
export GOPATH="$project_dir/.task-tools/gopath"
export TMPDIR="$project_dir/.task-tools/tmp"
export GOTOOLCHAIN=local

for target in arm64-v8a:aarch64-linux-android21 armeabi-v7a:armv7a-linux-androideabi21 x86:i686-linux-android21 x86_64:x86_64-linux-android21; do
    abi=${target%%:*}
    triple=${target#*:}
    base="$tmp_dir/$abi-libclash.so"
    patch="$output_dir/$abi/libaurora_root_patch.so"
    mkdir -p "$output_dir/$abi"
    cp "$core_dir/$abi/libclash.so" "$base"
    "$toolchain/llvm-strip" --strip-unneeded "$base"
    "$toolchain/${triple}-clang" -shared -fPIC -nostdlib -Wl,-soname,libaurora_root_patch.so \
        "$project_dir/tools/root-patch-stub.c" -o "$patch"
    (cd "$project_dir/core/src/foss/golang/clash" && "$go_bin" run "$project_dir/tools/root-patch/main.go" \
        "$base" "$root_dir/$abi/libaurora_root.so" "$patch")
    rm "$base"
done

package main

import (
	"crypto/sha256"
	"encoding/binary"
	"fmt"
	"io"
	"os"

	"github.com/klauspost/compress/zstd"
)

func main() {
	if len(os.Args) != 4 {
		panic("usage: root-patch <stripped-libclash> <root-executable> <elf-container>")
	}
	base, err := os.ReadFile(os.Args[1])
	check(err)
	root, err := os.Open(os.Args[2])
	check(err)
	defer root.Close()
	rootHash := sha256.New()
	output, err := os.OpenFile(os.Args[3], os.O_APPEND|os.O_WRONLY, 0)
	check(err)
	defer output.Close()
	start, err := output.Seek(0, io.SeekEnd)
	check(err)
	encoder, err := zstd.NewWriter(output,
		zstd.WithEncoderDictRaw(0, base),
		zstd.WithEncoderLevel(zstd.SpeedBestCompression),
		zstd.WithWindowSize(1<<27),
		zstd.WithEncoderConcurrency(4))
	check(err)
	_, err = io.Copy(encoder, io.TeeReader(root, rootHash))
	check(err)
	check(encoder.Close())
	end, err := output.Seek(0, io.SeekEnd)
	check(err)
	var length [8]byte
	binary.LittleEndian.PutUint64(length[:], uint64(end-start))
	_, err = output.Write(length[:])
	check(err)
	baseHash := sha256.Sum256(base)
	_, err = output.Write(baseHash[:])
	check(err)
	_, err = output.Write(rootHash.Sum(nil))
	check(err)
	_, err = output.Write([]byte("AURPATCH"))
	check(err)
	fmt.Printf("root patch: %d bytes\n", end-start)
}

func check(err error) {
	if err != nil { panic(err) }
}

package main

/*
#include "bridge.h"
*/
import "C"

import (
	"bytes"
	"crypto/sha256"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"

	"github.com/klauspost/compress/zstd"
)

//export restoreRootCore
func restoreRootCore(basePath, patchPath, outputPath C.c_string) C.int {
	if err := restoreRootCoreFile(C.GoString(basePath), C.GoString(patchPath), C.GoString(outputPath)); err != nil {
		fmt.Fprintln(os.Stderr, "Aurora ROOT restore:", err)
		return -1
	}
	return 0
}

func restoreRootCoreFile(basePath, patchPath, outputPath string) error {
	base, err := os.ReadFile(basePath)
	if err != nil { return err }
	patch, err := os.Open(patchPath)
	if err != nil { return err }
	defer patch.Close()
	stat, err := patch.Stat()
	if err != nil { return err }
	const trailerSize = 80
	if stat.Size() < trailerSize { return errors.New("ROOT patch is incomplete") }
	trailer := make([]byte, trailerSize)
	if _, err = patch.ReadAt(trailer, stat.Size()-trailerSize); err != nil { return err }
	if string(trailer[72:]) != "AURPATCH" { return errors.New("ROOT patch signature mismatch") }
	length := int64(binary.LittleEndian.Uint64(trailer[:8]))
	if length <= 0 || length > stat.Size()-trailerSize { return errors.New("ROOT patch length is invalid") }
	baseHash := sha256.Sum256(base)
	if !bytes.Equal(baseHash[:], trailer[8:40]) { return errors.New("Installed core does not match ROOT patch") }
	section := io.NewSectionReader(patch, stat.Size()-trailerSize-length, length)
	decoder, err := zstd.NewReader(section, zstd.WithDecoderDictRaw(0, base))
	if err != nil { return err }
	defer decoder.Close()
	output, err := os.CreateTemp(filepath.Dir(outputPath), ".aurora-root-*")
	if err != nil { return err }
	defer os.Remove(output.Name())
	hash := sha256.New()
	_, copyErr := io.Copy(io.MultiWriter(output, hash), decoder)
	closeErr := output.Close()
	if copyErr != nil { return copyErr }
	if closeErr != nil { return closeErr }
	if !bytes.Equal(hash.Sum(nil), trailer[40:72]) { return errors.New("Restored ROOT core failed integrity check") }
	return os.Rename(output.Name(), outputPath)
}

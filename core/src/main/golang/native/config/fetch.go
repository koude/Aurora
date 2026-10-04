package config

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	U "net/url"
	"os"
	P "path"
	"runtime"
	"strconv"
	"strings"
	"time"

	"cfa/native/app"

	"github.com/metacubex/mihomo/adapter/provider"
	clashHttp "github.com/metacubex/mihomo/component/http"
	RB "github.com/metacubex/mihomo/rules/bundle"
)

type Status struct {
	Action              string   `json:"action"`
	Args                []string `json:"args"`
	Progress            int      `json:"progress"`
	MaxProgress         int      `json:"max"`
	DownloadedBytes     int64    `json:"downloadedBytes,omitempty"`
	TotalBytes          int64    `json:"totalBytes,omitempty"`
	SpeedBytesPerSecond int64    `json:"speedBytesPerSecond,omitempty"`
	SubUpload           *int64   `json:"subUpload,omitempty"`
	SubDownload         *int64   `json:"subDownload,omitempty"`
	SubTotal            *int64   `json:"subTotal,omitempty"`
	SubExpire           *int64   `json:"subExpire,omitempty"`
	SubUpdateInterval   *int64   `json:"subUpdateInterval,omitempty"`
}

type fetchHeader struct {
	SubscriptionUserInfo  string
	ProfileUpdateInterval string
	ContentLength         int64
}

func openUrl(ctx context.Context, url string) (io.ReadCloser, fetchHeader, error) {
	response, err := clashHttp.HttpRequest(ctx, url, http.MethodGet, http.Header{"User-Agent": {"ClashMetaForAndroid/" + app.VersionName()}}, nil)

	if err != nil {
		return nil, fetchHeader{}, err
	}

	return response.Body, fetchHeader{
		SubscriptionUserInfo:  response.Header.Get("subscription-userinfo"),
		ProfileUpdateInterval: response.Header.Get("profile-update-interval"),
		ContentLength:         response.ContentLength,
	}, nil
}

func openContent(url string) (io.ReadCloser, error) {
	return app.OpenContent(url)
}

func fetch(url *U.URL, file string, action string, args []string, progress int, maxProgress int, reportStatus func(string)) (fetchHeader, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	var reader io.ReadCloser
	var header fetchHeader
	var err error

	switch url.Scheme {
	case "http", "https":
		reader, header, err = openUrl(ctx, url.String())
	case "content":
		reader, err = openContent(url.String())
	default:
		err = fmt.Errorf("unsupported scheme %s of %s", url.Scheme, url)
	}

	if err != nil {
		return fetchHeader{}, err
	}

	defer reader.Close()

	report := func(downloaded, total, speed int64) {
		bytes, _ := json.Marshal(&Status{
			Action: action, Args: args, Progress: progress, MaxProgress: maxProgress,
			DownloadedBytes: downloaded, TotalBytes: total, SpeedBytesPerSecond: speed,
		})
		reportStatus(string(bytes))
	}
	return header, writeFile(file, reader, header.ContentLength, report)
}

func writeFile(file string, reader io.Reader, totalBytes int64, report func(downloaded, total, speed int64)) error {
	_ = os.MkdirAll(P.Dir(file), 0700)

	f, err := os.OpenFile(file, os.O_WRONLY|os.O_TRUNC|os.O_CREATE, 0600)
	if err != nil {
		return err
	}

	defer f.Close()

	lastReportAt := time.Now()
	var downloaded int64
	var lastReportedBytes int64
	buffer := make([]byte, 32*1024)
	for {
		count, readErr := reader.Read(buffer)
		if count > 0 {
			written, writeErr := f.Write(buffer[:count])
			downloaded += int64(written)
			now := time.Now()
			if now.Sub(lastReportAt) >= 200*time.Millisecond || readErr == io.EOF {
				elapsed := now.Sub(lastReportAt).Seconds()
				speed := int64(0)
				if elapsed > 0 {
					speed = int64(float64(downloaded-lastReportedBytes) / elapsed)
				}
				report(downloaded, totalBytes, speed)
				lastReportedBytes = downloaded
				lastReportAt = now
			}
			if writeErr != nil {
				err = writeErr
				break
			}
			if written != count {
				err = io.ErrShortWrite
				break
			}
		}
		if readErr != nil {
			if readErr == io.EOF && count == 0 {
				report(downloaded, totalBytes, 0)
			}
			if readErr != io.EOF {
				err = readErr
			}
			break
		}
	}
	if err != nil {
		_ = os.Remove(file)
	}

	return err
}

func parseProfileUpdateInterval(value string) (int64, bool) {
	hours, err := strconv.ParseInt(strings.TrimSpace(value), 10, 64)
	if err != nil {
		return 0, false
	}

	if hours <= 0 {
		return 0, true
	}

	interval := time.Duration(hours) * time.Hour
	if interval < 15*time.Minute {
		interval = 15 * time.Minute
	}

	return int64(interval / time.Millisecond), true
}

func reportSubscriptionInfo(header fetchHeader, reportStatus func(string)) {
	userinfo := header.SubscriptionUserInfo
	updateIntervalHeader := header.ProfileUpdateInterval
	if userinfo == "" && updateIntervalHeader == "" {
		return
	}

	status := Status{
		Action:      "SubscriptionInfo",
		Args:        []string{},
		Progress:    -1,
		MaxProgress: -1,
	}

	if userinfo != "" {
		info := provider.NewSubscriptionInfo(userinfo)
		expire := info.Expire * 1000
		status.SubUpload = &info.Upload
		status.SubDownload = &info.Download
		status.SubTotal = &info.Total
		status.SubExpire = &expire
	}

	if interval, ok := parseProfileUpdateInterval(updateIntervalHeader); ok {
		status.SubUpdateInterval = &interval
	}

	bytes, _ := json.Marshal(&status)
	reportStatus(string(bytes))
}

func FetchAndValid(
	path string,
	url string,
	force bool,
	reportStatus func(string),
) error {
	configPath := P.Join(path, "config.yaml")

	if _, err := os.Stat(configPath); os.IsNotExist(err) || force {
		url, err := U.Parse(url)
		if err != nil {
			return err
		}

		bytes, _ := json.Marshal(&Status{
			Action:      "FetchConfiguration",
			Args:        []string{url.Host},
			Progress:    -1,
			MaxProgress: -1,
		})

		reportStatus(string(bytes))

		header, err := fetch(url, configPath, "FetchConfiguration", []string{url.Host}, -1, -1, reportStatus)
		if err != nil {
			return err
		}

		reportSubscriptionInfo(header, reportStatus)
	}

	defer runtime.GC()

	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		return err
	}

	forEachProviders(rawCfg, func(index int, total int, name string, provider map[string]any, prefix string) {
		bytes, _ := json.Marshal(&Status{
			Action:      "FetchProviders",
			Args:        []string{name},
			Progress:    index,
			MaxProgress: total,
		})

		reportStatus(string(bytes))

		u, uok := provider["url"]
		p, pok := provider["path"]

		if !uok || !pok {
			return
		}

		us, uok := u.(string)
		ps, pok := p.(string)

		if !uok || !pok {
			return
		}

		if _, err := os.Stat(ps); err == nil {
			return
		}

		url, err := U.Parse(us)
		if err != nil {
			return
		}

		if prefix == RULES {
			if pib, uok := provider["path-in-bundle"]; uok {
				if pib, uok := pib.(string); uok && pib != "" {
					// actually, we don't need to extract the file here; the core will do it.
					// however, due to historical reasons, CMFA fetches provider content when loading profile,
					// so we maintain consistency with the old behavior.
					if file, err := RB.Open(pib); err == nil {
						defer file.Close()
						if err := writeFile(ps, file, -1, func(downloaded, totalBytes, speed int64) {
							bytes, _ := json.Marshal(&Status{
								Action: "FetchProviders", Args: []string{name}, Progress: index, MaxProgress: total,
								DownloadedBytes: downloaded, TotalBytes: totalBytes, SpeedBytesPerSecond: speed,
							})
							reportStatus(string(bytes))
						}); err == nil {
							return
						}
					}
				}
			}
		}

		_, _ = fetch(url, ps, "FetchProviders", []string{name}, index, total, reportStatus)
	})

	bytes, _ := json.Marshal(&Status{
		Action:      "Verifying",
		Args:        []string{},
		Progress:    0xffff,
		MaxProgress: 0xffff,
	})

	reportStatus(string(bytes))

	cfg, err := Parse(rawCfg)
	if err != nil {
		return err
	}

	destroyProviders(cfg)

	return nil
}

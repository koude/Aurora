package config

import (
	"os"
	P "path"
	"runtime"
	"strings"

	"cfa/native/app"

	"github.com/metacubex/mihomo/common/yaml"
	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/log"
)

func logDns(cfg *config.RawConfig) {
	bytes, err := yaml.Marshal(&cfg.DNS)
	if err != nil {
		log.Warnln("Marshal dns: %s", err.Error())

		return
	}

	log.Infoln("dns:")

	for _, line := range strings.Split(string(bytes), "\n") {
		log.Infoln("  %s", line)
	}
}

func UnmarshalAndPatch(profilePath string) (*config.RawConfig, error) {
	configPath := P.Join(profilePath, "config.yaml")

	configData, err := os.ReadFile(configPath)
	if err != nil {
		return nil, err
	}

	rawConfig, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		return nil, err
	}

	if err := process(rawConfig, profilePath); err != nil {
		return nil, err
	}

	return rawConfig, nil
}

// ExportRootConfiguration prepares the same imported profile for Aurora's
// standalone, privileged mihomo process. The Android VPN path keeps its own
// configuration processing unchanged.
func ExportRootConfiguration(profilePath, rootProfilePath, secret string) (string, error) {
	data, err := os.ReadFile(P.Join(profilePath, "config.yaml"))
	if err != nil {
		return "", err
	}
	raw, err := config.UnmarshalRawConfig(data)
	if err != nil {
		return "", err
	}
	if err := process(raw, rootProfilePath); err != nil {
		return "", err
	}

	raw.Tun.Enable = true
	raw.Tun.Device = "Aurora"
	raw.Tun.AutoRoute = true
	raw.Tun.AutoRedirect = true
	raw.Tun.AutoDetectInterface = true
	raw.Tun.IPRoute2TableIndex = 20222
	// Android's own per-network rules start at 10000. Match sing-tun's
	// default priority so app traffic reaches Aurora before those rules.
	raw.Tun.IPRoute2RuleIndex = 9000
	raw.Tun.DNSHijack = []string{"any:53"}
	raw.ExternalController = "127.0.0.1:19090"
	raw.ExternalControllerTLS = ""
	raw.ExternalControllerUnix = ""
	raw.Secret = secret

	data, err = yaml.Marshal(raw)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func Parse(rawConfig *config.RawConfig) (*config.Config, error) {
	cfg, err := config.ParseRawConfig(rawConfig)
	if err != nil {
		return nil, err
	}

	return cfg, nil
}

func Load(path string) error {
	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}

	logDns(rawCfg)

	cfg, err := Parse(rawCfg)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}

	// like hub.Parse()
	hub.ApplyConfig(cfg)

	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)

	runtime.GC()

	return nil
}

func LoadDefault() {
	cfg, err := config.Parse([]byte{})
	if err != nil {
		panic(err.Error())
	}

	hub.ApplyConfig(cfg)
}

package main

//#include "bridge.h"
import "C"

import (
	"context"
	"fmt"
	"strings"

	"github.com/metacubex/mihomo/component/resolver"
	M "github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/tunnel"
)

type routePreview struct {
	Target     string  `json:"target"`
	Mode       string  `json:"mode"`
	Rule       string  `json:"rule"`
	Policy     string  `json:"policy"`
	Outbound   string  `json:"outbound"`
	ResolvedIP *string `json:"resolvedIp,omitempty"`
	Error      *string `json:"error,omitempty"`
}

//export queryRoutePreview
func queryRoutePreview(target C.c_string) *C.char {
	host := strings.TrimSpace(C.GoString(target))
	result := routePreview{Target: host, Mode: tunnel.Mode().String()}
	if host == "" {
		message := "empty target"
		result.Error = &message
		return marshalJson(result)
	}

	metadata := &M.Metadata{
		NetWork: M.TCP,
		Type:    M.INNER,
		Host:    host,
		DstPort: 443,
	}
	proxies := tunnel.Proxies()

	resolveIP := func() {
		if metadata.Resolved() {
			return
		}
		ctx, cancel := context.WithTimeout(context.Background(), resolver.DefaultDNSTimeout)
		defer cancel()
		if ip, err := resolver.ResolveIP(ctx, host); err == nil {
			metadata.DstIP = ip
			value := ip.String()
			result.ResolvedIP = &value
		}
	}

	helper := M.RuleMatchHelper{
		ResolveIP:   resolveIP,
		FindProcess: func() {},
		CheckPassRule: func(adapterName string) bool {
			proxy, ok := proxies[adapterName]
			if !ok {
				return false
			}
			for current := M.ProxyAdapter(proxy); current != nil; current = current.Unwrap(metadata, false) {
				if current.Type() == M.PassRule {
					return true
				}
			}
			return false
		},
	}

	switch tunnel.Mode() {
	case tunnel.Direct:
		result.Rule = "DIRECT"
		result.Policy = "DIRECT"
		result.Outbound = "DIRECT"
	case tunnel.Global:
		result.Rule = "GLOBAL"
		result.Policy = "GLOBAL"
		result.Outbound = unwrapOutbound(proxies["GLOBAL"], metadata)
	default:
		for _, configuredRule := range tunnel.Rules() {
			matchRule := configuredRule
			if wrapper, ok := configuredRule.(M.RuleWrapper); ok {
				if wrapper.IsDisabled() {
					continue
				}
				matchRule = wrapper.Unwrap()
			}

			matched, policy := matchRule.Match(metadata, helper)
			if !matched {
				continue
			}

			proxy, ok := proxies[policy]
			if !ok {
				continue
			}
			if containsAdapterType(proxy, metadata, M.Pass) {
				continue
			}

			result.Rule = formatRule(configuredRule)
			result.Policy = policy
			result.Outbound = unwrapOutbound(proxy, metadata)
			break
		}

		if result.Policy == "" {
			result.Rule = "FINAL"
			result.Policy = "DIRECT"
			result.Outbound = "DIRECT"
		}
	}

	if result.Outbound == "" {
		result.Outbound = result.Policy
	}
	return marshalJson(result)
}

func formatRule(rule M.Rule) string {
	if payload := rule.Payload(); payload != "" {
		return fmt.Sprintf("%s(%s)", rule.RuleType().String(), payload)
	}
	return rule.RuleType().String()
}

func unwrapOutbound(proxy M.Proxy, metadata *M.Metadata) string {
	if proxy == nil {
		return ""
	}
	name := proxy.Name()
	for current := proxy.Unwrap(metadata, false); current != nil; current = current.Unwrap(metadata, false) {
		name = current.Name()
	}
	return name
}

func containsAdapterType(proxy M.Proxy, metadata *M.Metadata, adapterType M.AdapterType) bool {
	for current := M.ProxyAdapter(proxy); current != nil; current = current.Unwrap(metadata, false) {
		if current.Type() == adapterType {
			return true
		}
	}
	return false
}

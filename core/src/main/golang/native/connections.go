package main

//#include "bridge.h"
import "C"

import (
	appTunnel "cfa/native/tunnel"
	"github.com/metacubex/mihomo/tunnel/statistic"
)

type connectionSummary struct {
	ID          string   `json:"id"`
	Host        string   `json:"host"`
	Process     string   `json:"process"`
	Network     string   `json:"network"`
	Destination string   `json:"destination"`
	Rule        string   `json:"rule"`
	RulePayload string   `json:"rulePayload"`
	Chains      []string `json:"chains"`
	Uploaded    int64    `json:"uploaded"`
	Downloaded  int64    `json:"downloaded"`
	StartedAt   int64    `json:"startedAt"`
}

//export queryConnections
func queryConnections() *C.char {
	connections := make([]connectionSummary, 0)
	statistic.DefaultManager.Range(func(tracker statistic.Tracker) bool {
		info := tracker.Info()
		metadata := info.Metadata
		host := metadata.RuleHost()
		if host == "" {
			host = metadata.DstIP.String()
		}
		connections = append(connections, connectionSummary{
			ID:          tracker.ID(),
			Host:        host,
			Process:     metadata.Process,
			Network:     metadata.NetWork.String(),
			Destination: metadata.RemoteDst,
			Rule:        info.Rule,
			RulePayload: info.RulePayload,
			Chains:      []string(tracker.Chains()),
			Uploaded:    info.UploadTotal.Load(),
			Downloaded:  info.DownloadTotal.Load(),
			StartedAt:   info.Start.UnixMilli(),
		})
		return true
	})
	return marshalJson(connections)
}

//export closeConnection
func closeConnection(id C.c_string) C.int {
	tracker := statistic.DefaultManager.Get(C.GoString(id))
	if tracker == nil {
		return 0
	}
	if err := tracker.Close(); err != nil {
		return 0
	}
	return 1
}

//export closeAllConnections
func closeAllConnections() {
	appTunnel.CloseAllConnections()
}

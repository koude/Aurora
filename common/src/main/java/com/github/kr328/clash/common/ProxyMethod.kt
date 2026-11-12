package com.github.kr328.clash.common

/** The mutually exclusive ways Aurora exposes or captures proxy traffic. */
enum class ProxyMethod {
    Vpn,
    Proxy,
    Root,
}

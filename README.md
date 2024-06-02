SCION-IP Translator for Android PoC
===================================

Based on [ToyVpn](https://android.googlesource.com/platform/development/+/refs/heads/main/samples/ToyVpn?autodive=0%2F)
sample using [Android VPN service](https://developer.android.com/develop/connectivity/vpn#service) to create the TUN interface.

Uses [JPAN](https://github.com/scionproto-contrib/jpan) for packet parsing and to connect to a SCION daemon.

TODO:
- Configuration UI
- Support for end host bootstrapper
- More efficient main loop
- More compatible packet parsing
- AS-internal connections
- Handling of ICMP and SCMP
- Support for dispatcherless border router

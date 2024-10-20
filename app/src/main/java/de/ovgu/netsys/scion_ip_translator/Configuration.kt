package de.ovgu.netsys.scion_ip_translator

data class Configuration(
    var bindAddress: String = "",
    var endhostPort: String = "30041",
    var bootstrapServer: String = "192.168.200.253:8041",
    var connectToDaemon: Boolean = false,
    var remoteDaemon: String = "192.168.200.253:30255",
    var overrideDns: Boolean = true,
    var dnsServer: String = "1-0:0:fc02,10.128.0.1",
)
package de.ovgu.netsys.scion_ip_translator

data class Configuration(
    var bindAddress: String = "192.168.1.100",
    var endhostPort: String = "30041",
    var bootstrapServer: String = "141.44.25.151:8041",
    var connectToDaemon: Boolean = true,
    var remoteDaemon: String = "192.168.1.112:30255",
)
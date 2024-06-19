package de.ovgu.netsys.scion_ip_translator

/**
 * Singleton object to store the configuration of the application.
 */
object ConfigurationManager {
    @JvmStatic
    var configuration: Configuration = Configuration()
}

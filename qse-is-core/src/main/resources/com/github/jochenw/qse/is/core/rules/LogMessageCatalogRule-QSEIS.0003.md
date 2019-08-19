### The LogMessageCatalogRule

The WxLog package is one of the most useful, and popular packages, that are avilable as an addon for the
webMethods IS. It is typically used in conjunction with a config file (like **PACKAGE_NAME/config/log-messages.xml**). This config file provides the so-called "message catalog", and
the plugin checks, whether that catalog is properly used.

#### The error code QSEIS.0003

This error code is about using the service **wx.log.pub:logMessageFromCatalog**. This service reads an
entry from the message catalog, which is specified by the parameters componentKey, facilityKey, and
messageKey. The catalog entry provides a message severity (**TRACE**, **DEBUG**, **INFO**, **WARN**,
**ERROR**, or **FATAL**, the implicit severity), which will be used when logging the catalog message.
In theory, it is possible to override the severity (by specifying an explicit severity) when invoking
the log service. However, doing so is discouraged, and the plugin flags explicit severities with an
error code **QSEIS.0003**.

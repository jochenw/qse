### The LogMessageCatalogRule

The WxLog package is one of the most useful, and popular packages, that are avilable as an addon for the
webMethods IS. It is typically used in conjunction with a config file (like **PACKAGE_NAME/config/log-messages.xml**). This config file provides the so-called "message catalog", and
the plugin checks, whether that catalog is properly used.

#### The error code QSEIS.0004

This error code is about using the service **wx.log.pub:logMessageFromCatalog**. This service reads an
entry from the message catalog, which is specified by the parameters componentKey, facilityKey, and
messageKey. For every invocation of this service, the rule provides a lookup in the message catalog,
using the input parameters from the pipeline as keys. If that lookup fails (or, in other words: If there
is no matching entry in the log message catalog with the given component key, facility key, and message
key, then that error is flagged as an issue with error code **QSEIS.0004**.
 

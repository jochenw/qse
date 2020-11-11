# QSE IS (Core)

QSE IS is a quick scanning engine (qse) for the webMethods Integration Server (IS), providing some rather limited static code analyzations.

Historically, it was written as a successor to a non-public tool from a customer project. We had the idea, to use that predecessor as an analyzer in [Sonarqube](https://www.sonarqube.org/).
However, that idea was abandoned quickly, as the design limitations of the predecessor became way too obvious. (Or, more precisely: The design could not be brought to fit with the
rather advanced design of Sonarqube, and it's scanners.)

Internally, it is completely event driven, which allows for extremely good performance. It supports a plugin framework, that should easily allow custom analyzations. The event driven
design is also, what would be required within SonarQube.

As of this writing, the following analyzers (so-called rules) are available:

    - AuditSettingsRule - Validates trigger configurations for possible pain points, like number of parallel threads, etc.
    - DependencyCheckingRule - Validates, whether service references are only local, or within a limited range of permitted "tool packages".
    - ForbiddenServicesRule - Validates, whether some services are used, that are known to be possible source of trouble. (Might as well
                              call this the "DeprecatedServicesRule".)
    - LogMessageCatalogRule - Verifies, whether the message catalog is properly used. (A log message *must* be registered in the message
                              catalog, so that it can be properly localized.
    - PipelineDebugRule - Verifies service configurations for possible pain points, like debugging modes, etc., that should onyl be
                          present in a development environment.
    - StartupServiceRule - Validates the configuration of startup/shutdown services: Those must be located locally, and (obviously) should
                           exist at all.
 
 

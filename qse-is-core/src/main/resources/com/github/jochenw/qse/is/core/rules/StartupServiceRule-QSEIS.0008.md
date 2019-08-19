### The StartupServiceRule

It id a reasonable requirement, that an IS packages startup-, and shutdown service(s) are part of the respective package, or package group. This is, what's verified by this rule. No configuration parameters
are supported, or required, apart from the basic rule attributes, like severity, id, etc.
So, our example rule looks almost trivial:

    <rule enabled="true" class="com.github.jochenw.qse.is.core.rules.StartupServiceRule"
          severity="ERROR"/>
    
#### The error code QSEIS.0008

The error code **QSEIS.0008** is issued, if a package declares a startup service, but that service isn't found
in the local package, or package group.
 
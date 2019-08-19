### The ForbiddenServicesRule

This rule is used to stop using services, that are obsolete, deprecated, or even forbidden, for whatever reason.
The rule is being configured through a list parameter, called 'serviceNames'. For example, this could look like the following:

        <listProperty name="serviceNames">
          <value>pub.list:appendToDocumentList</value>
          <value>pub.list:appendToStringList</value>
        </listProperty>

The example declares two services as forbidden, namely **pub.list:appendToDocumentList**, and **pub.list:appendToStringList**.
Both services are known to cause serious performance problems for large lists, because they will create a new array for
every element, that's being added to the list.

How do we specify, whether a service is obsolete, deprecate, or even forbidden? We do so by choosing an appropriate severity for
the rule. The suggested values are:

| Status     | Severity |
| ----------:| --------:|
| Obsolete   | INFO     |
| Deprecated | WARN     |
| Forbidden  | ERROR    |

If there is more than one status to configure, then you can specify the rule more than once. If you do so, however, then you
must select a unique rule id for every specification, thus overriding the default rule id ("ForbiddenServicesRule").

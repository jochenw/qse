### The AuditSettingsRule

This rule can be used to validate, whether the audit settings for services are meeting the expectations. Unsuitable audit
 settings can be a reason for performance problems in production environments.
 
 By default, the rule operates on all services, which is most likely not, what you want. In a typical use case scenario,
  the rule is configured with a filter, that includes, or excludes services from the rules scope. An example of such a filter
   would be:
   
    <listProperty name="includedServices">
      <!-- Public servives (namespace below 'pub' -->
      <value>^.*\\.pub(\\..*\\:|\\:).*$</value>
      <!-- Web servives (namespace below 'ws.provider' -->
      <value>^.*\\.ws\\.provider(\\..*\\:|\\:).*$</value>
      <!-- REST servives (named _get, _post, _put, or _delete) -->
      <value>^.*\\:(_get|_post|_put|_delete)$</value>
    </listProperty>
    <listProperty name=\"excludedServices\">
      <!-- Nothing to exclude -->
    </listProperty>

#### Validating the "Log on" setting

The parameter expectedLogOnValue configures, what's valid for the 'Log on' setting.
The property's value is a comma separated list of the following integers:

   - 0 = Error only
   - 1 = Error, and success
   - 2 = Error, success, and start

 

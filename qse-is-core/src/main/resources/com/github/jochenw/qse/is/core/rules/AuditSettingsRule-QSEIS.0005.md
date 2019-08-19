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

#### Validating the "Enable auditing" setting

The parameter expectedEnableAuditingValue configures, what's valid for the 'Enable auditing' setting.
 The property's value is a comma separated list of the following integers:
 
   - 0 = Never
   - 1 = Always
   - 2 = When top-level service only
 

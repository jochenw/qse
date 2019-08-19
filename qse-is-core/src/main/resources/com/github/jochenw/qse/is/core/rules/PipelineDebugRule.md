### The PipelineDebugRule

This rule is used to suppress undesirable values for a services "Pipeline Debug" setting.
Using that rule will look like follows:

      <rule enabled="true" class="com.github.jochenw.qse.is.core.rules.PipelineDebugRule"
            severity="ERROR">
        <property name="permittedValues">0,1</property>
      </rule>

The crucial point is the selection of the parameter "permittedValues", a comma separated list of integer values.
The problem here is, that these integer values aren't constant between webMethods versions. So, on some versions
you might find 0=None, 1=Save, 2=Restore (Override), and 3=Restore (Merge). On other versions, this might be
1=None, 2=Save, 3=Restore (Override), and 4=Restore (Merge).

It is recommended, that you find out for yourself, as follows: Select a flow service, say the **com.foo:MyService**,
in **FooPackage**, and open the associated node.ndf. In the example, this would be
**<IS_HOME>/instances/default/packages/FooPackage/ns/com/foo/MyService/node.ndf**, and find a line, which looks like

    <value name="pipeline_option">0</value>

Now, start modifying the "Pipeline debug" setting on the service. After each modification, save the service, reload the
file, and inspect the integer value in the above line. Assuming, that you want to permit only the value "None", and
your experiences demonstrate, that "None" is being represented by the value 0, then set the parameter "permittedValue" to 0.


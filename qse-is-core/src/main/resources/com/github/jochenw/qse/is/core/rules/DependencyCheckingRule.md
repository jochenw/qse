### The DependencyCheckingRule

This rule addresses the necessity for proper declaration of dependencies.
It is designed for the architecture pattern, that an IS package, or a group of IS packages
should basically be self contained, except for a limited, and controlled number of common base
packages, which are shared by all others. Furthermore, it assumes, that the services of such a
shared package are present under one common namespace (one per package, that is).

The rule must be configured through a property 'dependencySpecifications', which will look like this:

    <listProperty name='dependencySpecifications'>
      <value>WxConfig:^wx.config[\\.\\:].*</value>
      <value>WxLog:^wx.log[\\.\\:].*</value>
    </listProperty>


In the example, there are two shared packages: WxConfig, with namespace wx.config, and WxLog, with namespace
wx.log. Thus, if an IS package is using a service, like wx.config.pub:getValue, then that package must have
a dependency on WxConfig in its manifest. (The dependency version is being ignored.) Likewise, if a package
is using a service in the wx.log namespace, then it must have a dependency on the WxLog package.

#### Transitive dependencies

Transitive dependencies are not supported: For example, there is no way to determine, that the package
**FooCommonUtils** has a dependency on **WxConfig**, so that a dependency on the former eliminates a
dependency on the latter. However, you might change your dependency specification, as follows:

      <value>FooCommonUtils|WxConfig:^wx.config[\\.\\:].*</value>

This means, that a dependency on either **FooCommonUtils**, or **WxConfig** is sufficient, if a service in
the wx.config namespace is used.

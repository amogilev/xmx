== XMX ==

XMX is a tool for managing your Java application. It may be thought of as a "better JMX".
XMX requires no modifications of your code; you select which classes are to manage by
specifying them in the configuration file. Moreover, all 'service's, 'manager's etc. are 
managed by default.

XMX provides both the Web UI for advanced managing of the selected classes, and the JMX 
bridge to use in your favorite JMX viewer, like 'jconsole'. (The JMX bridge is disabled 
by default, by may be easily enabled in the configuration).

Additionally, XMX provides a convenient Web viewer for all Spring contexts and beans, and 
also all resolved Spring placeholders and properties in your application. The Spring beans
may be managed in the same way as other selected classes, besides that they are managed
automatically and require no specification in the configuration file.

XMX is a pure Java library, so it is available on all platforms, including Windows, Linux 
and MacOS.

== Getting Started ==

In order to enable XMX for your Java application, do the following:
- Unzip xmx-distribution.zip to any folder, e.g. /opt/lib/xmx;
- Add XMX as a Java agent to the VM options of your application, e.g.
   java -javaagent:/opt/lib/xmx/bin/xmx-agent.jar -cp ... YourApp
- Start your application

The Web management console will be automatically available at http://localhost:8081 in
few seconds after the start of your application.

== Configuration ==

After the first use of XMX, the configuration file is automatically created:
  ${user.home}/.xmx/xmx.ini

You can use it to change the default behaviour, e.g. enable or disable the web console, the 
JMX bridge, the port for the web console, which classes are managed etc.

For example, in order to add a new class into the managed set, just add the new section like

---
[App=*; Class=org.example.MyClassToManage]
Managed=true
---

The section names supports simple star-patterns. For example, you can make all classes in a 
package managed:

---
[App=*; Class=org.example.myservices.*]
Managed=true
---

== Web UI Features ==

In the Web UI, you can do the following:
- list all instances of all managed classes;
- for each instance, view values of all fields as JSON or toString;
- set a new value for any field (primitive values and JSON are accepted);
- run any method, specifying primitive values or JSON as the arguments.

By default, the web UI is available at http://localhost:8081, but the port number may be 
changed in the configuration file

== Spring MX Web UI Features ==

An advanced viewer for all Spring contexts and beans is available at 
http://localhost:8081/smx/. It contains an interactive visual graph with all found Spring 
contexts and beans. Please use a mouse right-button clicks to call a context menu, which 
provides further available actions, like 
- viewing details of the bean definition;
- viewing details of the bean instance (which allows managing action like changing fields 
and calling methods);
- displaying resolved placeholders and properties related to a given context.


== AOP Advices ==

XMX provides a possibility to specify custom AOP-like "advices" to be executed before or after 
some "target" methods, with full access to arguments, return value or thrown exception, called
method and object etc. In order to do that:
- create an advice class and advice methods, using annotations provided in `xmx-aop-api.jar`
(available at 'XMX_HOME/lib/'). Make sure the advice methods are marked by the @Advice annotation;
- put the JAR file with your advices into 'XMX_HOME/lib/advices/';
- in 'xmx.ini', add the method-level configuration section like

---
[App=*; Class=org.example.MyClassToManage; Method="foo(int,String)"]
Advices=yourAdvices.jar:org.example.YourAdvicesClass
---

In this example, all advice methods found in `YourAdvicesClass` will be bound to the target method
`foo`. It is also possible to bound the advices to the multiple target methods, using method
patterns which match the multiple methods, like
  "*"               - matches all methods
  "public *"        - matches all public methods
  "!static *"       - matches all non-static methods
  "String get*()"   - matches all String-returning getters

The detailed specification of the method patterns with extra examples may be found in Wiki:
https://bitbucket.org/amogilev/xmx/wiki/xmx.ini%20configuration%20file%20format

== How to build  ==

Aside of using the binary distribution, you can build it yourself from the unmodified sources.
Just run 'mvn clean package' in the root directory of the source package, and find the resulting
distribution zip file in 'xmx-distribution/target/'

NOTE: modifying the sources is not allowed by the license! 

== Contacts ==

Please feel free to contact me at mailto://amogilev@gmail.com for reporting any issue or suggestion.

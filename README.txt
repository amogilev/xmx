== XMX ==

XMX is a tool for managing your Java application. It may be thought of as a "better JMX".
XMX requires no modifications of your code; you select which classes are to manage by
specifying them in the configuration file. Moreover, all 'service's, 'manager's etc. are 
managed by default.

XMX provides both the Web UI for advanced managing of the selected classes, and the JMX 
bridge to use in your favorite JMX viewer, like 'jconsole'.

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

== Features ==

In the Web UI, you can do the following:
- list all instances of all managed classes;
- for each instance, view values of all fields as JSON or toString;
- set a new value for any field (primitive values and JSON are accepted);
- run any method, specifying primitive values or JSON as the arguments.

== How to build  ==

Aside of using the binary distribution, you can build it yourself from the unmodified sources.
Just run 'mvn clean package' in the root directory of the source package, and find the resulting
distribution zip file in 'xmx-distribution/target/'

NOTE: modifying the sources is not allowed by the license! 

== Contacts ==

Please feel free to contact me at mailto://amogilev@gmail.com for reporting any issue or suggestion.

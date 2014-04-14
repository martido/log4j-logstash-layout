A simple log4j layout that formats log statements as required by logstash. Currently there's only an implementation for version 1 of the logstash format.

#### Example

```
{
   "@timestamp" : "2014-04-14T12:33:50.192Z",
   "@version" : 1,
   "host" : "localhost",
   "level" : "ERROR",
   "message" : "Oops!",
   "logger" : "de.martido.log4jes.Tester",
   "file" : "Tester.java",
   "class" : "de.martido.log4jes.Tester",
   "method" : "main",
   "line" : "17",
   "exception" : {
      "class" : "java.lang.NullPointerException",
      "message" : "null",
      "stackTrace" : "java.lang.NullPointerException\n
        \tat java.lang.String.<init>(String.java:556)\n
        \tat de.martido.log4jes.Tester.main(Tester.java:15)"
   }
}
```
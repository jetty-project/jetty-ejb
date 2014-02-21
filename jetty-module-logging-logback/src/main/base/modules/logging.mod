[depend]
resources

[lib]
lib/logging/*.jar

[xml]
etc/jetty-logging.xml

[files]
# Slf4J 1.7.6
http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.6/slf4j-api-1.7.6.jar:lib/logging/slf4j-api-1.7.6.jar
http://central.maven.org/maven2/org/slf4j/jcl-over-slf4j/1.7.6/jcl-over-slf4j-1.7.6.jar:lib/logging/jcl-over-slf4j-1.7.6.jar
http://central.maven.org/maven2/org/slf4j/jul-to-slf4j/1.7.6/jul-to-slf4j-1.7.6.jar:lib/logging//jul-to-slf4j-1.7.6.jar
http://central.maven.org/maven2/org/slf4j/log4j-over-slf4j/1.7.6/log4j-over-slf4j-1.7.6.jar:lib/logging/log4j-over-slf4j-1.7.6.jar
# Logback 1.1.1
http://central.maven.org/maven2/ch/qos/logback/logback-core/1.1.1/logback-core-1.1.1.jar:lib/logging/logback-core-1.1.1.jar
http://central.maven.org/maven2/ch/qos/logback/logback-classic/1.1.1/logback-classic-1.1.1.jar:lib/logging/logback-classic-1.1.1.jar
# Directory for output
logs/

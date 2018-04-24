FROM openjdk:8
RUN apt-get install -y ca-certificates curl \ 
	&& sed -i s/#networkaddress.cache.ttl=-1/networkaddress.cache.ttl=30/ ${JAVA_HOME}/jre/lib/security/java.security 

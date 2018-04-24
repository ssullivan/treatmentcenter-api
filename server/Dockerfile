FROM openjdk:8-alpine
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
RUN apk --no-cache update && \ 
    apk --no-cache upgrade && \
    apk --no-cache add \
	ca-certificates \
	curl \
    && sed -i s/#networkaddress.cache.ttl=-1/networkaddress.cache.ttl=30/ ${JAVA_HOME}/jre/lib/security/java.security 

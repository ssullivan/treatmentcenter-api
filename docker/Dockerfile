FROM openjdk:8-alpine
ARG UID=7999
ARG GID=7999

ENV JAVA_OPTS="-server -XX:+UseG1GC -Dfile.encoding=UTF-8 -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMPercentage=80.0 -XX:InitialRAMPercentage=80.0"
RUN apk --no-cache update \
    && apk --no-cache upgrade \
    && apk --no-cache add \
	ca-certificates \
	curl \
        shadow \
        su-exec \
        dos2unix \
    && sed -i s/#networkaddress.cache.ttl=-1/networkaddress.cache.ttl=30/ ${JAVA_HOME}/jre/lib/security/java.security \
    && addgroup -g $GID app_user \
    && adduser -D -s /bin/false -H -u $UID -G app_user app_user

WORKDIR /usr/share/ca-certificates/
RUN curl -O https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem && \
    curl -O https://s3.amazonaws.com/rds-downloads/rds-ca-2015-root.pem && \
    curl -O https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem && \
    update-ca-certificates && \
    keytool -noprompt -import -trustcacerts -alias aws-rds -file rds-combined-ca-bundle.pem -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit && \
    keytool -noprompt -import -trustcacerts -alias rds-ca-2019-root.pem -file rds-ca-2019-root.pem -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit && \
    keytool -noprompt -import -trustcacerts -alias rds-ca-2015-root.pem -file rds-ca-2015-root.pem -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit


WORKDIR /

COPY docker-entrypoint.sh /
RUN dos2unix /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]


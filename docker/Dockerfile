FROM openjdk:8-alpine
ARG UID=7999
ARG GID=7999

ENV JAVA_OPTS="-server -XX:+UseG1GC -Dfile.encoding=UTF-8 -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMPercentage=80.0 -XX:InitialRAMPercentage=80.0"

COPY docker-entrypoint.sh /
COPY import-rds-ca.sh /bin/import-rds-ca

RUN apk --no-cache update \
    && apk --no-cache upgrade \
    && apk --no-cache add \
	      ca-certificates \
	      curl \
        shadow \
        su-exec \
        dos2unix \
        coreutils \
        perl \
        openssl \
    && sed -i s/#networkaddress.cache.ttl=-1/networkaddress.cache.ttl=30/ ${JAVA_HOME}/jre/lib/security/java.security \
    && addgroup -g $GID app_user \
    && adduser -D -s /bin/false -H -u $UID -G app_user app_user && \
    dos2unix /docker-entrypoint.sh && \
    dos2unix /bin/import-rds-ca && \
    chmod ugo+rx docker-entrypoint.sh && \
    (chmod ugo+rx /bin/import-rda-ca || true)


WORKDIR /usr/share/ca-certificates/
RUN curl -O https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem && \
    curl -O https://s3.amazonaws.com/rds-downloads/rds-ca-2015-root.pem && \
    curl -O https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem && \
    update-ca-certificates && \
    keytool -noprompt -import -trustcacerts -alias rds-ca-2019-root.pem -file rds-ca-2019-root.pem -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit && \
    keytool -noprompt -import -trustcacerts -alias rds-ca-2015-root.pem -file rds-ca-2015-root.pem -keystore /etc/ssl/certs/java/cacerts -keypass changeit -storepass changeit && \
    import-rds-ca && \
    apk --no-cache del perl openssl dos2unix coreutils


WORKDIR /

ENTRYPOINT ["/docker-entrypoint.sh"]


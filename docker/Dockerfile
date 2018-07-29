FROM openjdk:8-alpine
ARG UID=7999
ARG GID=7999

ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
RUN apk --no-cache update \
    && apk --no-cache upgrade \
    && apk --no-cache add \
	ca-certificates \
	curl \
        shadow \
        su-exec \
    && sed -i s/#networkaddress.cache.ttl=-1/networkaddress.cache.ttl=30/ ${JAVA_HOME}/jre/lib/security/java.security \
    && addgroup -g $GID app_user \
    && adduser -D -s /bin/false -H -u $UID -G app_user app_user


COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]

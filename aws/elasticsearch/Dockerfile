FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4
ENV REGION=us-east-1
COPY --chown=elasticsearch:elasticsearch elasticsearch.yml /usr/share/elasticsearch/config/
WORKDIR /usr/share/elasticsearch
RUN bin/elasticsearch-plugin install discovery-ec2 && \ 
	bin/elasticsearch-plugin install repository-s3 && \
	sed -e '/^-Xm/s/^/#/g' -i /usr/share/elasticsearch/config/jvm.options

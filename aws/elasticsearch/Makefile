version = $(shell cat version.txt)

image = aws-elasticsearch-oss
registry = 016640895912.dkr.ecr.us-east-1.amazonaws.com

vtag = $(image):$(version)
ltag = $(image):latest

build:
	docker build -t $(vtag) .
	docker tag $(vtag) $(registry)/$(vtag)
	docker tag $(vtag) $(registry)/$(ltag)

push: build
	docker push $(registry)/$(ltag)

OS := $(shell uname)

postgresql:
	printf "$'\nnuxeo:\n mongodb:\n  deploy: false" >> values.yaml
	printf "$'\nnuxeo:\n postgresql:\n  deploy: true" >> values.yaml
mongodb:
	printf "$'\nnuxeo:\n postgresql:\n  deploy: false" >> values.yaml	
	printf "$'\nnuxeo:\n mongodb:\n  deploy: true" >> values.yaml		
preview: 
ifeq ($(OS),Darwin)
	sed -i "" -e "s/version:.*/version: $(PREVIEW_VERSION)/" Chart.yaml
	sed -i "" -e "s/version:.*/version: $(PREVIEW_VERSION)/" ../*/Chart.yaml
	sed -i "" -e "s/tag: .*/tag: $(PREVIEW_VERSION)/" values.yaml
else ifeq ($(OS),Linux)
	sed -i -e "s/version:.*/version: $(PREVIEW_VERSION)/" Chart.yaml
	sed -i -e "s/version:.*/version: $(PREVIEW_VERSION)/" ../*/Chart.yaml
	sed -i -e "s|repository: .*|repository: $(DOCKER_REGISTRY)\/nuxeo-sandbox\/nuxeo-notification-stream|" values.yaml
	sed -i -e "s/tag: .*/tag: $(PREVIEW_VERSION)/" values.yaml
else
	echo "platfrom $(OS) not supported to release from"
	exit -1
endif
	jx step helm build

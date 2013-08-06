jar:
	git submodule init
	git submodule update
	cd library/volley && ant jar
	cp library/volley/bin/volley.jar library/libs
	cd library && ant jar

glide-minus-volley:
	cd library && ant glide-minus-volley


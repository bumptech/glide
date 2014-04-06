jar:
	git submodule init
	git submodule update
	cd library/volley && ant clean && ant jar
	cp library/volley/bin/volley.jar library/libs
	cd library && ant clean && ant jar

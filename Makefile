jar:
	git submodule init
	git submodule update
	cd third_party/gif_decoder && ant clean && ant jar
	cp third_party/gif_decoder/bin/gifdecoder*.jar library/libs
	cd third_party/volley/volley && ant clean && ant jar
	cp third_party/volley/volley/bin/volley.jar library/libs
	cd library && ant clean && ant jar

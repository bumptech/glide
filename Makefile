jar:
	git submodule init
	git submodule update
	cp -f third_party/disklrucache/disklrucache*.jar library/libs
	cd third_party/gif_decoder && ant clean && ant jar
	cp -f third_party/gif_decoder/bin/gifdecoder*.jar library/libs
	cd third_party/volley/volley && ant clean && ant jar
	cp -f third_party/volley/volley/bin/volley.jar library/libs
	cd library && ant clean && ant jar

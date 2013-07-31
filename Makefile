setup:
	git submodule init
	git submodule update
	cd library/volley && ant jar
	cp library/volley/bin/volley.jar library/libs

update-ant: setup
	android update project --path .. --library glide/library

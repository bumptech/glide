setup:
	git submodule init
	git submodule update
	echo android.library=true >> library/volley/project.properties
	android update project --path .. --library glide/library

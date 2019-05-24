all: portrait landscape

portrait:
	curl --location https://source.unsplash.com/random/1200x1600 --output ./Portrait.jpg
	bash -c "cd generator && ./generate.rb ../Portrait.jpg"
	rm -f ./Portrait.jpg

landscape:
	curl --location https://source.unsplash.com/random/1600x1200 --output ./Landscape.jpg
	bash -c "cd generator && ./generate.rb ../Landscape.jpg"
	rm -f ./Landscape.jpg

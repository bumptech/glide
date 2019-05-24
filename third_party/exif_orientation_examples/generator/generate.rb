#!/usr/bin/env ruby

# Make sure to 'brew install gs' before running this.

require 'RMagick'
require 'tempfile'

if ARGV.length != 1
  abort "Usage: #{$PROGRAM_NAME} /path/to/image"
end

# Make sure the file exists.
source = ARGV[0]
abort "Error: File '#{source}' not found" unless File.exist?(source) && File.file?(source)

# Copy it to the temp directory.
path = Tempfile.new('to-convert').path
FileUtils.cp source, path

# Make sure it's an image.
image = begin
  Magick::Image::read(path).first
rescue Magick::ImageMagickError
  abort "Error: File '#{source}' does not appear to be an image."
end

# Make sure exiftool and convert are available.
abort 'Error: The exiftool command does not appear to be available' if `which exiftool` == ''
abort 'Error: The convert command does not appear to be available' if `which convert` == ''
abort 'Error: the input file must be a JPEG' unless image.format == 'JPEG'

# Decide where we'll put the output.
dest_folder    = File.dirname(source)
dest_file_base = File.basename(source, '.*')
dest_extention = File.extname(source)

# Strip all exif data.
`exiftool -all= #{path}`

# Strip color profile info.
FileUtils.cp path, "#{path}.convert"
`convert #{path}.convert +profile "*" #{path}`
FileUtils.rm_f "#{path}.convert"

# Decide on a suitable font size.
dimension = [image.rows, image.columns].max
font_size = dimension / 20

# Add top / right / bottom / left text.
text              = Magick::Draw.new
text.font_family  = 'helvetica'
text.pointsize    = font_size
text.fill         = 'white'
text.stroke       = 'black'
text.stroke_width = 1
edge_padding      = font_size / 4

text.annotate(image, 0, 0, 0, edge_padding, 'top') do
  self.gravity = Magick::NorthGravity
end

text.annotate(image, 0, 0, 0, edge_padding, 'bottom') do
  self.gravity = Magick::SouthGravity
end

text.annotate(image, 0, 0, edge_padding, 0, 'right') do
  self.gravity = Magick::EastGravity
end

text.annotate(image, 0, 0, edge_padding, 0, 'left') do
  self.gravity = Magick::WestGravity
end

transformations = [
  {
    exif_tag:         1,
    rotation_degrees: 0,
    flop:             false,
  },
  {
    exif_tag:         2,
    rotation_degrees: 0,
    flop:             true,
  },

  {
    exif_tag:         3,
    rotation_degrees: 180,
    flop:             false,
  },
  {
    exif_tag:         4,
    rotation_degrees: 180,
    flop:             true,
  },
  {
    exif_tag:         5,
    rotation_degrees: -90,
    flop:             true,
  },
  {
    exif_tag:         6,
    rotation_degrees: -90,
    flop:             false,
  },
  {
    exif_tag:         7,
    rotation_degrees: 90,
    flop:             true,
  },
  {
    exif_tag:         8,
    rotation_degrees: 90,
    flop:             false,
  },
]

transformations.each do |t|
  tmp_image = image.dup

  # Add centered text displaying the orientation tag number.
  text.annotate(tmp_image, 0, 0, 0, 0, t[:exif_tag].to_s) do
    self.gravity   = Magick::CenterGravity
    text.pointsize = font_size * 2
  end

  # Rotate and transform the image.
  tmp_image.flop! if t[:flop]
  tmp_image.rotate! t[:rotation_degrees] if t[:rotation_degrees] != 0
  out_path = File.join(dest_folder, "#{dest_file_base}_#{t[:exif_tag]}#{dest_extention}")
  tmp_image.write(out_path)

  # Set the EXIF Orientation tag.
  `exiftool -overwrite_original -orientation=#{t[:exif_tag]} -n #{out_path}`
end

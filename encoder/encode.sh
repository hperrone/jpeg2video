#!/bin/sh

################################################################################
### Encodes a sequence of images as an MPEG-DASH video stream using ffmpeg.
#
# The sequence of images must be placed in /jobs_in/$1 and the files must have
# the .jpg extension.
#
# The output files are placed in /jobs_out/$1. The dash manifest is named 
# stream.mpd. Also the first frame is copied to the output directory named as
# thumbnail.jpg.
#
# Once the encoding is completed, the input directory is deleted.
# 
# Usage:
#    encode.sh <dir_name> [frame_rate]
#
# Argumnents:
#    <dir_name>: sub-directory within /jobs_in containing the image sequence to
#                be processed. The same sub-directory name will be used under
#                /jobs_out where the output stream files will be placed.
#    [frame_rate]: (optional) frame rate in fps. If not specified it will use
#                environment E_FPS (5fps by default).
#
# Envioronment variables:
#  - E_FPS: Frame rate of the ingested image sequence. The same frame rate will
#       be used for the output video stream. (default 5fps).
#
# Ref.: https://www.ffmpeg.org/ffmpeg-formats.html#dash-2 
#
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################

# If E_FPS is not set, set it to 5fps
if [[ -z "${E_FPS}" ]]; then
    E_FPS=5
fi
frame_rate=$2
if [[ -z "${frame_rate}" ]]; then
    frame_rate=${E_FPS}
fi

# Create output directory
mkdir /jobs_out/$1

# Frame rate of the ingested image sequence
ffmpeg_in_args="-framerate ${frame_rate}"

# File name pattern of the ingested images
ffmpeg_in_args="${ffmpeg_in_args} -pattern_type glob"

# Output stream codec (H.264)
ffmpeg_out_args="${ffmpeg_out_args} -c:v libx264"

# Output stream format (MPEG-DASH)
ffmpeg_out_args="${ffmpeg_out_args} -f dash"

# Output video pixel format (YUV4:2:0)
ffmpeg_out_args="${ffmpeg_out_args} -vf format=pix_fmts=yuv420p"

# Output video frame rate
ffmpeg_out_args="${ffmpeg_out_args} -r ${frame_rate}"

/usr/bin/ffmpeg ${ffmpeg_in_args} -i "/jobs_in/${1}/*.jpg" ${ffmpeg_out_args} /jobs_out/${1}/stream.mpd

# Copy the first frame to the output directory to use as thumbnail
first_frame=`ls -1tS /jobs_in/${1}/ | head -n 1`
cp /jobs_in/${1}/${first_frame} /jobs_out/${1}/thumbnail.jpg

# Delete input files
rm -rf /jobs_in/$1

################################################################################
###  Encoder docker image
#
# The purpose of this docker image is to encodes sequences of images to 
# MPEG-DASH video stream.
#
# It is based on alpine linux and includes ffmpeg package.
#
# When run, the container runs the register.sh and waits until the container
# termination is signaled.
#
# The encode.sh script is intended to be called multiple times using docker exec
# whenever a new encoding job needs to be processed.
#
# Envioronment variables:
#  - E_FPS: Frame rate of the ingested image sequence. The same frame rate will
#       be used for the output video stream. (default 5fps).
#
# Mount points:
#  - /jobs_in: contains the sequences of images ingested and to be encoded
#       into an MPEG-DASH stream
#  - /job_out: directory where the encoded stream will be output
#  - /encoders: directory where the encoder containers registers itself
#
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################
FROM alpine:3.12

# Set FPS environment variable default value
ENV E_FPS 5

# Include ffmpeg package and dependencies
RUN apk upgrade -U \
    && apk add --no-cache ca-certificates ffmpeg libva-intel-driver

COPY ./encode.sh /encode.sh
COPY ./register.sh /register.sh

RUN chmod a+x /encode.sh
RUN chmod a+x /register.sh

CMD ["/register.sh"]

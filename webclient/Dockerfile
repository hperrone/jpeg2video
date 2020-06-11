################################################################################
###  Web client interface docker image
#
# The purpose of this docker image is to provide an html web interface to view
# the videos.
#
# It is based on nginx web server and includes all the web client static
# resources (html, css, js and images), which it serves.
# Also, it serves all the dynamic content that is placed in the vidfeed volume.
# 
#   Volumes:
#       - vidfeed: contains the generated video streams.
#
#   Ports:
#       - TCP 80: HTTP server.
# 
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################
FROM nginx:1.19-alpine
COPY ./static /usr/share/nginx/html

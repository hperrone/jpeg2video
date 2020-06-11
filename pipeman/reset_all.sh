#!/bin/sh

################################################################################
### Clears all the persistent data within the docker's volumes.
#
# Usage:
#    reset_all.sh
#
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################
rm -rf /jobs_in/*
rm -rf /jobs_out/*

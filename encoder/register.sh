#!/bin/sh

################################################################################
### Register this container within the system.
#
# The registry is very simple. Just creates a file named as the container id in
# the /encoders directory.
#
# The script traps the SIGTERM signal, and blocks indefinitely until receiving
# it. On termination, it deletes its file from the /encoders directory.
#
###  # Released under MIT License
###  Copyright (c) 2020 Hernan Perrone (hernan.perrone@gmail.com)
################################################################################
container_id=`cat /etc/hostname`

#Define cleanup procedure
unregister() {
    echo "Unregister container ${container_id}..."
    rm /encoders/${container_id}
}

echo "Starting container ${container_id}..."

trap 'unregister' SIGTERM
trap 'unregister' SIGINT
trap 'unregister' SIGQUIT
trap 'unregister' SIGTSTP

touch /encoders/${container_id}

sleep infinity &
wait $!

unregister